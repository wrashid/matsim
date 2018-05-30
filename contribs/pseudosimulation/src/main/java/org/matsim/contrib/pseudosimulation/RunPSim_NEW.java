/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *  * ***********************************************************************
 */

package org.matsim.contrib.pseudosimulation;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculator;
import org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformanceRecorder;
import org.matsim.contrib.pseudosimulation.mobsim.PSimProvider;
import org.matsim.contrib.pseudosimulation.mobsim.SwitchingMobsimProvider;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.contrib.pseudosimulation.searchacceleration.AccelerationConfigGroup;
import org.matsim.contrib.pseudosimulation.searchacceleration.AcceptIntendedReplanningStragetyProvider;
import org.matsim.contrib.pseudosimulation.searchacceleration.AcceptIntendedReplanningStrategy;
import org.matsim.contrib.pseudosimulation.searchacceleration.ConstantReplanningParameters;
import org.matsim.contrib.pseudosimulation.searchacceleration.ReplanningParameterContainer;
import org.matsim.contrib.pseudosimulation.searchacceleration.SearchAccelerator;
import org.matsim.contrib.pseudosimulation.searchacceleration.SearchAcceleratorModule;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimStopStopTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimTravelTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimWaitTimeCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.pt.router.TransitRouter;

import com.google.inject.Singleton;

import floetteroed.utilities.TimeDiscretization;

/**
 * @author pieterfourie
 * @author Gunnar Flötteröd
 * 
 */
public class RunPSim_NEW {
	
	private Config config;
	private Scenario scenario;
	private TransitPerformanceRecorder transitPerformanceRecorder;
	private Controler matsimControler;

	private PlanCatcher plancatcher;
	private PSimProvider pSimProvider;

	public RunPSim_NEW(final Config config, final PSimConfigGroup pSimConfigGroup,
			final AccelerationConfigGroup accelerationConfigGroup) {

		// pSim.
		
		this.config = config;
		this.scenario = ScenarioUtils.loadScenario(config);

		// The following line will make the controler use the events manager that
		// doesn't check for event order.
		// This is essential for pseudo-simulation as the PSim module generates events
		// on a person-basis,
		// not a system basis
		config.parallelEventHandling().setSynchronizeOnSimSteps(false);
		config.parallelEventHandling().setNumberOfThreads(1);

		this.matsimControler = new Controler(this.scenario);

		final MobSimSwitcher mobSimSwitcher = new MobSimSwitcher(pSimConfigGroup, this.scenario);
		this.matsimControler.addControlerListener(mobSimSwitcher);

		this.matsimControler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				bind(MobSimSwitcher.class).toInstance(mobSimSwitcher);
				bindMobsim().toProvider(SwitchingMobsimProvider.class);
				bind(WaitTimeCalculator.class).to(PSimWaitTimeCalculator.class);
				bind(WaitTime.class).toProvider(PSimWaitTimeCalculator.class);
				bind(StopStopTimeCalculator.class).to(PSimStopStopTimeCalculator.class);
				bind(StopStopTime.class).toProvider(PSimStopStopTimeCalculator.class);
				bind(TravelTimeCalculator.class).to(PSimTravelTimeCalculator.class);
				bind(TravelTime.class).toProvider(PSimTravelTimeCalculator.class);
				bind(TransitRouter.class).toProvider(TransitRouterEventsWSFactory.class);
				bind(PlanCatcher.class).toInstance(new PlanCatcher());
				bind(PSimProvider.class).toInstance(new PSimProvider(scenario, matsimControler.getEvents()));
			}
		});		
		
		if (config.transit().isUseTransit()) {
			if (pSimConfigGroup.isFullTransitPerformanceTransmission()) {
				transitPerformanceRecorder = new TransitPerformanceRecorder(matsimControler.getScenario(),
						matsimControler.getEvents(), mobSimSwitcher);
			}
		}
		
		// Re-planner selection.
		
		final TimeDiscretization timeDiscr = accelerationConfigGroup.getTimeDiscretization();
		final ReplanningParameterContainer replanningParameterProvider = new ConstantReplanningParameters(
				accelerationConfigGroup, this.scenario.getNetwork());
		this.matsimControler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.bind(TimeDiscretization.class).toInstance(timeDiscr);
				this.bind(ReplanningParameterContainer.class).toInstance(replanningParameterProvider);
				this.bind(SearchAccelerator.class).in(Singleton.class);
				this.addControlerListenerBinding().to(SearchAccelerator.class);
				this.addEventHandlerBinding().to(SearchAccelerator.class);
				this.addPlanStrategyBinding(AcceptIntendedReplanningStrategy.STRATEGY_NAME)
						.toProvider(AcceptIntendedReplanningStragetyProvider.class);
			}
		});
	}

	public void run() {
		this.matsimControler.run();
	}

	public static void main(String args[]) {

		/*
		 * Load configuration file. Add one configuration mode for the
		 * AcceptIntendedReplanningStrategy that only clones previously made
		 * hypothetical re-planning decisions (evaluated within the search acceleration
		 * framework) is added. To switch it off/almost certainly on, its weight is set
		 * to either zero or a very large number within the accelerated simulation
		 * process.
		 * 
		 * TODO It seems as if the pSim does not interpret the simulation end time
		 * "00:00:00" (which appears to indicate "run until everyone is done") right.
		 */

		final Config config;
		config = ConfigUtils.loadConfig("/Users/GunnarF/NoBackup/data-workspace/searchacceleration"
				+ "/rerun-2015-11-23a_No_Toll_large/matsim-config.xml");

		AcceptIntendedReplanningStrategy.addOwnStrategySettings(config);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.qsim().setEndTime(30 * 3600);
		// config.controler().setCreateGraphs(false);

		/*
		 * Run pSim/acceleration.
		 */

		final PSimConfigGroup pSimConfigGroup = ConfigUtils.addOrGetModule(config, PSimConfigGroup.class);
		final AccelerationConfigGroup accelerationConfigGroup = ConfigUtils.addOrGetModule(config,
				AccelerationConfigGroup.class);

		final RunPSim_NEW pSim = new RunPSim_NEW(config, pSimConfigGroup, accelerationConfigGroup);
		pSim.run();
	}
}
