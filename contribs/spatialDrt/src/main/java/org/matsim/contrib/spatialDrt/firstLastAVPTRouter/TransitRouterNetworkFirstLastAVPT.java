/* *********************************************************************** *
 * project: org.matsim.*
 * TransitRouterNetworkWW.java
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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.api.*;
import org.matsim.utils.objectattributes.attributable.Attributes;

import java.util.*;

/**
 * Transit router network with travel, transfer, and waiting links
 * 
 * @author sergioo
 */

public final class TransitRouterNetworkFirstLastAVPT implements Network {

	private final static Logger log = Logger.getLogger(TransitRouterNetworkFirstLastAVPT.class);
	public static final double maxBeelineAVConnectionDistance = 2000;
	public static final double maxBeelineWalkConnectionDistance = 500;


	private final Map<Id<Link>, TransitRouterNetworkLink> links = new LinkedHashMap<>();
	private final Map<Id<Node>, TransitRouterNetworkNode> nodes = new LinkedHashMap<>();
	private final NetworkModes networkModes;
	protected QuadTree<TransitRouterNetworkNode> qtNodes = null;
	protected QuadTree<TransitRouterNetworkNode> qtNodesAV = null;

	private long nextNodeId = 0;
	protected long nextLinkId = 0;

	public TransitRouterNetworkFirstLastAVPT(NetworkModes networkModes) {
		this.networkModes = networkModes;
	}

	public NetworkModes getNetworkModes() {
		return networkModes;
	}

	public static final class TransitRouterNetworkNode implements Node {

		public final TransitStopFacility stop;
		public final TransitRoute route;
		public final TransitLine line;
		final Id<Node> id;
		final Map<Id<Link>, TransitRouterNetworkLink> ingoingLinks = new LinkedHashMap<Id<Link>, TransitRouterNetworkLink>();
		final Map<Id<Link>, TransitRouterNetworkLink> outgoingLinks = new LinkedHashMap<Id<Link>, TransitRouterNetworkLink>();

		public TransitRouterNetworkNode(final Id<Node> id, final TransitStopFacility stop, final TransitRoute route, final TransitLine line) {
			this.id = id;
			this.stop = stop;
			this.route = route;
			this.line = line;
		}

		@Override
		public Map<Id<Link>, ? extends Link> getInLinks() {
			return this.ingoingLinks;
		}

		@Override
		public Map<Id<Link>, ? extends Link> getOutLinks() {
			return this.outgoingLinks;
		}

		@Override
		public boolean addInLink(final Link link) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean addOutLink(final Link link) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Coord getCoord() {
			return this.stop.getCoord();
		}

		@Override
		public Id<Node> getId() {
			return this.id;
		}

		@Override
		public Link removeInLink(Id<Link> linkId) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public Link removeOutLink(Id<Link> outLinkId) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public void setCoord(Coord coord) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public Attributes getAttributes() {
			throw new UnsupportedOperationException();
		}
	}

	/**
	 * Looks to me like an implementation of the Link interface, with get(Transit)Route and get(Transit)Line on top.
	 * To recall: TransitLine is something like M44.  But it can have more than one route, e.g. going north, going south,
	 * long route, short route. That is, presumably we have one such TransitRouterNetworkLink per TransitRoute. kai/manuel, feb'12
	 */
	public static final class TransitRouterNetworkLink implements Link {

		final TransitRouterNetworkNode fromNode;
		final TransitRouterNetworkNode toNode;
		final TransitRoute route;
		final TransitLine line;
		final Id<Link> id;
		private double length;
		final String mode;

		public TransitRouterNetworkLink(final Id<Link> id, final TransitRouterNetworkNode fromNode, final TransitRouterNetworkNode toNode, final TransitRoute route, final TransitLine line, Network network, String mode) {
			this.id = id;
			this.fromNode = fromNode;
			this.toNode = toNode;
			this.route = route;
			this.line = line;
			if(route==null)
				this.length = CoordUtils.calcEuclideanDistance(this.toNode.stop.getCoord(), this.fromNode.stop.getCoord());
			else {
				this.length = 0;
				for(Id<Link> linkId:route.getRoute().getSubRoute(fromNode.stop.getLinkId(), toNode.stop.getLinkId()).getLinkIds())
					this.length += network.getLinks().get(linkId).getLength();
				this.length += network.getLinks().get(toNode.stop.getLinkId()).getLength();
			}
			this.mode = mode;
		}

		@Override
		public TransitRouterNetworkNode getFromNode() {
			return this.fromNode;
		}

		@Override
		public TransitRouterNetworkNode getToNode() {
			return this.toNode;
		}

		@Override
		public double getCapacity() {
			return getCapacity(Time.UNDEFINED_TIME);
		}

		@Override
		public double getCapacity(final double time) {
			return 9999;
		}

		@Override
		public double getFreespeed() {
			return getFreespeed(Time.UNDEFINED_TIME);
		}

		@Override
		public double getFreespeed(final double time) {
			return 10;
		}

		@Override
		public Id<Link> getId() {
			return this.id;
		}

		@Override
		public double getNumberOfLanes() {
			return getNumberOfLanes(Time.UNDEFINED_TIME);
		}

		@Override
		public double getNumberOfLanes(final double time) {
			return 1;
		}

		@Override
		public double getLength() {
			return this.length;
		}

		@Override
		public void setCapacity(final double capacity) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setFreespeed(final double freespeed) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean setFromNode(final Node node) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setNumberOfLanes(final double lanes) {
			throw new UnsupportedOperationException();
		}

		@Override
		public void setLength(final double length) {
			this.length = length;
		}

		@Override
		public boolean setToNode(final Node node) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Coord getCoord() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<String> getAllowedModes() {
			return null;
		}

		@Override
		public void setAllowedModes(final Set<String> modes) {
			throw new UnsupportedOperationException();
		}

		public TransitRoute getRoute() {
			return route;
		}

		public TransitLine getLine() {
			return line;
		}

		@Override
		public double getFlowCapacityPerSec() {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public double getFlowCapacityPerSec(double time) {
			// TODO Auto-generated method stub
			throw new RuntimeException("not implemented") ;
		}

		@Override
		public Attributes getAttributes() {
			throw new UnsupportedOperationException();
		}
	}
	public TransitRouterNetworkNode createNode(final TransitStopFacility stop, final TransitRoute route, final TransitLine line) {
		Id<Node> id = null;
		if(line==null && route==null)
			id = Id.createNodeId(stop.getId().toString());
		else
			id = Id.createNodeId("number:"+nextNodeId++);
		final TransitRouterNetworkNode node = new TransitRouterNetworkNode(id, stop, route, line);
		if(this.nodes.get(node.getId())!=null)
			throw new RuntimeException();
		this.nodes.put(node.getId(), node);
		return node;
	}

	public TransitRouterNetworkLink createLink(final Network network, final TransitRouterNetworkNode fromNode, final TransitRouterNetworkNode toNode, String mode) {
		final TransitRouterNetworkLink link = new TransitRouterNetworkLink(Id.createLinkId(this.nextLinkId++), fromNode, toNode, null, null, network, mode);
		this.links.put(link.getId(), link);
		fromNode.outgoingLinks.put(link.getId(), link);
		toNode.ingoingLinks.put(link.getId(), link);
		return link;
	}
	public TransitRouterNetworkLink createLink(final Network network, final TransitRouterNetworkNode fromNode, final TransitRouterNetworkNode toNode, final TransitRoute route, final TransitLine line) {
		final TransitRouterNetworkLink link = new TransitRouterNetworkLink(Id.createLinkId(this.nextLinkId++), fromNode, toNode, route, line, network, "pt");
		this.getLinks().put(link.getId(), link);
		fromNode.outgoingLinks.put(link.getId(), link);
		toNode.ingoingLinks.put(link.getId(), link);
		return link;
	}
	@Override
	public Map<Id<Node>, TransitRouterNetworkNode> getNodes() {
		return this.nodes;
	}
	@Override
	public Map<Id<Link>, TransitRouterNetworkLink> getLinks() {
		return this.links;
	}
	public void finishInit() {
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		for (TransitRouterNetworkNode node : getNodes().values())
			if(node.line == null) {
				Coord c = node.stop.getCoord();
				if (c.getX() < minX)
					minX = c.getX();
				if (c.getY() < minY)
					minY = c.getY();
				if (c.getX() > maxX)
					maxX = c.getX();
				if (c.getY() > maxY)
					maxY = c.getY();
			}
		QuadTree<TransitRouterNetworkNode> quadTree = new QuadTree<TransitRouterNetworkNode>(minX, minY, maxX, maxY);
		for (TransitRouterNetworkNode node : getNodes().values()) {
			if(node.line == null) {
				Coord c = node.stop.getCoord();
				quadTree.put(c.getX(), c.getY(), node);
			}
		}
		this.qtNodes = quadTree;
		minX = Double.POSITIVE_INFINITY;
		minY = Double.POSITIVE_INFINITY;
		maxX = Double.NEGATIVE_INFINITY;
		maxY = Double.NEGATIVE_INFINITY;
		for (TransitRouterNetworkNode node : getNodes().values())
			if(node.line == null && node.stop.getStopAreaId()!=null && node.stop.getStopAreaId().equals(Id.create("mp", TransitStopArea.class))) {
				Coord c = node.stop.getCoord();
				if (c.getX() < minX)
					minX = c.getX();
				if (c.getY() < minY)
					minY = c.getY();
				if (c.getX() > maxX)
					maxX = c.getX();
				if (c.getY() > maxY)
					maxY = c.getY();
			}
		quadTree = new QuadTree<>(minX, minY, maxX, maxY);
		for (TransitRouterNetworkNode node : getNodes().values()) {
			if(node.line == null && node.stop.getStopAreaId()!=null && node.stop.getStopAreaId().equals(Id.create("mp", TransitStopArea.class))) {
				Coord c = node.stop.getCoord();
				quadTree.put(c.getX(), c.getY(), node);
			}
		}
		this.qtNodesAV = quadTree;
	}
	public enum NetworkModes {
		PT,PT_AV,AV;
	}
	public static TransitRouterNetworkFirstLastAVPT createFromSchedule(final Network network, final TransitSchedule schedule, final double maxBeelineWalkConnectionDistance, NetworkModes networkModes) {
		log.info("start creating transit network");
		final TransitRouterNetworkFirstLastAVPT transitNetwork = new TransitRouterNetworkFirstLastAVPT(networkModes);
		final Counter linkCounter = new Counter(" link #");
		final Counter nodeCounter = new Counter(" node #");
		int numTravelLinks = 0, numWaitingLinks = 0, numInsideLinks = 0, numWalkTransferLinks = 0, numAVTransferLinks = 0;
		Map<Id<TransitStopFacility>, TransitRouterNetworkNode> stops = new HashMap<Id<TransitStopFacility>, TransitRouterNetworkNode>();
		TransitRouterNetworkNode nodeSR, nodeS;
		// build stop nodes
		for(TransitStopFacility stop:schedule.getFacilities().values()) {
			nodeS = stops.get(stop.getId());
			if(nodeS == null) {
				nodeS = transitNetwork.createNode(stop,null,null);
				nodeCounter.incCounter();
				stops.put(stop.getId(), nodeS);
			}
		}
		transitNetwork.finishInit();
		// build transfer links
		log.info("add walk transfer links");
		// connect all stops with walking links if they're located less than beelineWalkConnectionDistance from each other
		TransitRoute dummy = schedule.getTransitLines().values().iterator().next().getRoutes().values().iterator().next();
		Map<TransitRouterNetworkNode, Map<TransitRouterNetworkNode, TransitRouterNetworkNode>> nodesSR = new HashMap<>();
		for (TransitRouterNetworkNode node : transitNetwork.getNodes().values())
			for (TransitRouterNetworkNode node2 : transitNetwork.getNearestNodes(node.stop.getCoord(), maxBeelineWalkConnectionDistance))
				if (!node.getCoord().equals(node2.getCoord())) {
					Id<Node> id = Id.createNodeId(node2.stop.getId().toString() + "t");
					Map<TransitRouterNetworkNode, TransitRouterNetworkNode> map = nodesSR.get(node);
					if(map==null) {
						map = new HashMap<>();
						nodesSR.put(node,map);
					}
					map.put(node2, new TransitRouterNetworkNode(id, node2.stop, dummy, null));
				}
		if(networkModes == NetworkModes.PT_AV || networkModes == NetworkModes.AV) {
			log.info("add av transfer links");
			// connect all stops with av links if they're located less than beelineAVConnectionDistance from each other and both of them are located in the region of interest
			Map<TransitRouterNetworkNode, Map<TransitRouterNetworkNode, TransitRouterNetworkNode>> nodesSRAV = new HashMap<>();
			for(TransitRouterNetworkNode node : transitNetwork.getNodes().values())
				if(node.stop.getStopAreaId()!=null && node.stop.getStopAreaId().equals(Id.create("mp", TransitStopArea.class)))
					for(TransitRouterNetworkNode node2 : transitNetwork.getNearestNodes(node.stop.getCoord(), maxBeelineAVConnectionDistance))
						if (!node.getCoord().equals(node2.getCoord()))
							if (node2.stop.getStopAreaId()!=null && node2.stop.getStopAreaId().equals(Id.create("mp", TransitStopArea.class))) {
								Id<Node> id = Id.createNodeId(node2.stop.getId().toString() + "t");
								Map<TransitRouterNetworkNode, TransitRouterNetworkNode> map = nodesSRAV.get(node);
								if(map==null) {
									map = new HashMap<>();
									nodesSRAV.put(node,map);
								}
								map.put(node2, new TransitRouterNetworkNode(id, node2.stop, dummy, null));
							}
			for(Map.Entry<TransitRouterNetworkNode, Map<TransitRouterNetworkNode, TransitRouterNetworkNode>> triple:nodesSRAV.entrySet())
				for(Map.Entry<TransitRouterNetworkNode, TransitRouterNetworkNode> duple:triple.getValue().entrySet()) {
					TransitRouterNetworkNode node = triple.getKey();
					TransitRouterNetworkNode node2 = duple.getKey();
					nodeSR = duple.getValue();
					transitNetwork.createLink(network, node, nodeSR, TransitRouterFirstLastAVPT.AV_MODE);
					linkCounter.incCounter();
					numInsideLinks++;
					transitNetwork.createLink(network, nodeSR, node2, null);
					linkCounter.incCounter();
					numAVTransferLinks++;
				}
		}
		for(Map.Entry<TransitRouterNetworkNode, Map<TransitRouterNetworkNode, TransitRouterNetworkNode>> triple:nodesSR.entrySet())
			for(Map.Entry<TransitRouterNetworkNode, TransitRouterNetworkNode> duple:triple.getValue().entrySet()) {
				TransitRouterNetworkNode node = triple.getKey();
				TransitRouterNetworkNode node2 = duple.getKey();
				nodeSR = duple.getValue();
				transitNetwork.nodes.put(nodeSR.id, nodeSR);
				transitNetwork.createLink(network, node, nodeSR, TransportMode.transit_walk);
				linkCounter.incCounter();
				numInsideLinks++;
				transitNetwork.createLink(network, nodeSR, node2, null);
				linkCounter.incCounter();
				numWalkTransferLinks++;
			}
		// build nodes and links connecting the nodes according to the transit routes
		if(networkModes!= NetworkModes.AV) {
			log.info("add travel, waiting and inside links");
			for (TransitLine line : schedule.getTransitLines().values())
				for (TransitRoute route : line.getRoutes().values()) {
					TransitRouterNetworkNode prevNode = null;
					for (TransitRouteStop stop : route.getStops()) {
						nodeS = stops.get(stop.getStopFacility().getId());
						nodeSR = transitNetwork.createNode(stop.getStopFacility(), route, line);
						nodeCounter.incCounter();
						if (prevNode != null) {
							transitNetwork.createLink(network, prevNode, nodeSR, route, line);
							linkCounter.incCounter();
							numTravelLinks++;
						}
						prevNode = nodeSR;
						transitNetwork.createLink(network, nodeS, nodeSR, null);
						linkCounter.incCounter();
						numWaitingLinks++;
						transitNetwork.createLink(network, nodeSR, nodeS, null);
						linkCounter.incCounter();
						numInsideLinks++;
					}
				}
		}
		log.info("transit router network statistics:");
		log.info(" # nodes: " + transitNetwork.getNodes().size());
		log.info(" # links total:     " + transitNetwork.getLinks().size());
		log.info(" # travel links:  " + numTravelLinks);
		log.info(" # waiting links:  " + numWaitingLinks);
		log.info(" # inside links:  " + numInsideLinks);
		log.info(" # walk transfer links:  " + numWalkTransferLinks);
		log.info(" # av transfer links:  " + numAVTransferLinks);
		return transitNetwork;
	}
	public Collection<TransitRouterNetworkNode> getNearestNodes(final Coord coord, final double distance) {
		return this.qtNodes.getDisk(coord.getX(), coord.getY(), distance);
	}
	public Collection<TransitRouterNetworkNode> getNearestAVNodes(final Coord coord, final double distance) {
		return this.qtNodesAV.getDisk(coord.getX(), coord.getY(), distance);
	}

	public TransitRouterNetworkNode getNearestNode(final Coord coord) {
		return this.qtNodes.getClosest(coord.getX(), coord.getY());
	}

	@Override
	public double getCapacityPeriod() {
		return 3600.0;
	}

	@Override
	public NetworkFactory getFactory() {
		return null;
	}

	@Override
	public double getEffectiveLaneWidth() {
		return 3;
	}

	@Override
	public void addNode(Node nn) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addLink(Link ll) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Link removeLink(Id<Link> linkId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Node removeNode(Id<Node> nodeId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setCapacityPeriod(double capPeriod) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void setEffectiveCellSize(double effectiveCellSize) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void setEffectiveLaneWidth(double effectiveLaneWidth) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public void setName(String name) {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public double getEffectiveCellSize() {
		// TODO Auto-generated method stub
		throw new RuntimeException("not implemented") ;
	}

	@Override
	public Attributes getAttributes() {
		throw new UnsupportedOperationException();
	}
}
