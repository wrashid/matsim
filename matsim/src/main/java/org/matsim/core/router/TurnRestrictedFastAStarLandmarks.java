/* *********************************************************************** *
 * project: org.matsim.*
 * TurnRestrictedFastAStarLandmarks.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.DijkstraNodeData;
import org.matsim.core.router.util.LinkToLinkRoutingNetwork;
import org.matsim.core.router.util.LinkToLinkRoutingNetworkLink;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.RoutingNetworkNode;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TurnRestrictedLeastCostPathCalculator;
import org.matsim.core.utils.collections.RouterPriorityQueue;
import org.matsim.vehicles.Vehicle;

/**
 * <p>
 * Performance optimized version of the Dijkstra {@link org.matsim.core.router.Dijkstra} 
 * least cost path router which uses its own network to route within.
 * </p>
 * 
 * @see org.matsim.core.router.Dijkstra
 * @see org.matsim.core.router.util.RoutingNetwork
 * @author cdobler
 */
public class TurnRestrictedFastAStarLandmarks extends FastAStarLandmarks implements TurnRestrictedLeastCostPathCalculator {

	private final static Logger log = Logger.getLogger(TurnRestrictedFastAStarLandmarks.class);
	
	private LinkToLinkRoutingNetworkLink fromLink;
	private LinkToLinkRoutingNetworkLink toLink;
	private Node[] toNodes;	// Turn restricted Nodes from which the toLink can be reached. null, if no restrictions are present.
		
	public TurnRestrictedFastAStarLandmarks(final LinkToLinkRoutingNetwork routingNetwork, final TravelDisutility costFunction, final TravelTime timeFunction,
			final PreProcessLandmarks preProcessData, final double overdoFactor, final FastRouterDelegateFactory fastRouterFactory) {
		super(routingNetwork, preProcessData, costFunction, timeFunction, overdoFactor, fastRouterFactory);		
	}
	
	@Override
	public Path calcLeastCostPath(final Node fromNode, final Node toNode, final double startTime, final Person person, final Vehicle vehicle) {
		// Ensure that this method is not called!
		throw new RuntimeException("This router does not support this operation - use a regular FastDijsktra instead!");
	}
	
	@Override
	public Path calcLeastCostPath(final Link fromLink, final Link toLink, final double startTime, final Person person, final Vehicle vehicle) {	
		
		this.fastRouter.initialize();
		this.routingNetwork.initialize();
		
		final RoutingNetworkNode routingNetworkFromNode;
		final RoutingNetworkNode routingNetworkToNode;
		
		// This cast is save since we expect a LinkToLinkRoutingNetwork in the constructor!
		this.fromLink = ((LinkToLinkRoutingNetwork) this.routingNetwork).getLinks().get(fromLink.getId());
		this.toLink = ((LinkToLinkRoutingNetwork) this.routingNetwork).getLinks().get(toLink.getId());
		
		/*
		 * In case the from link has turn restrictions, we find it in the routing network's
		 * links map. In that case, we have to start routing at the found link's to-node, which
		 * takes the turn restrictions into account.
		 */
		if (this.fromLink != null) routingNetworkFromNode = this.fromLink.getToNode();
		else routingNetworkFromNode = this.routingNetwork.getNodes().get(fromLink.getToNode().getId());
		
		/*
		 * In case the link can be accessed from multiple nodes, i.e. the regular node had to be
		 * duplicated due to turn restrictions, also the to-link had to be duplicated. In case
		 * the node had to be duplicated but the link can only be accessed from one of the
		 * new nodes, we get that single node. Otherwise, the canEndSearch(...) method in
		 * AbstractFastRouterDelegate handles checking whether one of the valid nodes was reached.
		 */
		if (this.toLink != null) {	// i.e. we have a RoutingNetworkLink, therefore, we might have multiple to-nodes
	        this.toNodes = this.toLink.getFromNodes();
			if ( this.toNodes == null ||  this.toNodes.length == 1) {
				routingNetworkToNode = this.toLink.getFromNode();
				this.toNodes = null;	// there is only a single to-node, therefore, we can set the array to null
			} else {
				routingNetworkToNode = null;	// unclear which toNode is valid; let the canEndSearch method decide
			}
		} else {	// it is a regular to-node, i.e. it can only be accessed from a single node 
			routingNetworkToNode = this.routingNetwork.getNodes().get(toLink.getFromNode().getId());
			this.toNodes = null;
		}

		// in case the routingNetworkToNode is null, we use the node from the regular network
		if (routingNetworkToNode == null) return super.calcLeastCostPath(routingNetworkFromNode, toLink.getFromNode(), startTime, person, vehicle);
		else return super.calcLeastCostPath(routingNetworkFromNode, routingNetworkToNode, startTime, person, vehicle);
	}
	
	// Replace search logic from plain Dijkstra. In a future step, we could extract the "outNode == toNode" check to its own clas
	/*package*/ Node searchLogicReplacement(final Node fromNode, final Node toNode, final RouterPriorityQueue<Node> pendingNodes) {
		
		boolean stillSearching = true;
		
		while (stillSearching) {
			Node outNode = pendingNodes.poll();

			if (outNode == null) {
				log.warn("No route was found from node " + fromNode.getId() + " to node " + toNode.getId());
				return null;
			}

			if (canEndSearch(outNode, toNode)) {
				stillSearching = false;
			} else {
				relaxNode(outNode, toNode, pendingNodes);
			}
		}
		return toNode;
	}
	
	// In the plain Dijkstra, the check is "if (outNode == toNode)" 
	// If that is extracted to its own method, the searchLogicReplacement(...) method can be removed
	private boolean canEndSearch(final Node outNode, final Node toNode) {
		if (this.toNodes != null) {
			for (Node node : this.toNodes) {
				if (outNode == node) return true;
			}
			return false;
		} else return outNode == toNode;
	}
	
	@Override
	/*package*/ Node searchLogic(final Node fromNode, final Node toNode, final RouterPriorityQueue<Node> pendingNodes) {
//		Node foundToNode = super.searchLogic(fromNode, toNode, pendingNodes);	// can be re-activated once the canEndSearch method is extracted
		Node foundToNode = searchLogicReplacement(fromNode, toNode, pendingNodes);
		
		// If the to-Link can be reached from multiple nodes, check all of them to find the correct one.
		// If non of them was visited by the router, no route was found, i.e. we have to return null.
		if (this.toLink != null && this.toLink.getFromNodes() != null) {
			Node minNode = null;
			double time = Double.MAX_VALUE;
			for (Node node : this.toLink.getFromNodes()) {
				DijkstraNodeData data = getData(node);
				if (data.isVisited(getIterationId())) {
					if (data.getTime() < time) {
						time = data.getTime();
						minNode = node;
					}
				}
			} return minNode;
		} else return foundToNode;
	}
}