/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetworkTravelTimeAndDisutilityVariableWW.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.spatialDrt.firstLastAVPTRouter;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.contrib.spatialDrt.firstLastAVPTRouter.linkLinkTimes.LinkLinkTime;
import org.matsim.contrib.spatialDrt.firstLastAVPTRouter.waitLinkTime.WaitLinkTime;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.TravelTimeCalculatorConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterNetworkTravelTimeAndDisutility;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

/**
 * TravelTime and TravelDisutility calculator to be used with the transit network used for transit routing.
 * This version considers waiting time at stops, and takes travel time between stops from a {@link StopStopTime} object.
 *
 * @author sergioo
 */

public class TransitRouterTravelTimeAndDisutilityFirstLastAVPT extends TransitRouterNetworkTravelTimeAndDisutility implements TravelDisutility {

	private Link previousLink;
	private double previousTime;
	private double cachedLinkTime;
	private double cachedWaitTime;
	private final Map<Id<Link>, double[]> linkTravelTimes = new HashMap<>();
	private final Map<Id<Link>, double[]> linkTravelTimesAV = new HashMap<>();
	private final Map<Id<Link>, double[]> linkWaitingTimes = new HashMap<>();
	private final Map<Id<Link>, double[]> linkWaitingTimesAV = new HashMap<>();
	private final WaitLinkTime waitLinkTime;
	private final LinkLinkTime linkLinkTime;
	private final int numSlots;
	private final double timeSlot;
	final static double avSpeed = 6.0;
	final static double avTaxiSpeed = 9.0;
	private final TransitRouterParams params;


	public TransitRouterTravelTimeAndDisutilityFirstLastAVPT(TransitRouterParams params, final TransitRouterConfig config, TransitRouterNetworkFirstLastAVPT routerNetwork, WaitTime waitTime, WaitTime waitTimeAV, WaitLinkTime waitLinkTimeAV, StopStopTime stopStopTime, StopStopTime stopStopTimeAV, LinkLinkTime linkLinkTime, TravelTimeCalculatorConfigGroup tTConfigGroup, QSimConfigGroup qSimConfigGroup, PreparedTransitSchedule preparedTransitSchedule) {
		this(params, config, routerNetwork, waitTime, waitTimeAV, waitLinkTimeAV, stopStopTime, stopStopTimeAV, linkLinkTime, tTConfigGroup, qSimConfigGroup.getStartTime(), qSimConfigGroup.getEndTime(), preparedTransitSchedule);
	}
	public TransitRouterTravelTimeAndDisutilityFirstLastAVPT(TransitRouterParams params, final TransitRouterConfig config, TransitRouterNetworkFirstLastAVPT routerNetwork, WaitTime waitTime, WaitTime waitTimeAV, WaitLinkTime waitLinkTime, StopStopTime stopStopTime, StopStopTime stopStopTimeAV, LinkLinkTime linkLinkTime, TravelTimeCalculatorConfigGroup tTConfigGroup, double startTime, double endTime, PreparedTransitSchedule preparedTransitSchedule) {
		super(config, preparedTransitSchedule);
		this.params = params;
		this.waitLinkTime = waitLinkTime;
		this.linkLinkTime = linkLinkTime;
		timeSlot = tTConfigGroup.getTraveltimeBinSize();
		numSlots = (int) ((endTime-startTime)/timeSlot);
		for(TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkLink link:routerNetwork.getLinks().values())
			if(link.route!=null) {
				double[] times = new double[numSlots];
				for(int slot = 0; slot<numSlots; slot++)
					times[slot] = stopStopTime.getStopStopTime(link.fromNode.stop.getId(), link.toNode.stop.getId(), startTime+slot*timeSlot);
				linkTravelTimes.put(link.getId(), times);
			}
			else if(link.toNode.route!=null && link.toNode.line!=null) {
				double[] times = new double[numSlots];
				for(int slot = 0; slot<numSlots; slot++)
					times[slot] = waitTime.getRouteStopWaitTime(link.toNode.line.getId(), link.toNode.route.getId(), link.fromNode.stop.getId(), startTime+slot*timeSlot);
				linkWaitingTimes.put(link.getId(), times);
			}
			else if(link.fromNode.route==null && link.mode.equals(TransitRouterFirstLastAVPT.AV_MODE)) {
				double[] times = new double[numSlots];
				for(int slot = 0; slot<numSlots; slot++)
					times[slot] = stopStopTimeAV.getStopStopTime(link.fromNode.stop.getId(), link.toNode.stop.getId(), startTime+slot*timeSlot);
				linkTravelTimesAV.put(link.getId(), times);
				times = new double[numSlots];
				for(int slot = 0; slot<numSlots; slot++)
					times[slot] = waitTimeAV.getRouteStopWaitTime(null, null, link.fromNode.stop.getId(), startTime+slot*timeSlot);
				linkWaitingTimesAV.put(link.getId(), times);
			}
	}
	@Override
	public double getLinkTravelTime(final Link link, final double time, Person person, Vehicle vehicle) {
		previousLink = link;
		previousTime = time;
		cachedWaitTime = 0;
		TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkLink wrapped = (TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkLink) link;
		int index = time/timeSlot<numSlots ? (int)(time/timeSlot) : (numSlots-1);
		double length = wrapped.getLength()<3?3:wrapped.getLength();
		if (wrapped.route!=null)
			//in line link
			cachedLinkTime = linkTravelTimes.get(wrapped.getId())[index];
		else if(wrapped.toNode.route!=null && wrapped.toNode.line!=null)
			//wait link
			cachedLinkTime = linkWaitingTimes.get(wrapped.getId())[index];
		else if(wrapped.fromNode.route==null && wrapped.mode.equals(TransportMode.transit_walk))
			//walking link
			cachedLinkTime = length/this.config.getBeelineWalkSpeed();
		else if(wrapped.fromNode.route==null) {
			// it's a transfer link (av)
			cachedLinkTime = linkTravelTimesAV.get(wrapped.getId())[index];
			cachedWaitTime = linkWaitingTimesAV.get(wrapped.getId())[index];
		}
		else
			//inside link
			cachedLinkTime = 0;
		if(cachedLinkTime+cachedWaitTime>10000000)
			System.out.println();
		return cachedLinkTime + cachedWaitTime;
	}
	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager) {
		boolean cachedTravelDisutility = false;
		if(previousLink==link && previousTime==time)
			cachedTravelDisutility = true;
		TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkLink wrapped = (TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkLink) link;
		int index = time/timeSlot<numSlots ? (int)(time/timeSlot) : (numSlots-1);
		double length = wrapped.getLength()<3?3:wrapped.getLength();
		if (wrapped.route != null)
			return -(cachedTravelDisutility?cachedLinkTime:linkTravelTimes.get(wrapped.getId())[index])*this.config.getMarginalUtilityOfTravelTimePt_utl_s()
					- link.getLength() * (this.config.getMarginalUtilityOfTravelDistancePt_utl_m());
		else if (wrapped.toNode.route!=null && wrapped.toNode.line!=null)
			// it's a wait link
			return -(cachedTravelDisutility?cachedLinkTime:linkWaitingTimes.get(wrapped.getId())[index])*this.config.getMarginalUtilityOfWaitingPt_utl_s();
		else if(wrapped.fromNode.route==null && wrapped.mode.equals(TransportMode.transit_walk))
			// it's a transfer link (walk)
			return -(cachedTravelDisutility?cachedLinkTime:length/this.config.getBeelineWalkSpeed())*this.config.getMarginalUtilityOfTravelTimeWalk_utl_s();
		else if(wrapped.fromNode.route==null)
			// it's a transfer link (av)
			return -(cachedTravelDisutility?cachedLinkTime:linkTravelTimesAV.get(wrapped.getId())[index])*params.marginalUtilityAV_s
					-(cachedTravelDisutility?cachedWaitTime:linkWaitingTimesAV.get(wrapped.getId())[index])*this.config.getMarginalUtilityOfWaitingPt_utl_s();
		else
			//inside link
			return -this.config.getUtilityOfLineSwitch_utl();
	}
	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkLink wrapped = (TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkLink) link;
		double length = wrapped.getLength()<3?3:wrapped.getLength();
		if (wrapped.route != null)
			return - linkTravelTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)]*this.config.getMarginalUtilityOfTravelTimePt_utl_s() 
					- link.getLength() * (this.config.getMarginalUtilityOfTravelDistancePt_utl_m());
		else if (wrapped.toNode.route!=null && wrapped.toNode.line!=null)
			// it's a wait link
			return - linkWaitingTimes.get(wrapped.getId())[time/timeSlot<numSlots?(int)(time/timeSlot):(numSlots-1)]*this.config.getMarginalUtilityOfWaitingPt_utl_s();
		else if(wrapped.fromNode.route==null && wrapped.mode.equals(TransportMode.transit_walk))
			// it's a transfer link (walk)
			return -length/this.config.getBeelineWalkSpeed()*this.config.getMarginalUtilityOfTravelTimeWalk_utl_s();
		else if(wrapped.fromNode.route==null)
			// it's a transfer link (av)
			return -linkTravelTimesAV.get(wrapped.getId())[time / timeSlot < numSlots ? (int) (time / timeSlot) : (numSlots - 1)]*params.marginalUtilityAV_s
					- params.avWaiting*this.config.getMarginalUtilityOfWaitingPt_utl_s();
		else
			//inside link
			return - this.config.getUtilityOfLineSwitch_utl();
	}
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0;
	}

	public double getAVTaxiTravelDisutility(Person person, Id<Link> linkA, Id<Link> linkB, double time) {
		double travelTime = linkLinkTime.getLinkLinkTime(linkA, linkB, time);
		double distanceCost = -avTaxiSpeed * travelTime * params.marginalUtilityAV_m;
		double waitCost = -waitLinkTime.getWaitLinkTime(linkA, time) * this.config.getMarginalUtilityOfWaitingPt_utl_s();
		return -travelTime * params.marginalUtilityAVTaxi_s + distanceCost + waitCost + params.initialCostAVTaxi;
	}

	public double getAVTravelTime(Person person, Id<Link> linkA, Id<Link> linkB, double time) {
		return waitLinkTime.getWaitLinkTime(linkA, time) + linkLinkTime.getLinkLinkTime(linkA, linkB, time);
	}

	@Override
	public double getWalkTravelDisutility(Person person, Coord coord, Coord toCoord) {
		double timeCost = -getWalkTravelTime(person, coord, toCoord) * params.marginalUtilityWalk_s;
		double distanceCost = - CoordUtils.calcEuclideanDistance(coord,toCoord) * config.getBeelineDistanceFactor() * params.marginalUtilityWalk_m;
		return timeCost + distanceCost + params.initialCostWalk;
	}

}
