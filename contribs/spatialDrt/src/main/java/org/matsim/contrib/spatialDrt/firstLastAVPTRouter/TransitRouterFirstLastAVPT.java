/* *********************************************************************** *
 * project: org.matsim.*
 * TranitRouterVariableImpl.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.eventsBasedPTRouter.MultiDestinationDijkstra;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.InitialNode;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.PtConstants;
import org.matsim.pt.router.MultiNodeDijkstra;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.*;

public class TransitRouterFirstLastAVPT implements RoutingModule {

    public static final String AV_MODE = "drt";
	public static final String AV_TAXI_MODE = "drtaxi";
	private final TransitRouterNetworkFirstLastAVPT transitNetwork;

	private final MultiNodeDijkstra dijkstra;
	private final MultiDestinationDijkstra mDijkstra;
	private final TransitRouterConfig config;
	private final TransitRouterTravelTimeAndDisutilityFirstLastAVPT ttCalculator;
	private final Network cleanNetwork;


	public TransitRouterFirstLastAVPT(final TransitRouterConfig config, final TransitRouterTravelTimeAndDisutilityFirstLastAVPT ttCalculator,
									  final TransitRouterNetworkFirstLastAVPT routerNetwork, Network cleanNetwork) {
		this.config = config;
		this.transitNetwork = routerNetwork;
		this.ttCalculator = ttCalculator;
		this.dijkstra = new MultiNodeDijkstra(this.transitNetwork, this.ttCalculator, this.ttCalculator);
		PreProcessDijkstra preProcessDijkstra = new PreProcessDijkstra();
		preProcessDijkstra.run(routerNetwork);
		mDijkstra = new MultiDestinationDijkstra(routerNetwork, this.ttCalculator, this.ttCalculator, preProcessDijkstra);
		this.cleanNetwork = cleanNetwork;
	}
	
	private Map<Node, InitialNode>[] locateWrappedNearestTransitNodes(Person person, Coord coord, Id<Link> link, double departureTime) {
		Collection<TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkNode> nearestNodes = this.transitNetwork.getNearestNodes(coord, TransitRouterNetworkFirstLastAVPT.maxBeelineWalkConnectionDistance);
		if (nearestNodes.size() < 2) {
			// also enlarge search area if only one stop found, maybe a second one is near the border of the search area
			TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkNode nearestNode = this.transitNetwork.getNearestNode(coord);
			double distance = CoordUtils.calcEuclideanDistance(coord, nearestNode.stop.getCoord());
			nearestNodes = this.transitNetwork.getNearestNodes(coord, distance + this.config.getExtensionRadius());
		}
		Map<Node, InitialNode> wrappedNearestNodes = new LinkedHashMap<>();
		for (TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkNode node : nearestNodes) {
			Coord toCoord = node.stop.getCoord();
			double initialTime = getWalkTime(person, coord, toCoord);
			double initialCost = getWalkDisutility(person, coord, toCoord);
			wrappedNearestNodes.put(node, new InitialNode(initialCost, initialTime + departureTime));
		}
		Collection<TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkNode> nearestNodesAV = this.transitNetwork.getNearestAVNodes(coord, TransitRouterNetworkFirstLastAVPT.maxBeelineAVConnectionDistance);
		Map<Node, InitialNode> wrappedNearestNodesAV = new LinkedHashMap<>();
		if(transitNetwork.getNetworkModes()!= TransitRouterNetworkFirstLastAVPT.NetworkModes.PT)
			for (TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkNode node : nearestNodesAV) {
				double initialTime = getAVTime(person, link, node.stop.getLinkId(), departureTime);
				double initialCost = getAVDisutility(person, link, node.stop.getLinkId(), departureTime);
				wrappedNearestNodesAV.put(node, new InitialNode(initialCost, initialTime + departureTime));
			}
		return new Map[]{wrappedNearestNodes, wrappedNearestNodesAV};
	}
	
	private double getWalkTime(Person person, Coord coord, Coord toCoord) {
		return this.ttCalculator.getWalkTravelTime(person, coord, toCoord);
	}
	
	private double getWalkDisutility(Person person, Coord coord, Coord toCoord) {
		return this.ttCalculator.getWalkTravelDisutility(person, coord, toCoord);
	}

	private double getAVTime(Person person, Id<Link> linkA, Id<Link> linkB, double time) {
		return this.ttCalculator.getAVTravelTime(person, linkA, linkB, time);
	}

	private double getAVDisutility(Person person, Id<Link> linkA, Id<Link> linkB, double time) {
		return this.ttCalculator.getAVTaxiTravelDisutility(person, linkA, linkB, time);
	}
	
	public Map<Id<Node>, Path> calcPathRoutes(final Id<Node> fromNodeId, final Set<Id<Node>> toNodeIds, final double startTime, final Person person) {
		Set<Node> toNodes = new HashSet<>();
		for(Id<Node> toNode:toNodeIds)
			if(transitNetwork.getNodes().get(toNode)!=null)
				toNodes.add(transitNetwork.getNodes().get(toNode));
		Node node = transitNetwork.getNodes().get(fromNodeId);
		if(node!=null)
			return mDijkstra.calcLeastCostPath(node, toNodes, startTime, person);
		else
			return new HashMap<>();
	}
	@Override
	public List<PlanElement> calcRoute(final Facility fromFacility, final Facility toFacility, final double departureTime, final Person person) {
		// find possible start stops
		Map<Node, InitialNode>[] wrappedFromNodes = this.locateWrappedNearestTransitNodes(person, fromFacility.getCoord(), fromFacility.getLinkId(), departureTime);
		Map<Node, InitialNode> wrappedAllFromNodes = wrappedFromNodes[0];
		if(cleanNetwork.getLinks().containsKey(fromFacility.getLinkId())) {
			Set<Node> toRemove = new HashSet<>();
			for (Map.Entry<Node, InitialNode> entry : wrappedFromNodes[1].entrySet()) {
				InitialNode other = wrappedAllFromNodes.get(entry.getKey());
				if (other != null) {
					if (entry.getValue().initialCost < other.initialCost)
						wrappedAllFromNodes.put(entry.getKey(), entry.getValue());
					else
						toRemove.add(entry.getKey());
				} else
					wrappedAllFromNodes.put(entry.getKey(), entry.getValue());
			}
			for (Node node : toRemove)
				wrappedFromNodes[1].remove(node);
		}
		else
			wrappedFromNodes[1].clear();
		// find possible end stops
		Map<Node, InitialNode>[] wrappedToNodes  = this.locateWrappedNearestTransitNodes(person, toFacility.getCoord(), toFacility.getLinkId(), departureTime);
		Map<Node, InitialNode> wrappedAllToNodes = wrappedToNodes[0];
		if(cleanNetwork.getLinks().containsKey(toFacility.getLinkId())) {
			Set<Node> toRemove = new HashSet<>();
			for (Map.Entry<Node, InitialNode> entry : wrappedToNodes[1].entrySet()) {
				InitialNode other = wrappedAllToNodes.get(entry.getKey());
				if (other != null) {
					if (entry.getValue().initialCost < other.initialCost)
						wrappedAllToNodes.put(entry.getKey(), entry.getValue());
					else
						toRemove.add(entry.getKey());
				} else
					wrappedAllToNodes.put(entry.getKey(), entry.getValue());
			}
			for (Node node : toRemove)
				wrappedToNodes[1].remove(node);
		}
		else
			wrappedToNodes[1].clear();
		// find routes between start and end stops
		Path p = this.dijkstra.calcLeastCostPath(wrappedAllFromNodes, wrappedAllToNodes, person);
		if (p == null)
			return null;
		double pathCost = p.travelCost + wrappedAllFromNodes.get(p.nodes.get(0)).initialCost + wrappedAllToNodes.get(p.nodes.get(p.nodes.size() - 1)).initialCost + (p.links.size()>0?this.config.getUtilityOfLineSwitch_utl():0);
		double directWalkCost = getWalkDisutility(person,fromFacility.getCoord(), toFacility.getCoord());
		boolean avFrom = false;
		InitialNode initialNode = wrappedFromNodes[1].get(p.nodes.get(0));
		if(initialNode!=null) {
			InitialNode walkNode = wrappedFromNodes[0].get(p.nodes.get(0));
			if(walkNode!=null) {
				if(initialNode.initialCost<walkNode.initialCost)
					avFrom = true;
			}
			else
				avFrom = true;
		}
		boolean avTo = false;
		initialNode = wrappedToNodes[1].get(p.nodes.get(p.nodes.size()-1));
		if(initialNode!=null) {
			InitialNode walkNode = wrappedToNodes[0].get(p.nodes.get(p.nodes.size()-1));
			if(walkNode!=null) {
				if(initialNode.initialCost<walkNode.initialCost)
					avTo = true;
			}
			else
				avTo = true;
		}
		List<PlanElement> legs = new ArrayList<>();
		boolean direct = false;
		if (directWalkCost <= pathCost) {
			pathCost = directWalkCost;
			Leg leg = PopulationUtils.createLeg(TransportMode.transit_walk);
			double walkDistance = CoordUtils.calcEuclideanDistance(fromFacility.getCoord(), toFacility.getCoord())*1.3;
			Route walkRoute = RouteUtils.createGenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
			walkRoute.setDistance(walkDistance);
			leg.setRoute(walkRoute);
			leg.setTravelTime(walkDistance/this.config.getBeelineWalkSpeed());
			legs.add(leg);
			direct = true;
		}
		if(wrappedFromNodes[1].size()>0 && wrappedToNodes[1].size()>0 && transitNetwork.getNetworkModes()!=TransitRouterNetworkFirstLastAVPT.NetworkModes.PT && cleanNetwork.getLinks().containsKey(fromFacility.getLinkId()) && cleanNetwork.getLinks().containsKey(toFacility.getLinkId())) {
			double directAVCost = getAVDisutility(person, fromFacility.getLinkId(), toFacility.getLinkId(), departureTime);
			if(directAVCost < pathCost && !fromFacility.getLinkId().equals(toFacility.getLinkId())) {
				legs.clear();
				Leg leg = PopulationUtils.createLeg(AV_TAXI_MODE);
				double walkDistance = CoordUtils.calcEuclideanDistance(fromFacility.getCoord(), toFacility.getCoord()) * 1.3;
				Route walkRoute = RouteUtils.createGenericRouteImpl(fromFacility.getLinkId(), toFacility.getLinkId());
				walkRoute.setDistance(walkDistance);
				leg.setRoute(walkRoute);
				leg.setTravelTime(walkDistance / ttCalculator.avSpeed);
				legs.add(leg);
				direct = true;
			}
		}
		if(!direct)
			legs = convertPathToLegList(departureTime, p, fromFacility.getCoord(), toFacility.getCoord(), person, avFrom ? AV_TAXI_MODE : TransportMode.transit_walk, avTo ? AV_TAXI_MODE : TransportMode.transit_walk, fromFacility.getLinkId(), toFacility.getLinkId());
		return fillWithActivities(legs);
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return new StageActivityTypesImpl(PtConstants.TRANSIT_ACTIVITY_TYPE);
	}
	
	protected List<PlanElement> convertPathToLegList(double departureTime, Path p, Coord fromCoord, Coord toCoord, Person person, String mode, String finalMode, Id<Link> startLinkId, Id<Link> endLinkId) {
		List<PlanElement> legs = new ArrayList<>();
		Leg leg;
		double distance, moveTime, travelTime = 0;
		Route route;
		Coord coord = fromCoord;
		boolean start = true;
		TransitStopFacility stop = ((TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkNode)p.nodes.get(0)).stop;
		double time = departureTime;
		for (Link link : p.links) {
			TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkLink l = (TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkLink) link;
			if(l.route!=null) {
				//in line link
				double ttime = ttCalculator.getLinkTravelTime(l, time, person, null);
				travelTime += ttime;
				time += ttime;
			}
			else if(l.fromNode.route!=null && l.fromNode.line!=null) {
				//inside link
				start = false;
				mode = TransportMode.pt;
				leg = PopulationUtils.createLeg(mode);
				ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(stop, l.fromNode.line, l.fromNode.route, l.fromNode.stop);
				leg.setRoute(ptRoute);
				leg.setTravelTime(travelTime);
				legs.add(leg);
				travelTime = 0;
				stop = l.fromNode.stop;
			}
			else if(l.toNode.route!=null && l.toNode.line!=null) {
				//wait link
				Id<Link> startId = start ? startLinkId : stop.getLinkId(), endId = l.fromNode.stop.getLinkId();
				if(!startId.equals(endId)) {
					start = false;
					distance = CoordUtils.calcEuclideanDistance(coord, l.fromNode.getCoord());
					leg = PopulationUtils.createLeg(mode);
					moveTime = distance / (mode.equals(TransportMode.transit_walk) ? this.config.getBeelineWalkSpeed() : ttCalculator.avSpeed);
					route = RouteUtils.createGenericRouteImpl(startId, endId);
					route.setDistance(distance);
					leg.setRoute(route);
					leg.setTravelTime(moveTime);
					time += moveTime;
					legs.add(leg);
				}
				stop = l.fromNode.stop;
				coord = null;
			}
			else if(l.mode!=null) {
				if (!mode.equals("pt") && !mode.equals(l.mode)) {
					Id<Link> startId = start ? startLinkId : stop.getLinkId(), endId = l.fromNode.stop.getLinkId();
					if (startId != endId) {
						start = false;
						distance = CoordUtils.calcEuclideanDistance(coord, l.fromNode.getCoord());
						leg = PopulationUtils.createLeg(mode);
						moveTime = distance / (mode.equals(TransportMode.transit_walk) ? this.config.getBeelineWalkSpeed() : ttCalculator.avSpeed);
						route = RouteUtils.createGenericRouteImpl(startId, endId);
						route.setDistance(distance);
						leg.setRoute(route);
						leg.setTravelTime(moveTime);
						time += moveTime;
						legs.add(leg);
						coord = l.fromNode.getCoord();
						stop = l.fromNode.stop;
						mode = l.mode;
					}
				}
				if(coord==null) {
					coord = l.fromNode.getCoord();
					mode = l.mode;
				}
			}
		}
		if(!mode.equals(TransportMode.pt) && !mode.equals(finalMode)) {
			TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkNode n = ((TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkNode)p.nodes.get(p.nodes.size()-1));
			if(!stop.getLinkId().equals(n.stop.getLinkId())) {
				distance = CoordUtils.calcEuclideanDistance(coord, n.getCoord());
				leg = PopulationUtils.createLeg(mode);
				moveTime = distance / (mode.equals(TransportMode.transit_walk) ? this.config.getBeelineWalkSpeed() : ttCalculator.avSpeed);
				route = RouteUtils.createGenericRouteImpl(stop.getLinkId(), n.stop.getLinkId());
				route.setDistance(distance);
				leg.setRoute(route);
				leg.setTravelTime(moveTime);
				legs.add(leg);
			}
			coord = n.getCoord();
			stop = n.stop;
		}
		if(!stop.getLinkId().equals(endLinkId)) {
			if(coord==null) {
				TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkLink l = ((TransitRouterNetworkFirstLastAVPT.TransitRouterNetworkLink)p.links.get(p.links.size()-1));
				coord = l.toNode.getCoord();
			}
			distance = CoordUtils.calcEuclideanDistance(coord, toCoord);
			leg = PopulationUtils.createLeg(finalMode);
			moveTime = distance / (finalMode.equals(TransportMode.transit_walk) ? this.config.getBeelineWalkSpeed() : ttCalculator.avSpeed);
			route = RouteUtils.createGenericRouteImpl(stop.getLinkId(), endLinkId);
			route.setDistance(distance);
			leg.setRoute(route);
			leg.setTravelTime(moveTime);
			legs.add(leg);
		}
		return legs;
	}

	private List<PlanElement> fillWithActivities(List<PlanElement> baseTrip) {
		List<PlanElement> trip = new ArrayList();
		for(Iterator var10 = baseTrip.iterator(); var10.hasNext();) {
			Leg leg = (Leg)var10.next();
			trip.add(leg);
			Activity act = PopulationUtils.createActivityFromLinkId("pt interaction", leg.getRoute().getEndLinkId());
			act.setMaximumDuration(0.0D);
			trip.add(act);
		}
		if (trip.size() <=0)
			System.out.println();
		trip.remove(trip.size() - 1);
		return trip;
	}

	public TransitRouterNetworkFirstLastAVPT getTransitRouterNetwork() {
		return this.transitNetwork;
	}

	protected TransitRouterNetworkFirstLastAVPT getTransitNetwork() {
		return transitNetwork;
	}

	protected MultiNodeDijkstra getDijkstra() {
		return dijkstra;
	}

	protected TransitRouterConfig getConfig() {
		return config;
	}

}
