/* *********************************************************************** *
 * project: org.matsim.*
 * FastDijkstraFactory.java
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

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LinkToLinkRoutingNetwork;
import org.matsim.core.router.util.LinkToLinkRoutingNetworkFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.RoutingNetworkNode;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TurnRestrictedLeastCostPathCalculator;
import org.matsim.core.router.util.TurnRestrictedLeastCostPathCalculatorFactory;
import org.matsim.lanes.data.Lanes;

@Singleton
public class TurnRestrictedFastDijkstraFactory implements TurnRestrictedLeastCostPathCalculatorFactory {
	
	private final boolean usePreProcessData;
	private final LinkToLinkRoutingNetworkFactory routingNetworkFactory;
	private final Map<Network, LinkToLinkRoutingNetwork> routingNetworks = new HashMap<>();	// TODO: should also depend on the Lanes
	private final Map<Network, PreProcessDijkstra> preProcessData = new HashMap<>();	// TODO: should also depend on the Lanes

	@Inject
	public TurnRestrictedFastDijkstraFactory() {
		this(false);
	}

    public TurnRestrictedFastDijkstraFactory(final boolean usePreProcessData) {
    	this.usePreProcessData = usePreProcessData;
    	this.routingNetworkFactory = new LinkToLinkRoutingNetworkFactory();
	}

	@Override
	public synchronized TurnRestrictedLeastCostPathCalculator createPathCalculator(final Network network, final Lanes lanes, final TravelDisutility travelCosts, final TravelTime travelTimes) {
		LinkToLinkRoutingNetwork routingNetwork = this.routingNetworks.get(network);
		PreProcessDijkstra preProcessDijkstra = this.preProcessData.get(network);
		
		if (routingNetwork == null) {
			routingNetwork = this.routingNetworkFactory.createRoutingNetwork(network, lanes);
			
			if (this.usePreProcessData) {
				if (preProcessDijkstra == null) {
					preProcessDijkstra = new PreProcessDijkstra();
					preProcessDijkstra.run(network);
					this.preProcessData.put(network, preProcessDijkstra);
					
					for (RoutingNetworkNode node : routingNetwork.getNodes().values()) {
						node.setDeadEndData(preProcessDijkstra.getNodeData(node.getNode()));
					}
				}
			}
			
			this.routingNetworks.put(network, routingNetwork);
		}
		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();
		
		return new TurnRestrictedFastDijkstra(routingNetwork, travelCosts, travelTimes, preProcessDijkstra, fastRouterFactory);
	}
}