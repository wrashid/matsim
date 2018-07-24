/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.contrib.minibus.routeProvider.PRouteProvider;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.*;

/**
 * Takes the route transformed into a lineString to calculate a buffer around it.
 * Excludes the part of the buffer going parallel/along the actual route, so only the end caps remain.
 * Chooses then randomly a new stop within the buffer and inserts it after the nearest existing stop.
 * 
 * @author aneumann
 *
 */
public final class EndRouteExtension extends AbstractPStrategyModule {
	
	private final static Logger log = Logger.getLogger(EndRouteExtension.class);
	public static final String STRATEGY_NAME = "EndRouteExtension";
	private final double bufferSize;
	private final double ratio;
	private final boolean backwardCompatibleAddNewBeforeBaseOrAfterRemoteStop;
	private final boolean addFormerTerminusOnWayBack;
	private enum Direction {FORWARD, RETURN};
	
	public EndRouteExtension(ArrayList<String> parameter) {
		super();
		if(parameter.size() < 2 && parameter.size() > 4){
			log.error("Parameter 1: Buffer size in meter");
			log.error("Parameter 2: Ratio bufferSize to route's beeline length (between termini ignoring detours to serve other stops to be served in between). If set to something very small, e.g. 0.01, the calculated buffer size may be smaller than the one specified in parameter 1. Parameter 1 will then be taken as minimal buffer size.");
			log.warn("Parameter 3: Optional: Backwards Compatibility: add new stop either before base stop or after remote stop. Else the new stop is added either before base stop or before remote stop. Default is true.");
			log.warn("Parameter 4: Optional: Create forth and back extension by adding the former terminus (A) after the new terminus B, creating a back and forth extension |C-...-A-|B|-A-...-C| . Default is false (only add the new stop B).");
		}
		this.bufferSize = Double.parseDouble(parameter.get(0));
		this.ratio = Double.parseDouble(parameter.get(1));
		if (parameter.size() > 2) {
			this.backwardCompatibleAddNewBeforeBaseOrAfterRemoteStop = Boolean.parseBoolean(parameter.get(2));
		} else {
			this.backwardCompatibleAddNewBeforeBaseOrAfterRemoteStop = false;
		}
		if (parameter.size() > 3) {
			this.addFormerTerminusOnWayBack = Boolean.parseBoolean(parameter.get(3));
		} else {
			this.addFormerTerminusOnWayBack = false;
		}
	}

	@Override
	public PPlan run(Operator operator) {

		PPlan oldPlan = operator.getBestPlan();
		ArrayList<TransitStopFacility> currentStopsToBeServedForwardDirection = oldPlan.getStopsToBeServedForwardDirection();
		ArrayList<TransitStopFacility> currentStopsToBeServedReturnDirection = oldPlan.getStopsToBeServedReturnDirection();
		
		TransitStopFacility baseStop = currentStopsToBeServedForwardDirection.get(0);
		TransitStopFacility remoteStop = currentStopsToBeServedReturnDirection.get(0);

		double bufferSizeBasedOnRatio = CoordUtils.calcEuclideanDistance(baseStop.getCoord(), remoteStop.getCoord()) * this.ratio;
		
		List<Geometry> lineStrings = this.createGeometryFromStops(currentStopsToBeServedForwardDirection, currentStopsToBeServedReturnDirection);
		Geometry bufferWithoutEndCaps = this.createBuffer(lineStrings, Math.max(this.bufferSize, bufferSizeBasedOnRatio), true);
		Geometry bufferWithEndCaps = this.createBuffer(lineStrings, Math.max(this.bufferSize, bufferSizeBasedOnRatio), false);
		Geometry buffer = bufferWithEndCaps.difference(bufferWithoutEndCaps);
		
		Set<Id<TransitStopFacility>> stopsUsed = this.getStopsUsed(oldPlan.getLine().getRoutes().values());
		TransitStopFacility newStop = this.drawRandomStop(buffer, operator.getRouteProvider(), stopsUsed);
		
		if (newStop == null) {
			return null;
		}
		
		// BEGIN Test
		
		System.out.println(newStop.getId().toString());
		
		// END Test
		
		Map<Direction, ArrayList<TransitStopFacility>> newStopsToBeServed = this.addStopToExistingStops(baseStop, remoteStop, 
				currentStopsToBeServedForwardDirection, currentStopsToBeServedReturnDirection, newStop);
		
		// BEGIN TEST
		
		for(TransitStopFacility stop: newStopsToBeServed.get(Direction.FORWARD)) {
			System.err.print(stop.getId().toString() + " ");
		}
		System.err.println();
		System.err.print("return: ");
		for(TransitStopFacility stop: newStopsToBeServed.get(Direction.RETURN)) {
			System.err.print(stop.getId().toString() + " ");
		}
		System.err.println();
		
		// END TEST
		
		// create new plan
		PPlan newPlan = new PPlan(operator.getNewPlanId(), this.getStrategyName(), oldPlan.getId());
		newPlan.setNVehicles(1);
		newPlan.setStartTime(oldPlan.getStartTime());
		newPlan.setEndTime(oldPlan.getEndTime());
		newPlan.setStopsToBeServedForwardDirection(newStopsToBeServed.get(Direction.FORWARD));
		newPlan.setStopsToBeServedReturnDirection(newStopsToBeServed.get(Direction.RETURN));
		newPlan.setLine(operator.getRouteProvider().createTransitLineFromOperatorPlan(operator.getId(), newPlan));
		
		return newPlan;
	}


	private Map<Direction, ArrayList<TransitStopFacility>> addStopToExistingStops(TransitStopFacility baseStop, TransitStopFacility remoteStop, 
			ArrayList<TransitStopFacility> currentStopsToBeServedForwardDirection, ArrayList<TransitStopFacility> currentStopsToBeServedReturnDirection, 
			TransitStopFacility newStop) {
		ArrayList<TransitStopFacility> newStopsToBeServedForwardDirection = new ArrayList<>(currentStopsToBeServedForwardDirection);
		ArrayList<TransitStopFacility> newStopsToBeServedReturnDirection = new ArrayList<>(currentStopsToBeServedReturnDirection);
		
		// decide which stop is closer
		if (CoordUtils.calcEuclideanDistance(baseStop.getCoord(), newStop.getCoord()) < CoordUtils.calcEuclideanDistance(remoteStop.getCoord(), newStop.getCoord())) {
			// baseStop is closer - insert before baseStop
			newStopsToBeServedForwardDirection.add(0, newStop);
			if (addFormerTerminusOnWayBack) {
				newStopsToBeServedReturnDirection.add(newStopsToBeServedReturnDirection.size(), baseStop);
			}
		} else {
			// remote stop is closer or both have the same distance - add after remote stop
			if (backwardCompatibleAddNewBeforeBaseOrAfterRemoteStop) {
				newStopsToBeServedForwardDirection.add(newStopsToBeServedForwardDirection.size(), remoteStop);
				newStopsToBeServedReturnDirection.set(0, newStop);
				if (addFormerTerminusOnWayBack) {
					newStopsToBeServedReturnDirection.add(1, remoteStop);
				}
			} else {
				newStopsToBeServedReturnDirection.add(0, newStop);
				if (addFormerTerminusOnWayBack) {
					newStopsToBeServedForwardDirection.add(newStopsToBeServedForwardDirection.size(), remoteStop);
				}
			}
		}
		
		// BEGIN TEST
		
		for(TransitStopFacility stop: currentStopsToBeServedForwardDirection) {
			System.err.print(stop.getId().toString() + " ");
		}
		System.err.print(" new: ");
		for(TransitStopFacility stop: newStopsToBeServedForwardDirection) {
			System.err.print(stop.getId().toString() + " ");
		}
		System.err.println();
		System.err.print("return: ");
		for(TransitStopFacility stop: currentStopsToBeServedReturnDirection) {
			System.err.print(stop.getId().toString() + " ");
		}
		System.err.print(" new: ");
		for(TransitStopFacility stop: newStopsToBeServedReturnDirection) {
			System.err.print(stop.getId().toString() + " ");
		}
		System.err.println();
		
		// END TEST
		
		Map<Direction, ArrayList<TransitStopFacility>> result = new HashMap<>();
		result.put(Direction.FORWARD, newStopsToBeServedForwardDirection);
		result.put(Direction.RETURN, newStopsToBeServedReturnDirection);
		return result;
	}

	private Set<Id<TransitStopFacility>> getStopsUsed(Collection<TransitRoute> routes) {
		Set<Id<TransitStopFacility>> stopsUsed = new TreeSet<>();
		for (TransitRoute route : routes) {
			for (TransitRouteStop stop : route.getStops()) {
				stopsUsed.add(stop.getStopFacility().getId());
			}
		}
		return stopsUsed;
	}

	private TransitStopFacility drawRandomStop(Geometry buffer, PRouteProvider pRouteProvider, Set<Id<TransitStopFacility>> stopsUsed) {
		List<TransitStopFacility> choiceSet = new LinkedList<>();
		
		// find choice-set
		for (TransitStopFacility stop : pRouteProvider.getAllPStops()) {
			if (!stopsUsed.contains(stop.getId())) {
				if (buffer.contains(MGC.coord2Point(stop.getCoord()))) {
					choiceSet.add(stop);
				}
			}
		}
		
		return pRouteProvider.drawRandomStopFromList(choiceSet);
	}


	private Geometry createBuffer(List<Geometry> lineStrings, double bufferSize, boolean excludeTermini) {
		BufferParameters bufferParameters = new BufferParameters();
		
		if (excludeTermini) {
			bufferParameters.setEndCapStyle(BufferParameters.CAP_FLAT);
		} else {
			bufferParameters.setEndCapStyle(BufferParameters.CAP_ROUND);
		}
		
		Geometry union = null;
		
		for (Geometry lineString : lineStrings) {
			Geometry buffer = BufferOp.bufferOp(lineString, bufferSize, bufferParameters);
			if (union == null) {
				union = buffer;
			} else {
				union = union.union(buffer);
			}
		}
		
		return union;
	}

	private List<Geometry> createGeometryFromStops(ArrayList<TransitStopFacility> stopsForwardDirection, ArrayList<TransitStopFacility> stopsReturnDirection) {
		List<Geometry> geometries = new LinkedList<>();
		
		// Forward direction
		ArrayList<Coordinate> coordsForward = new ArrayList<>();
		for (TransitStopFacility stop : stopsForwardDirection) {
			coordsForward.add(new Coordinate(stop.getCoord().getX(), stop.getCoord().getY(), 0.0));
		}
		// add first stop of return direction to close the circle
		coordsForward.add(new Coordinate(stopsReturnDirection.get(0).getCoord().getX(), stopsReturnDirection.get(0).getCoord().getY(), 0.0));
		Coordinate[] coordinatesForward = coordsForward.toArray(new Coordinate[coordsForward.size()]);
		Geometry lineStringForward = new GeometryFactory().createLineString(coordinatesForward);
		geometries.add(lineStringForward);
		
		// Return direction
		ArrayList<Coordinate> coordsReturn = new ArrayList<>();
		for (TransitStopFacility stop : stopsReturnDirection) {
			coordsReturn.add(new Coordinate(stop.getCoord().getX(), stop.getCoord().getY(), 0.0));
		}
		// add first stop of forward direction to close the circle
		coordsReturn.add(new Coordinate(stopsForwardDirection.get(0).getCoord().getX(), stopsForwardDirection.get(0).getCoord().getY(), 0.0));
		Coordinate[] coordinatesReturn = coordsReturn.toArray(new Coordinate[coordsReturn.size()]);
		Geometry lineStringReturn = new GeometryFactory().createLineString(coordinatesReturn);
		geometries.add(lineStringReturn);
		return geometries;
	}

	public String getStrategyName() {
		return EndRouteExtension.STRATEGY_NAME;
	}
}
