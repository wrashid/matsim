/* *********************************************************************** *
 * project: org.matsim.*
 * LinkToLinkFastAStarEuclidean.java
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

import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.AStarNodeData;
import org.matsim.core.router.util.AStarNodeDataFactory;
import org.matsim.core.router.util.LinkToLinkRoutingNetwork;
import org.matsim.core.router.util.LinkToLinkTravelDisutility;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.NodeData;
import org.matsim.core.router.util.PreProcessEuclidean;
import org.matsim.core.router.util.RoutingNetworkLink;
import org.matsim.core.router.util.RoutingNetworkNode;
import org.matsim.core.utils.collections.RouterPriorityQueue;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author cdobler
 */
public class LinkToLinkFastAStarEuclidean extends LinkToLinkFastDijkstra {

	protected final double overdoFactor;

	private double minTravelCostPerLength;

	public LinkToLinkFastAStarEuclidean(final LinkToLinkRoutingNetwork routingNetwork, final LinkToLinkTravelDisutility costFunction, final LinkToLinkTravelTime timeFunction,
			final PreProcessEuclidean preProcessData, final double overdoFactor) {
		super(routingNetwork, costFunction, timeFunction, preProcessData);

		setMinTravelCostPerLength(preProcessData.getMinTravelCostPerLength());		
		this.overdoFactor = overdoFactor;
		
		this.nodeDataFactory = new AStarNodeDataFactory();
	}

	@Override	
	/*package*/ void initFromLink(final RoutingNetworkLink fromLink, final RoutingNetworkLink toLink, final double startTime,
			final RouterPriorityQueue<RoutingNetworkLink> pendingNodes) {
		/*
		 * Use the fromLink's toNode to get the valid next links.
		 * Turn restrictions are taken into account by the link's to-node. In case the to-turns are restricted,
		 * the node is a copy of the original node containing only valid out-links. 
		 */
		final double estimatedRemainingTravelCost = estimateRemainingTravelCost(fromLink.getToNode(), toLink.getFromNode());
		for (RoutingNetworkLink outLink : fromLink.getToNode().getOutLinksArray()) {
			AStarNodeData data = getData(outLink);
			visitNode(null, outLink, data, pendingNodes, startTime, 0, estimatedRemainingTravelCost);	// fromLink is null
		}
	}
	
	@Override
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
		final double remainingTravelCost = estimateRemainingTravelCost(node, toLink.getFromNode());
		for (RoutingNetworkLink outLink : node.getOutLinksArray()) {
		
			// calculate travel time and costs in case the next outLink is the next link
			final double travelTime = this.timeFunction.getLinkToLinkTravelTime(link, outLink, currTime);
			final double travelCost = this.costFunction.getLinkToLinkTravelDisutility(link, outLink, currTime, this.person, this.vehicle);
			final AStarNodeData data = getData(outLink);
			if (!data.isVisited(getIterationId())) {
				visitNode(link, outLink, data, pendingNodes, currTime + travelTime, currCost + travelCost, remainingTravelCost);
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
	 * Inserts the given Node n into the pendingNodes queue and updates its time and cost information.
	 */
	private void visitNode(final RoutingNetworkLink fromLink, final RoutingNetworkLink outLink, final AStarNodeData data, 
			final RouterPriorityQueue<RoutingNetworkLink> pendingNodes, final double time, final double cost, final double expectedRemainingCost) {
		data.setExpectedRemainingCost(expectedRemainingCost);
		super.visitNode(fromLink, outLink, data, pendingNodes, time, cost);
	}
		
	/**
	 * @return The overdo factor used.
	 */
	public double getOverdoFactor() {
		return this.overdoFactor;
	}

	/**
	 * Estimates the remaining travel cost from fromNode to toNode using the euclidean distance between them.
	 * 
	 * @param fromNode The first node.
	 * @param toNode The second node.
	 * @return The travel cost when traveling between the two given nodes.
	 */
	protected double estimateRemainingTravelCost(final Node fromNode, final Node toNode) {
		double dist = CoordUtils.calcEuclideanDistance(fromNode.getCoord(), toNode.getCoord()) * getMinTravelCostPerLength();
		return dist * this.overdoFactor;
	}

	/**
	 * Returns the data for the given Node. Creates a new AStarNodeData if none exists yet.
	 *
	 * @param n The node for which to return the data for..
	 * @return The data to the given Node
	 */
	@Override
	protected AStarNodeData getData(final RoutingNetworkLink link) {
		return (AStarNodeData) super.getData(link);
	}

	@Override
	protected AStarNodeData createNodeData() {
		return new AStarNodeData();
	}
	
	/**
	 * Sets minTravelCostPerLength to the given value.
	 * 
	 * @param minTravelCostPerLength
	 *            the minTravelCostPerLength to set
	 */
	/*package*/ void setMinTravelCostPerLength(final double minTravelCostPerLength) {
		this.minTravelCostPerLength = minTravelCostPerLength;
	}

	/**
	 * Returns the minimal travel cost per length unit on a link in the network.
	 * 
	 * @return the minimal travel cost per length unit on a link in the network.
	 */
	public final double getMinTravelCostPerLength() {
		return this.minTravelCostPerLength;
	}

	/**
	 * The value used to sort the pending nodes during routing. This implementation compares the total 
	 * estimated remaining travel cost to sort the nodes in the pending nodes queue during routing.
	 */
	@Override
	protected double getPriority(final NodeData data) {
		return ((AStarNodeData) data).getExpectedCost();
	}
}