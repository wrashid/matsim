/* *********************************************************************** *
 * project: org.matsim.*
 * RunReceiver.java
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

/**
 * 
 */
package org.matsim.contrib.freight.usecases.receiver;

import java.io.File;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.io.ReceiversReader;
import org.matsim.contrib.freight.io.ReceiversWriter;
import org.matsim.contrib.freight.receiver.ReceiverModule;
import org.matsim.contrib.freight.receiver.ReceiverOrderStrategyManagerFactory;
import org.matsim.contrib.freight.receiver.ReceiverScoringFunctionFactory;
import org.matsim.contrib.freight.receiver.Receivers;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.replanning.modules.ReRouteVehicles;
import org.matsim.contrib.freight.replanning.modules.TimeAllocationMutator;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.usecases.analysis.CarrierScoreStats;
import org.matsim.contrib.freight.usecases.analysis.LegHistogram;
import org.matsim.contrib.freight.usecases.chessboard.TravelDisutilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *
 * @author jwjoubert, wlbean
 */
public class RunReceiver {
	final private static long SEED_BASE = 20180413l;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		int numberOfRuns = Integer.parseInt(args[0]);
		for(int i = 0; i < numberOfRuns; i++) {
			run(i);
		}
	}


	public static void run(int run) {
		String outputfolder = String.format("./output/run_%03d/", run);
		new File(outputfolder).mkdirs();

		ReceiverChessboardScenario.createChessboardScenario(SEED_BASE*run, run);

		/* Read basic scenario elements. */
		Config config = ConfigUtils.loadConfig(outputfolder + "config.xml");
		config.controler().setOutputDirectory(outputfolder + "./output/");
		config.network().setInputFile("../../input/usecases/chessboard/network/grid9x9.xml");
		config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(20);
		config.controler().setMobsim("qsim");
		config.controler().setWriteSnapshotsInterval(1);
		Scenario sc = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(sc);

		/* Set up freight portion. */
		Carriers carriers = setupCarriers(controler, outputfolder);
		Receivers receivers = setupReceivers(controler, carriers, outputfolder);

		/* TODO This stats must be set up automatically. */
		prepareFreightOutputDataAndStats(controler, carriers, outputfolder, receivers);
		controler.run();
	}


	private static Carriers setupCarriers(Controler controler, String outputfolder) {
		final Carriers carriers = new Carriers();							
		new CarrierPlanXmlReaderV2(carriers).readFile(outputfolder + "carriers.xml");	
		CarrierVehicleTypes types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).readFile(outputfolder + "carrierVehicleTypes.xml");
		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);

		/* Create a new instance of a carrier scoring function factory. */
		final CarrierScoringFunctionFactory cScorFuncFac = new MyCarrierScoringFunctionFactoryImpl(controler.getScenario().getNetwork());

		/* Create a new instance of a carrier plan strategy manager factory. */
		final CarrierPlanStrategyManagerFactory cStratManFac = new MyCarrierPlanStrategyManagerFactoryImpl(types, controler.getScenario().getNetwork(), controler);

		CarrierModule carrierControler = new CarrierModule(carriers, cStratManFac, cScorFuncFac);
		carrierControler.setPhysicallyEnforceTimeWindowBeginnings(true);
		controler.addOverridingModule(carrierControler);
		return carriers;
	}


	private static Receivers setupReceivers(Controler controler, Carriers carriers, String outputfolder) {
		final Receivers finalReceivers = new Receivers();
		new ReceiversReader(finalReceivers).readFile(outputfolder + "receivers.xml");
		finalReceivers.linkReceiverOrdersToCarriers(carriers);

		/*
		 * Create a new instance of a receiver scoring function factory.
		 */
		final ReceiverScoringFunctionFactory rScorFuncFac = new MyReceiverScoringFunctionFactoryImpl();

		/*
		 * Create a new instance of a receiver plan strategy manager factory.
		 */
		final ReceiverOrderStrategyManagerFactory rStratManFac = new MyReceiverOrderStrategyManagerFactorImpl();
		ReceiverModule receiverControler = new ReceiverModule(finalReceivers, rScorFuncFac, rStratManFac);
		controler.addOverridingModule(receiverControler);
		return finalReceivers;
	}


	private static class MyCarrierPlanStrategyManagerFactoryImpl implements CarrierPlanStrategyManagerFactory {

		/*
		 * Adapted from RunChessboard.java by sschroeder and gliedtke.
		 */
		private Network network;
		private MatsimServices controler;
		private CarrierVehicleTypes types;

		public MyCarrierPlanStrategyManagerFactoryImpl(final CarrierVehicleTypes types, final Network network, final MatsimServices controler) {
			this.types = types;
			this.network = network;
			this.controler= controler;
		}

		@Override
		public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
			TravelDisutility travelDis = TravelDisutilities.createBaseDisutility(types, controler.getLinkTravelTimes());
			final LeastCostPathCalculator router = controler.getLeastCostPathCalculatorFactory().createPathCalculator(network,	travelDis, controler.getLinkTravelTimes());

			final GenericStrategyManager<CarrierPlan, Carrier> strategyManager = new GenericStrategyManager<>();
			strategyManager.setMaxPlansPerAgent(5);
			{
				GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>(new ExpBetaPlanChanger<CarrierPlan, Carrier>(1.));
				strategyManager.addStrategy(strategy, null, 1.0);
			}

			{
				GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>(new KeepSelected<CarrierPlan, Carrier>());
				strategy.addStrategyModule(new TimeAllocationMutator());
				ReRouteVehicles reRouteModule = new ReRouteVehicles(router, network, controler.getLinkTravelTimes(), 1.);
				strategy.addStrategyModule(reRouteModule);
				strategyManager.addStrategy(strategy, null, 0.5);
			}
			return strategyManager;
		}
	}


	private static void prepareFreightOutputDataAndStats(MatsimServices controler, final Carriers carriers, String outputDir, final Receivers receivers) {
		/*
		 * Adapted from RunChessboard.java by sshroeder and gliedtke.
		 */
		final int statInterval = 1;
		final LegHistogram freightOnly = new LegHistogram(20);

		// freightOnly.setPopulation(controler.getScenario().getPopulation());
		//freightOnly.setInclPop(false);

		CarrierScoreStats scoreStats = new CarrierScoreStats(carriers, outputDir + "/carrier_scores", true);
		ReceiverScoreStats rScoreStats = new ReceiverScoreStats(receivers, outputDir + "/receiver_scores", true);

		controler.getEvents().addHandler(freightOnly);
		controler.addControlerListener(scoreStats);
		controler.addControlerListener(rScoreStats);
		controler.addControlerListener(new IterationEndsListener() {

			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				if(event.getIteration() % statInterval != 0) return;
				//write plans
				String dir = event.getServices().getControlerIO().getIterationPath(event.getIteration());
				new CarrierPlanXmlWriterV2(carriers).write(dir + "/" + event.getIteration() + ".carrierPlans.xml");

				new ReceiversWriter(receivers).write(dir + "/" + event.getIteration() + ".receivers.xml");

				//write stats
				freightOnly.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_freight.png");
				freightOnly.reset(event.getIteration());
			}
		});		
	}



}
