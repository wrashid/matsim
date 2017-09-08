/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractRoutingNetwork.java
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

package org.matsim.core.router.util;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.utils.objectattributes.attributable.Attributes;

public abstract class LinkToLinkAbstractRoutingNetwork implements LinkToLinkRoutingNetwork {
	
	/*package*/ final Map<Id<Node>, RoutingNetworkNode> nodes = new LinkedHashMap<>();	// needs to be a LinkedHashMap since the order is relevant for the router-preprocessing!
	/*package*/ final Map<Id<Link>, LinkToLinkRoutingNetworkLink> links = new HashMap<>();
	/*package*/ final Network network;
	/*package*/ PreProcessDijkstra preProcessData;
	/*package*/ transient boolean isInitialized = false;
	
	public LinkToLinkAbstractRoutingNetwork(Network network) {
		this.network = network;
	}
	
	@Override
	public void initialize() {
		// Some classes might override this method and do some additional stuff...
		this.isInitialized = true;
	}
	
	@Override
	public NetworkFactory getFactory() {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public Map<Id<Node>, RoutingNetworkNode> getNodes() {
		return this.nodes;
	}

	public void addNode(RoutingNetworkNode nn) {
		this.nodes.put(nn.getId(), nn);
	}
	
	@Override
	public RoutingNetworkNode removeNode(Id<Node> nodeId) {
		return this.nodes.remove(nodeId);
	}

	/*package*/ void addLink(LinkToLinkRoutingNetworkLink ll) {
		this.links.put(ll.getId(), ll);
	}

	@Override
	public void addNode(Node nn) {
		throw new RuntimeException("Not supported operation!");
	}
	
	@Override
	public void addLink(Link ll) {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public double getCapacityPeriod() {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public double getEffectiveLaneWidth() {
		throw new RuntimeException("Not supported operation!");
	}

	@Override
	public Map<Id<Link>, LinkToLinkRoutingNetworkLink> getLinks() {
		return this.links;
	}

	@Override
	public Link removeLink(Id<Link> linkId) {
		return this.links.remove(linkId);
	}
	@Override
	public void setCapacityPeriod(double capPeriod) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void setEffectiveCellSize(double effectiveCellSize) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public void setEffectiveLaneWidth(double effectiveLaneWidth) {
		throw new RuntimeException("not implemented");
	}
	@Override
	public double getEffectiveCellSize() {
		throw new RuntimeException("not implemented");
	}

	@Override
	public Attributes getAttributes() {
		return this.network.getAttributes();
	}
}