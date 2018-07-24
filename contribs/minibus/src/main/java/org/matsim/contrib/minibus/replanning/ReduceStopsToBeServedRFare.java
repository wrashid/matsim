/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.contrib.minibus.replanning;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.fare.StageContainer;
import org.matsim.contrib.minibus.fare.StageContainerHandler;
import org.matsim.contrib.minibus.fare.TicketMachineI;
import org.matsim.contrib.minibus.genericUtils.RecursiveStatsContainer;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * 
 * Removes all stops belonging to a demand relation with trips below a certain threshold.
 * Threshold is standard deviation of number of trips or collected fare of all relations twice a scaling factor.
 * 
 * @author aneumann
 *
 */
public final class ReduceStopsToBeServedRFare extends AbstractPStrategyModule implements StageContainerHandler{
	
	private final static Logger log = Logger.getLogger(ReduceStopsToBeServedRFare.class);
	
	public static final String STRATEGY_NAME = "ReduceStopsToBeServedRFare";
	
	private final double sigmaScale;
	private final boolean useFareAsWeight;

	private TicketMachineI ticketMachine;
	private LinkedHashMap<Id<TransitRoute>, LinkedHashMap<Id<TransitStopFacility>, LinkedHashMap<Id<TransitStopFacility>, Double>>> route2StartStop2EndStop2WeightMap = new LinkedHashMap<>();
	private enum Direction {FORWARD, RETURN};
	
	public ReduceStopsToBeServedRFare(ArrayList<String> parameter) {
		super();
		if(parameter.size() != 2){
			log.error("Too many parameter. Will ignore: " + parameter);
			log.error("Parameter 1: Scaling factor for sigma");
			log.error("Parameter 2: true=use the fare as weight, false=use number of trips as weight");
		}
		this.sigmaScale = Double.parseDouble(parameter.get(0));
		this.useFareAsWeight = Boolean.parseBoolean(parameter.get(1));
		log.info("enabled");
	}
	
	@Override
	public PPlan run(Operator operator) {
		PPlan oldPlan = operator.getBestPlan();
		ArrayList<TransitStopFacility> currentStopsToBeServedForwardDirection = oldPlan.getStopsToBeServedForwardDirection();
		ArrayList<TransitStopFacility> currentStopsToBeServedReturnDirection = oldPlan.getStopsToBeServedReturnDirection();
		
		TransitStopFacility baseStop = currentStopsToBeServedForwardDirection.get(0);
		TransitStopFacility remoteStop = currentStopsToBeServedReturnDirection.get(0);
		
		// get best plans route id
		TransitRoute routeToOptimize = null;
		if (operator.getBestPlan().getLine().getRoutes().size() != 1) {
			log.error("There should be only one route at this time - Please check");
		}
		for (TransitRoute route : operator.getBestPlan().getLine().getRoutes().values()) {
			routeToOptimize = route;
		}
		
		Map<Direction, ArrayList<TransitStopFacility>> newStopsToBeServed = getStopsToBeServed(baseStop, remoteStop, 
				this.route2StartStop2EndStop2WeightMap.get(routeToOptimize.getId()), routeToOptimize);
		
		if (newStopsToBeServed == null) {
			return null;
		}
		
		if (newStopsToBeServed.get(Direction.FORWARD).size() < 1 || newStopsToBeServed.get(Direction.RETURN).size() < 1) {
			// too few stops left - cannot return a new plan
			return null;
		}
		
		// profitable route, change startTime
		PPlan newPlan = new PPlan(operator.getNewPlanId(), this.getStrategyName(), operator.getBestPlan().getId());
		newPlan.setNVehicles(1);
		newPlan.setStopsToBeServedForwardDirection(newStopsToBeServed.get(Direction.FORWARD));
		newPlan.setStopsToBeServedReturnDirection(newStopsToBeServed.get(Direction.RETURN));
		newPlan.setStartTime(operator.getBestPlan().getStartTime());
		newPlan.setEndTime(operator.getBestPlan().getEndTime());
		
		newPlan.setLine(operator.getRouteProvider().createTransitLineFromOperatorPlan(operator.getId(), newPlan));
		
		return newPlan;
	}

	private Map<Direction, ArrayList<TransitStopFacility>> getStopsToBeServed(TransitStopFacility baseStop, TransitStopFacility remoteStop, 
			LinkedHashMap<Id<TransitStopFacility>, LinkedHashMap<Id<TransitStopFacility>, Double>> startStop2EndStop2WeightMap, TransitRoute routeToOptimize) {
		ArrayList<TransitStopFacility> tempStopsToBeServed = new ArrayList<>();
		RecursiveStatsContainer stats = new RecursiveStatsContainer();
		
		if (startStop2EndStop2WeightMap == null) {
			// There is no entry for that particular line - possibly no demand - returning empty line
			return null;
		}
		
		// calculate standard deviation
		for (LinkedHashMap<Id<TransitStopFacility>, Double> endStop2TripsMap : startStop2EndStop2WeightMap.values()) {
			for (Double trips : endStop2TripsMap.values()) {
				stats.handleNewEntry(trips);
			}
		}
		
		if (stats.getNumberOfEntries() == 1) {
			// We use circular routes. There is always a way back (with no demand). Add a second entry.
			stats.handleNewEntry(0.0);			
		}
		
		double sigmaTreshold = stats.getStdDev() * this.sigmaScale;
		Set<Id<TransitStopFacility>> stopIdsAboveTreshold = new TreeSet<>();
		
		// Get all stops serving a demand above threshold
		for (Entry<Id<TransitStopFacility>, LinkedHashMap<Id<TransitStopFacility>, Double>> endStop2TripsMapEntry : startStop2EndStop2WeightMap.entrySet()) {
			for (Entry<Id<TransitStopFacility>, Double> tripEntry : endStop2TripsMapEntry.getValue().entrySet()) {
				if (tripEntry.getValue() > sigmaTreshold) {
					// ok - add the corresponding stops to the set
					stopIdsAboveTreshold.add(endStop2TripsMapEntry.getKey());
					stopIdsAboveTreshold.add(tripEntry.getKey());
				}
			}
		}
		
		// Get new stops to be served
		for (TransitRouteStop stop : routeToOptimize.getStops()) {
			if (stopIdsAboveTreshold.contains(stop.getStopFacility().getId())) {
				tempStopsToBeServed.add(stop.getStopFacility());
			}
		}
		
		ArrayList<TransitStopFacility> stopsToBeServed = new ArrayList<>();
		
		// avoid using a stop twice in a row
		for (TransitStopFacility stop : tempStopsToBeServed) {
			if (stopsToBeServed.size() > 0) {
				if (stopsToBeServed.get(stopsToBeServed.size() - 1).getId() != stop.getId()) {
					stopsToBeServed.add(stop);
				}
			} else {
				stopsToBeServed.add(stop);
			}			
		}
		
		// delete last stop, if it is the same as the first one
		if (stopsToBeServed.size() > 1) {
			if (stopsToBeServed.get(0).getId() == stopsToBeServed.get(stopsToBeServed.size() - 1).getId()) {
				stopsToBeServed.remove(stopsToBeServed.size() - 1);
			}
		}
		
		// search new base and remote stop (the termini)
		int newBaseStopIndex = findClosestStopIndexInNewStopList(0, 
				stopsToBeServed, routeToOptimize);
		int newRemoteStopIndex = findClosestStopIndexInNewStopList(findIndexOfStopToBeServedInTransitRoute(remoteStop, routeToOptimize), 
				stopsToBeServed, routeToOptimize);
		
		// split stopsToBeServed into forward and backward directions
		ArrayList<TransitStopFacility> newStopsToBeServedForwardDirection = new ArrayList<>();
		ArrayList<TransitStopFacility> newStopsToBeServedReturnDirection = new ArrayList<>();
		
		int i = newBaseStopIndex;
		boolean isForward = true;
		do {
			if (i == newRemoteStopIndex) {
				isForward = false;
			}
			if (isForward) {
				newStopsToBeServedForwardDirection.add(stopsToBeServed.get(i));
			} else {
				newStopsToBeServedReturnDirection.add(stopsToBeServed.get(i));
			}
			i++;
			if (i == stopsToBeServed.size()) {
				// reached last stop in list, continue from index 0
				i = 0;
			}
		}
		// stop when the whole list is done and the stop with which was begun is reached
		while(i != newBaseStopIndex);
		
		Map<Direction, ArrayList<TransitStopFacility>> result = new HashMap<>();
		result.put(Direction.FORWARD, newStopsToBeServedForwardDirection);
		result.put(Direction.RETURN, newStopsToBeServedReturnDirection);
		return result;
	}
	
	private int findIndexOfStopToBeServedInTransitRoute (TransitStopFacility searchedStop, TransitRoute transitRoute) {
		List<Integer> indexesOfSearchedStopInTransitRoute = new ArrayList<>();
		for (int i = 0; i < transitRoute.getStops().size(); i++) {
			if (transitRoute.getStops().get(i).getStopFacility().getId().equals(searchedStop.getId())) {
				indexesOfSearchedStopInTransitRoute.add(i);
			}
		}
		
		int result = indexesOfSearchedStopInTransitRoute.get(0);
		if (indexesOfSearchedStopInTransitRoute.size() > 1) {
			// TODO FIXME
			log.warn("The remote terminus stop is served several times in the TransitRoute. Picking one of the positions in the TransitRoute. This is not fatal, but might lead to a wrong position of the terminus on the route and thereby to awkward routes.");
			// Take one occurence in the middle
			result = indexesOfSearchedStopInTransitRoute.get(indexesOfSearchedStopInTransitRoute.size() / 2);
		}
		return result;
	}

	private int findClosestStopIndexInNewStopList(int searchedStopIndexInTransitRoute,
			ArrayList<TransitStopFacility> stopsToBeServed, TransitRoute oldTransitRoute) {
		
		List<Id<TransitStopFacility>> stopIdsToBeServed = new ArrayList<>();
		for (TransitStopFacility stopFacility: stopsToBeServed) {
			stopIdsToBeServed.add(stopFacility.getId());
		}
		
		/* 
		 * decide whether the first stop found in stopsToBeServed going forward or
		 * the first stop found in stopsToBeServed going backward should be used as
		 * terminus based on the beeline distance from the previous terminus, which
		 * was posibly removed from the stops to be served list.
		 */
		
		double forwardBeelineDistance = -1;
		double backwardBeelineDistance = -1;
		int indexForwardSearch = searchedStopIndexInTransitRoute;
		int indexBackwardSearch = searchedStopIndexInTransitRoute;
		TransitStopFacility formerTerminus = oldTransitRoute.getStops().get(searchedStopIndexInTransitRoute).getStopFacility();
		
		while (forwardBeelineDistance < 0) {
			TransitStopFacility forwardStop = oldTransitRoute.getStops().get(indexForwardSearch).getStopFacility();
			if (stopIdsToBeServed.contains(forwardStop.getId())) {
				// found first stop in stops to be served from old transit route
				forwardBeelineDistance = CoordUtils.calcEuclideanDistance(formerTerminus.getCoord(), forwardStop.getCoord());
			} else {
				// continue search
				if (indexForwardSearch + 1 < oldTransitRoute.getStops().size()) {
					indexForwardSearch++;
				} else {
					indexForwardSearch = 0;
				}
			}
			if (indexForwardSearch == searchedStopIndexInTransitRoute) {
				new RuntimeException("Terminus stop could not be determined, because none of the new stops to be served was included in the old TransitRoute. This should not happen.");
			}
		}
		while (backwardBeelineDistance < 0) {
			TransitStopFacility backwardStop = oldTransitRoute.getStops().get(indexBackwardSearch).getStopFacility();
			if (stopIdsToBeServed.contains(backwardStop.getId())) {
				// found first stop in stops to be served from old transit route
				backwardBeelineDistance = CoordUtils.calcEuclideanDistance(formerTerminus.getCoord(), backwardStop.getCoord());
			} else {
				// continue search
				if (indexBackwardSearch - 1 >= 0) {
					indexBackwardSearch--;
				} else {
					indexBackwardSearch = oldTransitRoute.getStops().size() - 1;
				}
			}
			if (indexForwardSearch == searchedStopIndexInTransitRoute) {
				new RuntimeException("Terminus stop could not be determined, because none of the new stops to be served was included in the old TransitRoute. This should not happen.");
			}
		}
		
		// Choose stop found forward or backward by beeline distance from former terminus
		if (forwardBeelineDistance < backwardBeelineDistance) {
			/*
			 *  FIXME: What if the terminus stop is contained multiple times in stopIdsToBeServed? 
			 *  Should rather not happen for the terminus according to the replanning strategies, but not sure whether this is actually the case.
			 *  Maybe test whether stopIdsToBeServed and oldTransitRoute overlap (stopIdsToBeServed stops appear in the same order as on oldTransitRoute)
			 */
			return stopIdsToBeServed.indexOf(oldTransitRoute.getStops().get(indexForwardSearch).getStopFacility().getId());
		} else {
			return stopIdsToBeServed.indexOf(oldTransitRoute.getStops().get(indexBackwardSearch).getStopFacility().getId());
		}
	}

	@Override
	public String getStrategyName() {
		return ReduceStopsToBeServedRFare.STRATEGY_NAME;
	}
	
	@Override
	public void reset() {
		this.route2StartStop2EndStop2WeightMap = new LinkedHashMap<>();
	}

	@Override
	public void handleFareContainer(StageContainer stageContainer) {
		Id<TransitRoute> routeId = stageContainer.getRouteId();
		Id<TransitStopFacility> startStopId = stageContainer.getStopEntered();
		Id<TransitStopFacility> endStopId = stageContainer.getStopLeft();
		
		if (this.route2StartStop2EndStop2WeightMap.get(routeId) == null) {
			this.route2StartStop2EndStop2WeightMap.put(routeId, new LinkedHashMap<Id<TransitStopFacility>, LinkedHashMap<Id<TransitStopFacility>, Double>>());
		}

		if (this.route2StartStop2EndStop2WeightMap.get(routeId).get(startStopId) == null) {
			this.route2StartStop2EndStop2WeightMap.get(routeId).put(startStopId, new LinkedHashMap<Id<TransitStopFacility>, Double>());
		}

		if (this.route2StartStop2EndStop2WeightMap.get(routeId).get(startStopId).get(endStopId) == null) {
			this.route2StartStop2EndStop2WeightMap.get(routeId).get(startStopId).put(endStopId, 0.0);
		}

		double oldWeight = this.route2StartStop2EndStop2WeightMap.get(routeId).get(startStopId).get(endStopId);
		double additionalWeight = 1.0;
		if (this.useFareAsWeight) {
			additionalWeight = this.ticketMachine.getFare(stageContainer);
		}
		this.route2StartStop2EndStop2WeightMap.get(routeId).get(startStopId).put(endStopId, oldWeight + additionalWeight);
	}

	public void setTicketMachine(TicketMachineI ticketMachine) {
		this.ticketMachine = ticketMachine;
	}
}