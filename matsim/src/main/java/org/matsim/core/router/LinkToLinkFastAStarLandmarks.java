/* *********************************************************************** *
 * project: org.matsim.*
 * LinkToLinkFastAStarLandmarks.java
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
import java.util.Iterator;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.AStarNodeData;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LinkToLinkRoutingNetwork;
import org.matsim.core.router.util.LinkToLinkRoutingNetworkLink;
import org.matsim.core.router.util.LinkToLinkTravelDisutility;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.PreProcessLandmarks.LandmarksData;
import org.matsim.core.router.util.RoutingNetworkLink;
import org.matsim.core.utils.collections.RouterPriorityQueue;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

/**
 * @author cdobler
 */
public class LinkToLinkFastAStarLandmarks extends LinkToLinkFastAStarEuclidean {

	protected int[] activeLandmarkIndexes;

	protected final Node[] landmarks;

	/*package*/ static final int controlInterval = 40;
	/*package*/ int controlCounter = 0;

	public LinkToLinkFastAStarLandmarks(final LinkToLinkRoutingNetwork routingNetwork, final LinkToLinkTravelDisutility costFunction, final LinkToLinkTravelTime timeFunction,
			final PreProcessLandmarks preProcessData, final double overdoFactor) {
		super(routingNetwork, costFunction, timeFunction, preProcessData, overdoFactor);
		
		this.landmarks = preProcessData.getLandmarks();
	}

	@Override
	public Path calcLeastCostPath(final Link fromLink, final Link toLink, final double startTime, final Person person, final Vehicle vehicle) {	
		this.controlCounter = 0;	// reset counter for each calculated path!
		
		final LinkToLinkRoutingNetworkLink routingNetworkFromLink = this.routingNetwork.getLinks().get(fromLink.getId());
		final LinkToLinkRoutingNetworkLink routingNetworkToLink = this.routingNetwork.getLinks().get(toLink.getId());
		
		if (this.landmarks.length >= 2) {
			initializeActiveLandmarks(routingNetworkFromLink.getToNode(), routingNetworkToLink.getFromNode(), 2);
		} else {
			initializeActiveLandmarks(routingNetworkFromLink.getToNode(), routingNetworkToLink.getFromNode(), this.landmarks.length);
		}
		return super.calcLeastCostPath(fromLink, toLink, startTime, person, vehicle);
	}
	
	@Override
	protected void relaxNode(final RoutingNetworkLink outLink, final RouterPriorityQueue<RoutingNetworkLink> pendingNodes, final RoutingNetworkLink toLink) {
		this.controlCounter++;
		if (this.controlCounter == controlInterval) {
			int newLandmarkIndex = checkToAddLandmark(outLink.getFromNode(), toLink.getFromNode());
			if (newLandmarkIndex > 0) {
				updatePendingNodes(newLandmarkIndex, toLink, pendingNodes);
			}
			this.controlCounter = 0;
		}
		super.relaxNode(outLink, pendingNodes, toLink);
	}

	/**
	 * Inspect all landmarks and determines the actLandmarkCount best ones that will be used for routing (so-called active landmarks).
	 * 
	 * @param fromNode The node for which we estimate the travel time to the toNode in order to rank the landmarks.
	 * @param toNode The node to which we estimate the travel time from the fromNode in order to rank the landmarks.
	 * @param actLandmarkCount The number of active landmarks landmarks to set.
	 */
	/*package*/ void initializeActiveLandmarks(final Node fromNode, final Node toNode, final int actLandmarkCount) {
		final LandmarksData fromData = getPreProcessData(fromNode);
		final LandmarksData toData = getPreProcessData(toNode);

		// Sort the landmarks according to the accuracy of their distance estimation they yield.
		double[] estTravelTimes = new double[actLandmarkCount];
		this.activeLandmarkIndexes = new int[actLandmarkCount];
		for (int i = 0; i < estTravelTimes.length; i++) {
			estTravelTimes[i] = Time.UNDEFINED_TIME;
		}
		double tmpTravTime;
		for (int i = 0; i < this.landmarks.length; i++) {
			tmpTravTime = estimateRemainingTravelCost(fromData, toData, i);
			for (int j = 0; j < estTravelTimes.length; j++) {
				if (tmpTravTime > estTravelTimes[j]) {
					for (int k = estTravelTimes.length - 1; k > j; k--) {
						estTravelTimes[k] = estTravelTimes[k - 1];
						this.activeLandmarkIndexes[k] = this.activeLandmarkIndexes[k - 1];
					}
					estTravelTimes[j] = tmpTravTime;
					this.activeLandmarkIndexes[j] = i;
					break;
				}
			}
		}
	}
	
	@Override
	protected LandmarksData getPreProcessData(final Node n) {
		return (LandmarksData) super.getPreProcessData(n);
	}

	/**
	 * Estimates the remaining travel cost from fromNode to toNode using the landmarks on the network.
	 * 
	 * @param fromNode The first node.
	 * @param toNode The second node.
	 * @return The travel cost when traveling between the two given nodes.
	 */
	@Override
	protected double estimateRemainingTravelCost(final Node fromNode, final Node toNode) {

		LandmarksData fromRole = getPreProcessData(fromNode);
		LandmarksData toRole = getPreProcessData(toNode);
		double tmpTravCost;
		double travCost = 0;
		for (int i = 0, n = this.activeLandmarkIndexes.length; i < n; i++) {
			tmpTravCost = estimateRemainingTravelCost(fromRole, toRole, this.activeLandmarkIndexes[i]);
			if (tmpTravCost > travCost) {
				travCost = tmpTravCost;
			}
		}
		tmpTravCost = super.estimateRemainingTravelCost(fromNode, toNode);
		if (travCost > tmpTravCost) {
			return travCost;
		}
		/* else */
		return tmpTravCost;
	}

	/**
	 * If a landmark has been added to the set of the active landmarks, this function re-evaluates the estimated 
	 * remaining travel time based on the new set of active landmarks of the nodes contained in pendingNodes. 
	 * If this estimation improved, the node's position in the pendingNodes queue is updated.
	 * 
	 * @param newLandmarkIndex The index of the new active landmark.
	 * @param toNode The target node of the route to be calculated.
	 * @param pendingNodes The nodes visited so far.
	 */
	/*package*/ void updatePendingNodes(final int newLandmarkIndex, final RoutingNetworkLink toLink, final RouterPriorityQueue<RoutingNetworkLink> pendingNodes) {
		Iterator<RoutingNetworkLink> it = pendingNodes.iterator();
		PreProcessLandmarks.LandmarksData toRole = getPreProcessData(toLink.getFromNode());
		List<Double> newEstRemTravCosts = new ArrayList<>();
		List<RoutingNetworkLink> nodesToBeUpdated = new ArrayList<>();
		while (it.hasNext()) {
			RoutingNetworkLink link = it.next();
			AStarNodeData data = getData(toLink);
			PreProcessLandmarks.LandmarksData ppRole = getPreProcessData(link.getFromNode());
			double estRemTravCost = data.getExpectedRemainingCost();
			double newEstRemTravCost = estimateRemainingTravelCost(ppRole, toRole, newLandmarkIndex);
			if (newEstRemTravCost > estRemTravCost) {
				nodesToBeUpdated.add(link);
				newEstRemTravCosts.add(newEstRemTravCost);
			}
		}
		for (RoutingNetworkLink link : nodesToBeUpdated) {
			pendingNodes.remove(link);
		}
		for (int i = 0; i < nodesToBeUpdated.size(); i++) {
			RoutingNetworkLink link = nodesToBeUpdated.get(i);
			AStarNodeData data = getData(link);
			data.setExpectedRemainingCost(newEstRemTravCosts.get(i));
			pendingNodes.add(link, getPriority(data));
		}
	}

	/**
	 * Checks whether there is a landmark in the set of the non-active landmarks that yields a better estimation than 
	 * the best active landmark. If there is, this landmark is added to the set of active landmark and its index is returned.
	 * 
	 * @param fromNode The node for which we estimate the remaining travel time to the toNode.
	 * @param toNode The target node. 
	 * @return The index of the landmark that has been added to the set of active landmarks, or -1 if no landmark was added.
	 */
	/*package*/ int checkToAddLandmark(final Node fromNode, final Node toNode) {
		double bestTravCostEst = estimateRemainingTravelCost(fromNode, toNode);
		LandmarksData fromRole = getPreProcessData(fromNode);
		LandmarksData toRole = getPreProcessData(toNode);
		int bestIndex = -1;
		for (int i = 0; i < this.landmarks.length; i++) {
			double tmpTravTime = estimateRemainingTravelCost(fromRole, toRole, i);
			if (tmpTravTime > bestTravCostEst) {
				bestIndex = i;
				bestTravCostEst = tmpTravTime;
			}
		}
		if (bestIndex != -1) {
			int[] newActiveLandmarks = new int[this.activeLandmarkIndexes.length + 1];
			System.arraycopy(this.activeLandmarkIndexes, 0, newActiveLandmarks, 0, this.activeLandmarkIndexes.length);
			newActiveLandmarks[this.activeLandmarkIndexes.length] = bestIndex;
			this.activeLandmarkIndexes = newActiveLandmarks;
		}
		return bestIndex;
	}

	/**
	 * Estimates the remaining travel cost from fromNode to toNode using the landmark given by index.
	 * 
	 * @param fromRole The first node/role.
	 * @param toRole The second node/role.
	 * @param index The index of the landmarks that should be used for
	 * the estimation of the travel cost.
	 * @return The travel cost when traveling between the two given nodes.
	 */
	protected double estimateRemainingTravelCost(final LandmarksData fromRole, final LandmarksData toRole, final int index) {
		double tmpTravTime;
		final double fromMinLandmarkTravelTime = fromRole.getMinLandmarkTravelTime(index);
		final double toMaxLandmarkTravelTime = toRole.getMaxLandmarkTravelTime(index);
		tmpTravTime = fromMinLandmarkTravelTime - toMaxLandmarkTravelTime;
		if (tmpTravTime < 0) {
			tmpTravTime = toRole.getMinLandmarkTravelTime(index) - fromRole.getMaxLandmarkTravelTime(index);
			if (tmpTravTime <= 0) {
				return 0;
			}
		}
		return tmpTravTime * this.overdoFactor;
	}
}