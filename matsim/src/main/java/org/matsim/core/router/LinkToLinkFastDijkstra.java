/* *********************************************************************** *
 * project: org.matsim.*
 * LinkToLinkFastDijkstra.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.core.router;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.priorityqueue.BinaryMinHeap;
import org.matsim.core.router.util.ArrayRoutingNetworkLink;
import org.matsim.core.router.util.DijkstraNodeData;
import org.matsim.core.router.util.DijkstraNodeDataFactory;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LinkToLinkArrayRoutingNetworkLink;
import org.matsim.core.router.util.LinkToLinkLeastCostPathCalculator;
import org.matsim.core.router.util.LinkToLinkRoutingNetwork;
import org.matsim.core.router.util.LinkToLinkRoutingNetworkLink;
import org.matsim.core.router.util.LinkToLinkTravelDisutility;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.NodeData;
import org.matsim.core.router.util.NodeDataFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.PreProcessDijkstra.DeadEndData;
import org.matsim.core.router.util.RoutingNetworkLink;
import org.matsim.core.router.util.RoutingNetworkNode;
import org.matsim.core.utils.collections.RouterPriorityQueue;
import org.matsim.vehicles.Vehicle;

/**
 * An implementation of the Dijkstra algorithm, that takes LinkToLink travel times into account.
 * 
 * This is a bit tricky: when a node is visited, it is not clear which outgoing link will be used.
 * I.e. it is not clear, what is the LinkToLink travel time!
 * 
 * Try to split nodes based on their out-links. one node for each out-link - then we can use the
 * link-to-link travel time since we can only continue to that single link. It should be possible
 * to use the link's indices for the heap since they are also enumerated from 0 .. n. Moreover,
 * the routing network contains duplicated links, but they share all the same index. Should be fine :?
 * 
 * @author cdobler
 */
public class LinkToLinkFastDijkstra implements LinkToLinkLeastCostPathCalculator {

	private final static Logger log = Logger.getLogger(LinkToLinkFastDijkstra.class);

	/**
	 * The cost calculator. Provides the cost for each link and time step.
	 */
	protected final LinkToLinkTravelDisutility costFunction;

	/**
	 * The travel time calculator. Provides the travel time for each link and time step.
	 */
	protected final LinkToLinkTravelTime timeFunction;

	/**
	 * Provides an unique id (loop number) for each routing request, so we don't
	 * have to reset all nodes at the beginning of each re-routing but can use the
	 * loop number instead.
	 */
	private int iterationID = Integer.MIN_VALUE + 1;

	/**
	 * Temporary field that is only used if dead ends are being pruned during
	 * routing and is updated each time a new route has to be calculated.
	 */
	private Node deadEndEntryNode;

	/**
	 * Determines whether we should mark nodes in dead ends during a
	 * pre-processing step so they won't be expanded during routing.
	 */
	/*package*/ final boolean pruneDeadEnds;
	
	/*package*/ Person person = null;
	/*package*/ Vehicle vehicle = null;

	/*package*/ final LinkToLinkRoutingNetwork routingNetwork;

	/**
	 * Instead of nodes, we now store links in the heap. Previously, we stored the links' to-nodes.
	 * However, since we add nodes multiple times to the heap (once for each out-link), we need
	 * to know which out-link should be handled. Therefore, now we store links instead of nodes.
	 */
	/*package*/ BinaryMinHeap<ArrayRoutingNetworkLink> heap = null;
	private int maxSize = -1;
	
	private boolean isInitialized = false;
	private final NodeData[] nodeData;
	/*package*/ NodeDataFactory nodeDataFactory;
	
	/**
	 * Constructor.
	 *
	 * @param network
	 *            The network on which to route.
	 * @param costFunction
	 *            Determines the link cost defining the cheapest route.
	 * @param timeFunction
	 *            Determines the travel time on each link.
	 * @param preProcessData
	 *            The pre processing data used during the routing phase.
	 */
	// please use the factory when you want to create an instance of this
	protected LinkToLinkFastDijkstra(final LinkToLinkRoutingNetwork routingNetwork, final LinkToLinkTravelDisutility costFunction, final LinkToLinkTravelTime timeFunction,
			final PreProcessDijkstra preProcessData) {

		this.costFunction = costFunction;
		this.timeFunction = timeFunction;

		if (preProcessData != null) {
			if (!preProcessData.containsData()) {
				this.pruneDeadEnds = false;
				log.warn("The preprocessing data provided to router class Dijkstra contains no data! Please execute its run(...) method first!");
				log.warn("Running without dead-end pruning.");
			} else {
				this.pruneDeadEnds = true;
			}
		} else {
			this.pruneDeadEnds = false;
		}
		
		this.routingNetwork = routingNetwork;
		this.nodeData = new NodeData[this.routingNetwork.getLinks().size()];
		this.nodeDataFactory = new DijkstraNodeDataFactory();
	}
	
	@Override
	public Path calcLeastCostPath(final Link fromLink, final Link toLink, final double startTime, final Person person, final Vehicle vehicle) {
		
		this.initialize();
		this.routingNetwork.initialize();
		
		final LinkToLinkRoutingNetworkLink routingNetworkFromLink;
		final LinkToLinkRoutingNetworkLink routingNetworkToLink;
		if (fromLink instanceof LinkToLinkRoutingNetworkLink) routingNetworkFromLink = (LinkToLinkRoutingNetworkLink) fromLink;
		else routingNetworkFromLink = this.routingNetwork.getLinks().get(fromLink.getId());
		if (toLink instanceof LinkToLinkRoutingNetworkLink) routingNetworkToLink = (LinkToLinkRoutingNetworkLink) fromLink;
		else routingNetworkToLink = this.routingNetwork.getLinks().get(toLink.getId());
		
		/*
		 * Ensure that the given links are part of the network used by the router. Otherwise, the router would
		 * route within the network of the nodes and NOT the one provided when it was created. Previously, this 
		 * caused problems when sub-networks where used.
		 * cdobler, jun'14
		 */
		checkLinkBelongToNetwork(routingNetworkFromLink);
		checkLinkBelongToNetwork(routingNetworkToLink);
				
		augmentIterationId(); // this call makes the class not thread-safe
		this.person = person;
		this.vehicle = vehicle;

		if (this.pruneDeadEnds) {
			this.deadEndEntryNode = getPreProcessData(routingNetworkToLink.getFromNode()).getDeadEndEntryNode();
		}

		@SuppressWarnings("unchecked")
		RouterPriorityQueue<RoutingNetworkLink> pendingNodes = (RouterPriorityQueue<RoutingNetworkLink>) createRouterPriorityQueue();
		initFromLink(routingNetworkFromLink, routingNetworkToLink, startTime, pendingNodes);
		
		if (searchLogic(routingNetworkFromLink, routingNetworkToLink, pendingNodes)) {
			NodeData outData = getData(routingNetworkToLink);
			double arrivalTime = outData.getTime();
			
			// now construct and return the path
			return constructPath(routingNetworkFromLink, routingNetworkToLink, startTime, arrivalTime);						
		} else return null;
	}
	
	private void initialize() {
		// lazy initialization
		if (!this.isInitialized) {
			for (RoutingNetworkLink link : this.routingNetwork.getLinks().values()) {
				int index = ((LinkToLinkArrayRoutingNetworkLink) link).getArrayIndex();
				this.nodeData[index] = this.nodeDataFactory.createNodeData();
			}
			
			this.isInitialized = true;
		}
	}

	/*
	 * Move this code to a separate method since it needs to be extended by the MultiNodeDijkstra.
	 * cdobler, jun'14
	 */
	/*package*/ void checkLinkBelongToNetwork(RoutingNetworkLink link) {
		if (this.routingNetwork.getLinks().get(link.getId()) != link) {
			throw new IllegalArgumentException("The links passed as parameters are not part of the network stored by "+
					getClass().getSimpleName() + ": the validity of the results cannot be guaranteed. Aborting!");
		}
	}
	
	/**
	 * Allow replacing the RouterPriorityQueue.
	 */
	/*package*/ RouterPriorityQueue<? extends Link> createRouterPriorityQueue() {	
		/*
		 * Re-use existing BinaryMinHeap instead of creating a new one. For large networks (> 10^6 nodes and links) this reduced
		 * the computation time by 40%! cdobler, oct'15
		 */
		int size = this.routingNetwork.getLinks().size();
		if (this.heap == null || this.maxSize != size) {
			this.maxSize = size;
			this.heap = new BinaryMinHeap<>(this.maxSize);
			return this.heap;
		} else {
			this.heap.reset();
			return this.heap;
		}
	}
	
	/**
	 * Logic that was previously located in the calcLeastCostPath(...) method.
	 * Can be overwritten in the MultiModalDijkstra.
	 * Returns the last node of the path. By default this is the to-node.
	 * The MultiNodeDijkstra returns the cheapest of all given to-nodes.
	 */
	/*package*/ boolean searchLogic(final RoutingNetworkLink fromLink, final RoutingNetworkLink toLink, final RouterPriorityQueue<RoutingNetworkLink> pendingNodes) {
		
		boolean stillSearching = true;
		
		while (stillSearching) {
			RoutingNetworkLink outLink = pendingNodes.poll();

			if (outLink == null) {
				log.warn("No route was found from link " + fromLink.getId() + " to link " + toLink.getId());
				return false;
			}

			if (canEndSearch(outLink, toLink)) stillSearching = false;
			else relaxNode(outLink, pendingNodes, toLink);
		}
		
		return true;
	}

	
	private boolean canEndSearch(final RoutingNetworkLink outLink, final RoutingNetworkLink toLink) {
		return ((LinkToLinkArrayRoutingNetworkLink) outLink).getArrayIndex() == ((LinkToLinkArrayRoutingNetworkLink) toLink).getArrayIndex();
	}
	
	/**
	 * Constructs the path after the algorithm has been run.
	 *
	 * @param fromNode
	 *            The node where the path starts.
	 * @param toNode
	 *            The node where the path ends.
	 * @param startTime
	 *            The time when the trip starts.
	 */
	protected Path constructPath(RoutingNetworkLink fromLink, RoutingNetworkLink toLink, double startTime, double arrivalTime) {
		List<Node> nodes = new ArrayList<>();
		List<Link> links = new ArrayList<>();

		nodes.add(0, toLink.getFromNode().getNode());
		Link tmpLink = getData(toLink).getPrevLink();
		if (tmpLink != null) {
			while (tmpLink != null) {
				links.add(0, ((RoutingNetworkLink) tmpLink).getLink());
				nodes.add(0, ((RoutingNetworkLink) tmpLink).getLink().getFromNode());
				tmpLink = getData((RoutingNetworkLink) tmpLink).getPrevLink();
			}
		}
		
		NodeData toNodeData = getData(toLink);
		Path path = new Path(nodes, links, arrivalTime - startTime, toNodeData.getCost());	
		return path;
	}
	
	/**
	 * Initializes the first node of a route.
	 *
	 * @param fromNode
	 *            The Node to be initialized.
	 * @param toNode
	 *            The Node at which the route should end.
	 * @param startTime
	 *            The time we start routing.
	 * @param pendingNodes
	 *            The pending nodes so far.
	 */
	/*package*/ void initFromLink(final RoutingNetworkLink fromLink, final RoutingNetworkLink toLink, final double startTime,
			final RouterPriorityQueue<RoutingNetworkLink> pendingNodes) {
		/*
		 * Use the fromLink's toNode to get the valid next links.
		 * Turn restrictions are taken into account by the link's to-node. In case the to-turns are restricted,
		 * the node is a copy of the original node containing only valid out-links. 
		 */
		for (RoutingNetworkLink outLink : fromLink.getToNode().getOutLinksArray()) {
			NodeData data = getData(outLink);
			visitNode(null, outLink, data, pendingNodes, startTime, 0);	// fromLink is null
			
			// Do we need the outlink here?? In the Dijkstra, it is null...
			// In case we set it not null, we need to adapt the constructPath(...) method since that aborts once the data is null
//			visitNode(fromNode, data, pendingNodes, startTime, 0, outLink);
		}
	}

	/**
	 * Expands the given Node in the routing algorithm; may be overridden in
	 * sub-classes.
	 *
	 * @param outNode
	 *            The Node to be expanded.
	 * @param toNode
	 *            The target Node of the route.
	 * @param pendingNodes
	 *            The set of pending nodes so far.
	 */
	protected void relaxNode(final RoutingNetworkLink outLink, final RouterPriorityQueue<RoutingNetworkLink> pendingNodes, final RoutingNetworkLink toLink) {
		
		NodeData outData = getData(outLink);
		double currTime = outData.getTime();
		double currCost = outData.getCost();
		if (this.pruneDeadEnds) {
			DeadEndData ddOutData = getPreProcessData(outLink.getFromNode());
			this.relaxNodeLogic(outLink, pendingNodes, currTime, currCost, ddOutData, toLink);
		} else { // this.pruneDeadEnds == false
			this.relaxNodeLogic(outLink, pendingNodes, currTime, currCost, null, toLink);
		}
	}
	
	/**
	 * Logic that was previously located in the relaxNode(...) method. 
	 * By doing so, the FastDijkstra can overwrite relaxNode without copying the logic. 
	 */
	/*package*/ void relaxNodeLogic(final RoutingNetworkLink link, final RouterPriorityQueue<RoutingNetworkLink> pendingNodes, 
			final double currTime, final double currCost, final DeadEndData ddOutData, final RoutingNetworkLink toLink) {
		if (this.pruneDeadEnds) {
			DeadEndData ddData = getPreProcessData(link.getToNode());
			
			/* 
			 * IF the current node n is not in a dead end
			 * OR it is in the same dead end as the fromNode
			 * OR it is in the same dead end as the toNode
			 * THEN we add the current node to the pending nodes
			 */
			if ((ddData.getDeadEndEntryNode() == null)
					|| (ddOutData.getDeadEndEntryNode() != null)
					|| ((this.deadEndEntryNode != null) && (this.deadEndEntryNode.getId() == ddData.getDeadEndEntryNode().getId()))) {
				addToPendingNodes(link, pendingNodes, currTime, currCost, toLink);
			}
		} else {
			addToPendingNodes(link, pendingNodes, currTime, currCost, toLink);
		}
	}
	
	/**
	 * Adds some parameters to the given Node then adds it to the set of pending
	 * nodes.
	 *
	 * @param link
	 *            The link from which we came to this Node.
	 * @param pendingNodes
	 *            The set of pending nodes.
	 * @param currTime
	 *            The time at which we started to traverse l.
	 * @param currCost
	 *            The cost at the time we started to traverse l.
	 * @param toNode
	 *            The target Node of the route.
	 * @return true if the node was added to the pending nodes, false otherwise
	 * 		(e.g. when the same node already has an earlier visiting time).
	 */
	protected boolean addToPendingNodes(final RoutingNetworkLink link, final RouterPriorityQueue<RoutingNetworkLink> pendingNodes, 
			final double currTime, final double currCost, final RoutingNetworkLink toLink) {
		
		/*
		 * I think we have to add not one but number-of-outLink entries to the heap. Instead of the node's indices,
		 * we can use the ones from the out-links. They should be unique as well. By doing so, we can calculate the
		 * link-to-link travel times since we know where the node is leading to.
		 * Moreover, I think we also need one NodeData object for each out-link.
		 */
		boolean added = false;	// true, if at least one node was added to the queue
		RoutingNetworkNode node = link.getToNode();
		for (RoutingNetworkLink outLink : node.getOutLinksArray()) {
		
			// calculate travel time and costs in case the next outLink is the next link
			final double travelTime = this.timeFunction.getLinkToLinkTravelTime(link, outLink, currTime);
			final double travelCost = this.costFunction.getLinkToLinkTravelDisutility(link, outLink, currTime, this.person, this.vehicle);
			final NodeData data = getData(outLink);
			if (!data.isVisited(getIterationId())) {
				visitNode(link, outLink, data, pendingNodes, currTime + travelTime, currCost + travelCost);
				added = true;
			} else {				
				final double nCost = data.getCost();
				final double totalCost = currCost + travelCost;
				if (totalCost < nCost) {
					revisitNode(link, outLink, data, pendingNodes, currTime + travelTime, totalCost);
					added = true;
				} else if (totalCost == nCost && travelCost > 0) {
					// Special case: a node can be reached from two links with exactly the same costs.
					// Decide based on the linkId which one to take... just have to common criteria to be deterministic.
					if (data.getPrevLink().getId().compareTo(link.getId()) > 0) {
						revisitNode(link, outLink, data, pendingNodes, currTime + travelTime, totalCost);
						added = true;
					}
				}
			}
		}
		return added;		
	}

	/**
	 * Inserts the given Node n into the pendingNodes queue and updates its time
	 * and cost information.
	 *
	 * @param fromLink
	 *            The link from which we came visiting the node.
	 * @param outLink
	 *            The link to which we have to continue from the current node.
	 * @param data
	 *            The data for the node.
	 * @param pendingNodes
	 *            The nodes visited and not processed yet.
	 * @param time
	 *            The time of the visit of the node.
	 * @param cost
	 *            The accumulated cost at the time of the visit of the node.
	 */
	protected void visitNode(final RoutingNetworkLink fromLink, final RoutingNetworkLink outLink, final NodeData data, 
			final RouterPriorityQueue<RoutingNetworkLink> pendingNodes, final double time, final double cost) {
		data.visit(fromLink, cost, time, getIterationId());
		pendingNodes.add(outLink, getPriority(data));
	}

	/**
	 * Changes the position of the given Node n in the pendingNodes queue and
	 * updates its time and cost information.
	 *
	 * @param fromLink
	 *            The link from which we came visiting the node.
	 * @param toLink
	 *            The link to which we have to continue from the current node.
	 * @param data
	 *            The data for n.
	 * @param pendingNodes
	 *            The nodes visited and not processed yet.
	 * @param time
	 *            The time of the visit of n.
	 * @param cost
	 *            The accumulated cost at the time of the visit of n.
	 */
	protected void revisitNode(final RoutingNetworkLink fromLink, final RoutingNetworkLink toLink, final NodeData data, final RouterPriorityQueue<RoutingNetworkLink> pendingNodes, 
			final double time, final double cost) {
		data.visit(fromLink, cost, time, getIterationId());
		pendingNodes.decreaseKey(toLink, getPriority(data));
	}
	
	/**
	 * Augments the iterationID and checks whether the visited information in
	 * the nodes in the nodes have to be reset.
	 */
	protected void augmentIterationId() {
		if (getIterationId() == Integer.MAX_VALUE) {
			this.iterationID = Integer.MIN_VALUE + 1;
			resetNetworkVisited();
		} else {
			this.iterationID++;
		}
	}

	/**
	 * @return iterationID
	 */
	protected int getIterationId() {
		return this.iterationID;
	}

	/**
	 * Resets all nodes in the network as if they have not been visited yet.
	 */
	private void resetNetworkVisited() {
		for (NodeData data : this.nodeData) data.resetVisited();
	}

	/**
	 * The value used to sort the pending nodes during routing.
	 * This implementation compares the total effective travel cost
	 * to sort the nodes in the pending nodes queue during routing.
	 */
	protected double getPriority(final NodeData data) {
		return data.getCost();
	}

	/**
	 * Returns the data for the given node.
	 *
	 * @param link
	 *            The Node for which to return the data.
	 * @return The data for the given Node
	 */
	protected NodeData getData(final RoutingNetworkLink link) {
		LinkToLinkArrayRoutingNetworkLink routingNetworkLink = (LinkToLinkArrayRoutingNetworkLink) link;
		return this.nodeData[routingNetworkLink.getArrayIndex()];
	}

	protected DijkstraNodeData createNodeData() {
		return new DijkstraNodeData();
	}
	
	protected DeadEndData getPreProcessData(final Node n) {
		return ((RoutingNetworkNode) n).getDeadEndData();
	}

	protected final Person getPerson() {
		return this.person;
	}
	
	protected final Vehicle getVehicle() {
		return this.vehicle;
	}
}