/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.spatialDrt.run;



import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.insertion.ParallelPathDataProvider;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.drt.routing.DrtRouteFactory;
import org.matsim.contrib.drt.routing.DrtStageActivityType;
import org.matsim.contrib.drt.run.DrtConfigConsistencyChecker;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculatorImpl;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeStuckCalculator;
import org.matsim.contrib.otfvis.OTFVisLiveModule;
import org.matsim.contrib.spatialDrt.firstLastAVPTRouter.MainModeIdentifierFirstLastAVPT;
import org.matsim.contrib.spatialDrt.firstLastAVPTRouter.TransitRouterFirstLastAVPTFactory;
import org.matsim.contrib.spatialDrt.firstLastAVPTRouter.TransitRouterNetworkFirstLastAVPT;
import org.matsim.contrib.spatialDrt.firstLastAVPTRouter.linkLinkTimes.LinkLinkTimeCalculatorAV;
import org.matsim.contrib.spatialDrt.firstLastAVPTRouter.stopStopTimes.StopStopTimeCalculatorAV;
import org.matsim.contrib.spatialDrt.firstLastAVPTRouter.waitLinkTime.WaitLinkTimeCalculatorAV;
import org.matsim.contrib.spatialDrt.firstLastAVPTRouter.waitTimes.WaitTimeCalculatorAV;
import org.matsim.contrib.spatialDrt.parkingAnalysis.DrtAnalysisModule;
import org.matsim.contrib.spatialDrt.parkingStrategy.alwaysRoaming.zoneBasedRoaming.DrtZonalModule;
import org.matsim.contrib.spatialDrt.parkingStrategy.insertionOptimizer.DefaultUnplannedRequestInserter;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ModeParams;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.pt.transitSchedule.api.TransitStopArea;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * @author jbischoff
 *
 */
public final class DrtControlerCreator {

	public static Scenario createScenarioWithDrtRouteFactory(Config config) {
		Scenario scenario = ScenarioUtils.createScenario(config);
		RouteFactories routeFactories = scenario.getPopulation().getFactory().getRouteFactories();
		routeFactories.setRouteFactory(DrtRoute.class, new DrtRouteFactory());
		return scenario;
	}

	/**
	 * Creates a controller in one step.
	 *
	 * @param config
	 * @param otfvis
	 * @return
	 */
	public static Controler createControler(Config config, boolean otfvis) throws IOException {
		return createControler(config, otfvis, cfg -> {
			Scenario scenario = createScenarioWithDrtRouteFactory(cfg);
			ScenarioUtils.loadScenario(scenario);
			return scenario;
		});
	}

	/**
	 * Creates a controller in one step. Allows for customised scenario creation.
	 *
	 * @param config
	 * @param otfvis
	 * @param scenarioLoader
	 * @return
	 */
	public static Controler createControler(Config config, boolean otfvis, Function<Config, Scenario> scenarioLoader) throws IOException {
		adjustDrtConfig(config);
		Scenario scenario = scenarioLoader.apply(config);
		Controler controler = new Controler(scenario);
		final WaitTimeStuckCalculator waitTimeCalculator = new WaitTimeStuckCalculator(scenario.getPopulation(), scenario.getTransitSchedule(), scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (scenario.getConfig().qsim().getEndTime()-scenario.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(waitTimeCalculator);
		final WaitTimeCalculatorAV waitTimeCalculatorAV = new WaitTimeCalculatorAV(scenario.getPopulation(), scenario.getTransitSchedule(), scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (scenario.getConfig().qsim().getEndTime()-scenario.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(waitTimeCalculatorAV);
		final WaitLinkTimeCalculatorAV waitLinkTimeCalculatorAV = new WaitLinkTimeCalculatorAV(scenario.getPopulation(), scenario.getNetwork(), scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (scenario.getConfig().qsim().getEndTime()-scenario.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(waitLinkTimeCalculatorAV);
		final StopStopTimeCalculatorImpl stopStopTimeCalculator = new StopStopTimeCalculatorImpl(scenario.getTransitSchedule(), scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (scenario.getConfig().qsim().getEndTime()-scenario.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(stopStopTimeCalculator);
		final StopStopTimeCalculatorAV stopStopTimeCalculatorAV = new StopStopTimeCalculatorAV(scenario.getTransitSchedule(), scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (scenario.getConfig().qsim().getEndTime()-scenario.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(stopStopTimeCalculatorAV);
		final LinkLinkTimeCalculatorAV linkLinkTimeCalculatorAV = new LinkLinkTimeCalculatorAV(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator().getTraveltimeBinSize(), (int) (scenario.getConfig().qsim().getEndTime()-scenario.getConfig().qsim().getStartTime()));
		controler.getEvents().addHandler(linkLinkTimeCalculatorAV);
		final TravelTimeCalculator travelTimeCalculator = new TravelTimeCalculator(scenario.getNetwork(), scenario.getConfig().travelTimeCalculator());

		//String EVENTSFILE = "/home/ubuntu/data/biyu/IdeaProjects/NewParking/out/artifacts/output/drt_mix_V450_T250_bay_optimal/ITERS/it.40/40.events.xml.gz";
		String EVENTSFILE = "/home/biyu/IdeaProjects/NewParking/output/drt_mix_V450_T250_bay_optimal/ITERS/it.40/40.events.xml.gz";
		EventsManager manager = EventsUtils.createEventsManager();
//		manager.addHandler(waitTimeCalculator);
//		manager.addHandler(waitLinkTimeCalculatorAV);
//		manager.addHandler(waitTimeCalculatorAV);
//		manager.addHandler(stopStopTimeCalculator);
//		manager.addHandler(stopStopTimeCalculatorAV);
//		manager.addHandler(linkLinkTimeCalculatorAV);
		manager.addHandler(travelTimeCalculator);

		AtodConfigGroup atodCfg = (AtodConfigGroup) scenario.getConfig().getModule(AtodConfigGroup.GROUP_NAME);

		BufferedReader reader = new BufferedReader(new InputStreamReader(atodCfg.getStopInAreaURL(config.getContext()).openStream()));
		String line = reader.readLine();
		Set<Id<TransitStopFacility>> ids = new HashSet<>();
		while(line!=null) {
			ids.add(Id.create(line, TransitStopFacility.class));
			line = reader.readLine();

		}
		reader.close();
		for(TransitStopFacility stop:scenario.getTransitSchedule().getFacilities().values()) {
			IDS:
			for(Id<TransitStopFacility> id:ids)
				if(stop.getId().equals(id)) {
					stop.setStopAreaId(Id.create("mp", TransitStopArea.class));
					break IDS;
				}
		}
		addDrtAsSingleDvrpModeToControler(controler);
		if (otfvis) {
			controler.addOverridingModule(new OTFVisLiveModule());
		}
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addTravelTimeBinding(DvrpTravelTimeModule.DVRP_INITIAL).toInstance(travelTimeCalculator.getLinkTravelTimes());
				bind(MainModeIdentifier.class).toInstance(new MainModeIdentifierFirstLastAVPT(new HashSet<>(Arrays.asList("pvt","taxi","walk"))));
				addRoutingModuleBinding("pt").toProvider(new TransitRouterFirstLastAVPTFactory(scenario, waitTimeCalculator.get(), waitTimeCalculatorAV.get(), waitLinkTimeCalculatorAV.get(), stopStopTimeCalculator.get(), stopStopTimeCalculatorAV.get(), linkLinkTimeCalculatorAV.get(), TransitRouterNetworkFirstLastAVPT.NetworkModes.PT_AV));
			}

		});
		return controler;
	}

	public static void addDrtAsSingleDvrpModeToControler(Controler controler) {
		addDrtWithoutDvrpModuleToControler(controler);
		controler.addOverridingModule(DvrpModule.createModule(DrtConfigGroup.get(controler.getConfig()).getMode(),
				Arrays.asList(DrtOptimizer.class, DefaultUnplannedRequestInserter.class,
						ParallelPathDataProvider.class)));
	}

	public static void addDrtWithoutDvrpModuleToControler(Controler controler) {
		controler.addOverridingModule(new DrtZonalModule());
		controler.addQSimModule(new DrtQSimModule());
		controler.addOverridingModule(new DrtModule());
		controler.addOverridingModule(new DrtAnalysisModule());
	}

	public static void adjustDrtConfig(Config config) {
		DrtConfigGroup drtCfg = DrtConfigGroup.get(config);
		if (drtCfg.getOperationalScheme().equals(DrtConfigGroup.OperationalScheme.stopbased)) {
			if (config.planCalcScore().getActivityParams(DrtStageActivityType.DRT_STAGE_ACTIVITY) == null) {
				addDrtStageActivityParams(config);
			}
		}
		if (!config.planCalcScore().getModes().containsKey(DrtStageActivityType.DRT_WALK)) {
			addDrtWalkModeParams(config);
		}

		config.addConfigConsistencyChecker(new SpatialDrtConfigConsistencyChecker());
		config.checkConsistency();
	}

	private static void addDrtStageActivityParams(Config config) {
		ActivityParams params = new ActivityParams(DrtStageActivityType.DRT_STAGE_ACTIVITY);
		params.setTypicalDuration(1);
		params.setScoringThisActivityAtAll(false);
		config.planCalcScore()
				.getScoringParametersPerSubpopulation()
				.values()
				.forEach(k -> k.addActivityParams(params));
		config.planCalcScore().addActivityParams(params);
		Logger.getLogger(org.matsim.contrib.drt.run.DrtControlerCreator.class)
				.info("drt interaction scoring parameters not set. Adding default values (activity will not be scored).");
	}

	private static void addDrtWalkModeParams(Config config) {
		ModeParams drtWalk = new ModeParams(DrtStageActivityType.DRT_WALK);
		ModeParams walk = config.planCalcScore().getModes().get(TransportMode.walk);
		drtWalk.setConstant(walk.getConstant());
		drtWalk.setMarginalUtilityOfDistance(walk.getMarginalUtilityOfDistance());
		drtWalk.setMarginalUtilityOfTraveling(walk.getMarginalUtilityOfTraveling());
		drtWalk.setMonetaryDistanceRate(walk.getMonetaryDistanceRate());
		config.planCalcScore().getScoringParametersPerSubpopulation().values().forEach(k -> k.addModeParams(drtWalk));
		Logger.getLogger(org.matsim.contrib.drt.run.DrtControlerCreator.class)
				.info("drt_walk scoring parameters not set. Adding default values (same as for walk mode).");
	}

}
