/* *********************************************************************** *
 * project: org.matsim.*
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

import com.google.inject.Inject;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.contrib.spatialDrt.firstLastAVPTRouter.linkLinkTimes.LinkLinkTime;
import org.matsim.contrib.spatialDrt.firstLastAVPTRouter.waitLinkTime.WaitLinkTime;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.pt.router.PreparedTransitSchedule;
import org.matsim.pt.router.TransitRouterConfig;

import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Collections;

/**
 * Factory for the variable transit router
 * 
 * @author sergioo
 */
@Singleton
public class TransitRouterFirstLastAVPTFactory implements Provider<TransitRouterFirstLastAVPT> {

	private final TransitRouterConfig config;
	private final TransitRouterNetworkFirstLastAVPT routerNetwork;
	private final Scenario scenario;

	private final WaitTime waitTime;
	private final WaitTime waitTimeAV;
	private final WaitLinkTime waitLinkTimeAV;
    private final StopStopTime stopStopTime;
	private final StopStopTime stopStopTimeAV;
	private final LinkLinkTime linkLinkTimeAV;
	private  Network cleanNetwork;
	private TransitRouterParams params;

	@Inject
    public TransitRouterFirstLastAVPTFactory(final Scenario scenario, final WaitTime waitTime, final WaitTime waitTimeAV, final WaitLinkTime waitLinkTime, final StopStopTime stopStopTime, final StopStopTime stopStopTimeAV, final LinkLinkTime linkLinkTime, final TransitRouterNetworkFirstLastAVPT.NetworkModes networkModes) {
		this.config = new TransitRouterConfig(scenario.getConfig().planCalcScore(),
				scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(),
				scenario.getConfig().vspExperimental());
		this.config.setBeelineWalkConnectionDistance(300.0);
		routerNetwork = TransitRouterNetworkFirstLastAVPT.createFromSchedule(scenario.getNetwork(), scenario.getTransitSchedule(), this.config.getBeelineWalkConnectionDistance(), networkModes);
		this.scenario = scenario;
		this.waitTime = waitTime;
		this.waitTimeAV = waitTimeAV;
		this.waitLinkTimeAV = waitLinkTime;
		this.stopStopTime = stopStopTime;
		this.stopStopTimeAV = stopStopTimeAV;
		this.linkLinkTimeAV = linkLinkTime;
		cleanNetwork = NetworkUtils.createNetwork();
		new TransportModeNetworkFilter(scenario.getNetwork()).filter(cleanNetwork, Collections.singleton("car"));
		(new NetworkCleaner()).run(cleanNetwork);
		this.params = new TransitRouterParams(scenario.getConfig().planCalcScore());
	}
	@Override
	public TransitRouterFirstLastAVPT get() {
		return new TransitRouterFirstLastAVPT( config, new TransitRouterTravelTimeAndDisutilityFirstLastAVPT(params, config, routerNetwork, waitTime, waitTimeAV, waitLinkTimeAV, stopStopTime, stopStopTimeAV, linkLinkTimeAV, scenario.getConfig().travelTimeCalculator(), scenario.getConfig().qsim(), new PreparedTransitSchedule(scenario.getTransitSchedule())), routerNetwork, cleanNetwork);
	}
}
