/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation;

import java.util.LinkedHashSet;
import java.util.Set;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.eventsBasedPTRouter.TransitRouterEventsWSFactory;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeCalculator;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculator;
import org.matsim.contrib.pseudosimulation.mobsim.PSimProvider;
import org.matsim.contrib.pseudosimulation.mobsim.SwitchingMobsimProvider;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.contrib.pseudosimulation.searchacceleration.AccelerationConfigGroup;
import org.matsim.contrib.pseudosimulation.searchacceleration.AcceptIntendedReplanningStragetyProvider;
import org.matsim.contrib.pseudosimulation.searchacceleration.AcceptIntendedReplanningStrategy;
import org.matsim.contrib.pseudosimulation.searchacceleration.SearchAccelerator;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimStopStopTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimTravelTimeCalculator;
import org.matsim.contrib.pseudosimulation.trafficinfo.PSimWaitTimeCalculator;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.pt.router.TransitRouter;

import com.google.inject.Singleton;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Greedo extends AbstractModule {

	// -------------------- CONSTANTS --------------------

	private final int defaultIterationsPerCycle = 10;

	private final boolean defaultFullTransitPerformanceTransmission = false;

	// -------------------- MEMBERS --------------------

	private final Set<String> bestResponseStrategyNames = new LinkedHashSet<>();

	private final Set<String> randomStrategies = new LinkedHashSet<>();

	private Config config = null;

	private Scenario scenario = null;

	private Controler controler = null;

	// -------------------- CONSTRUCTION AND SETUP --------------------

	public Greedo() {
		this.addBestResponseStrategyName("ReRoute");
		this.addRandomStrategyName("TimeAllocationMutator");
	}

	public void addBestResponseStrategyName(final String strategyName) {
		this.bestResponseStrategyNames.add(strategyName);
	}

	public void addRandomStrategyName(final String strategyName) {
		this.randomStrategies.add(strategyName);
	}

	public void meet(final Config config) {

		if (this.config != null) {
			throw new RuntimeException("Have already met a config.");
		}
		this.config = config;

		/*
		 * This should not be strictly necessary but is currently assumed when computing
		 * iteration-dependent replanning rates.
		 */
		config.controler()
				.setLastIteration(config.controler().getLastIteration() - config.controler().getFirstIteration());
		config.controler().setFirstIteration(0);

		/*
		 * Use the event manager that does not check for event order. Essential for
		 * PSim, which generates events person-after-person.
		 */
		config.parallelEventHandling().setSynchronizeOnSimSteps(false);
		config.parallelEventHandling().setNumberOfThreads(1);

		/*
		 * Ensure a valid PSim configuration; fall back to default values if not
		 * available.
		 */
		final boolean pSimConfigExists = config.getModules().containsKey(PSimConfigGroup.GROUP_NAME);
		final PSimConfigGroup pSimConf = ConfigUtils.addOrGetModule(config, PSimConfigGroup.class);
		if (!pSimConfigExists) {
			pSimConf.setIterationsPerCycle(this.defaultIterationsPerCycle);
			pSimConf.setFullTransitPerformanceTransmission(this.defaultFullTransitPerformanceTransmission);
			// If one has not set the pSim, one has (probably) not accounted for its
			// iteration overhead.
			config.controler().setLastIteration(config.controler().getLastIteration() * this.defaultIterationsPerCycle);
			config.controler().setWriteEventsInterval(
					config.controler().getWriteEventsInterval() * this.defaultIterationsPerCycle);
			config.controler().setWritePlansInterval(
					config.controler().getWriteEventsInterval() * this.defaultIterationsPerCycle);
			config.controler().setWriteSnapshotsInterval(
					config.controler().getWriteSnapshotsInterval() * this.defaultIterationsPerCycle);
		}

		/*
		 * Ensure a valid Acceleration configuration; fall back to default values if not
		 * available.
		 */
		ConfigUtils.addOrGetModule(config, AccelerationConfigGroup.class);

		/*
		 * Keep minimal choice set, always remove the worse plan -> as close to
		 * best-response as one gets even with random plan strategies.
		 */
		config.strategy().setMaxAgentPlanMemorySize(1);
		config.strategy().setPlanSelectorForRemoval("WorstPlanSelector");

		/*
		 * Keep (and re-weight) only plan innovation strategies.
		 * 
		 */
		double bestResponseStrategyWeightSum = 0.0;
		double randomStrategyWeightSum = 0.0;
		for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
			if (this.bestResponseStrategyNames.contains(strategySettings.getStrategyName())) {
				strategySettings.setWeight(1.0 / pSimConf.getIterationsPerCycle());
				bestResponseStrategyWeightSum += strategySettings.getWeight();
			} else if (this.randomStrategies.contains(strategySettings.getStrategyName())) {
				randomStrategyWeightSum += strategySettings.getWeight();
			} else {
				strategySettings.setWeight(0.0);
			}
		}
		final double randomStrategyFactor = (1.0 - bestResponseStrategyWeightSum) / randomStrategyWeightSum;
		for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
			if (this.randomStrategies.contains(strategySettings.getStrategyName())) {
				strategySettings.setWeight(randomStrategyFactor * strategySettings.getWeight());
			}
		}

		/*
		 * Add a strategy that reacts to the near-optimal re-planning decisions made by
		 * the search acceleration logic.
		 */
		final StrategySettings stratSets = new StrategySettings();
		stratSets.setStrategyName(AcceptIntendedReplanningStrategy.STRATEGY_NAME);
		stratSets.setWeight(0.0); // changed dynamically
		config.strategy().addStrategySettings(stratSets);
	}

	public void meet(final Scenario scenario) {
		if (this.config == null) {
			throw new RuntimeException("First meet the config.");
		} else if (this.scenario != null) {
			throw new RuntimeException("Have already met the scenario.");
		}
		this.scenario = scenario;
		ConfigUtils.addOrGetModule(this.config, AccelerationConfigGroup.class).configure(this.scenario,
				ConfigUtils.addOrGetModule(this.config, PSimConfigGroup.class).getIterationsPerCycle());
	}

	public void meet(final Controler controler) {
		if (this.scenario == null) {
			throw new RuntimeException("First meet the scenario.");
		} else if (this.controler != null) {
			throw new RuntimeException("Have already met the controler.");
		}
		this.controler = controler;
		controler.addOverridingModule(this);
	}

	// -------------------- PREPARE THE CONFIG --------------------

	@Override
	public void install() {
		if (this.controler == null) {
			throw new RuntimeException("First meet the controler.");
		}

		final PSimConfigGroup pSimConf = ConfigUtils.addOrGetModule(this.config, PSimConfigGroup.class);
		final PSimProvider pSimProvider = new PSimProvider(this.scenario, this.controler.getEvents());
		final MobSimSwitcher mobSimSwitcher = new MobSimSwitcher(pSimConf, this.scenario);
		// >>> TODO I don't know what this is good for. Gunnar aug'18>>>
		// final TransitPerformanceRecorder transitPerformanceRecorder;
		// if (this.controler.getConfig().transit().isUseTransit()) {
		// if (pSimConf.isFullTransitPerformanceTransmission()) {
		// transitPerformanceRecorder = new TransitPerformanceRecorder(scenario,
		// this.controler.getEvents(),
		// mobSimSwitcher);
		// } else {
		// transitPerformanceRecorder = null;
		// }
		// }
		// <<< TODO <<<
		this.addControlerListenerBinding().toInstance(mobSimSwitcher);
		this.bind(MobSimSwitcher.class).toInstance(mobSimSwitcher);
		this.bindMobsim().toProvider(SwitchingMobsimProvider.class);
		this.bind(WaitTimeCalculator.class).to(PSimWaitTimeCalculator.class);
		this.bind(WaitTime.class).toProvider(PSimWaitTimeCalculator.class);
		this.bind(StopStopTimeCalculator.class).to(PSimStopStopTimeCalculator.class);
		this.bind(StopStopTime.class).toProvider(PSimStopStopTimeCalculator.class);
		this.bind(TravelTimeCalculator.class).to(PSimTravelTimeCalculator.class);
		this.bind(TravelTime.class).toProvider(PSimTravelTimeCalculator.class);
		this.bind(TransitRouter.class).toProvider(TransitRouterEventsWSFactory.class);
		this.bind(PlanCatcher.class).toInstance(new PlanCatcher());
		this.bind(PSimProvider.class).toInstance(pSimProvider);

		this.bind(SearchAccelerator.class).in(Singleton.class);
		this.addControlerListenerBinding().to(SearchAccelerator.class);
		this.addEventHandlerBinding().to(SearchAccelerator.class);
		this.addPlanStrategyBinding(AcceptIntendedReplanningStrategy.STRATEGY_NAME)
				.toProvider(AcceptIntendedReplanningStragetyProvider.class);
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		/*
		 * TODO The "auto-magic" needs refinement for PT and sub-populations (and
		 * possibly more); it has been tested with car-only and ReRoute +
		 * TimeAllocationMutator innovation.
		 */

		/*
		 * Create the Greedo. Indicate all relevant plan innovation strategies. TODO
		 * Standard strategies should eventually be pre-configured.
		 */

		Greedo greedo = new Greedo();
		greedo.addBestResponseStrategyName("ReRoute");
		greedo.addRandomStrategyName("TimeAllocationMutator");

		/*
		 * Create Config, Scenario, Controler in the usual order. Let the Greedo meet
		 * each one of them before moving on to the next. Finally, run the (now greedy)
		 * simulation.
		 */

		Config config = ConfigUtils.loadConfig(args[0]);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.qsim().setEndTime(30 * 3600); // FIXME QSim seems to interpret zero opening time wrong.
		greedo.meet(config);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		greedo.meet(scenario);

		Controler controler = new Controler(scenario);
		greedo.meet(controler);

		controler.run();
	}
}
