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

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
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
import org.matsim.contrib.pseudosimulation.util.ScoreHistogramLogger;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.utils.CreatePseudoNetwork;

import com.google.inject.Singleton;

import ch.sbb.matsim.routing.pt.raptor.SwissRailRaptorModule;
import saleem.stockholmmodel.modelbuilding.PTCapacityAdjusmentPerSample;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Greedo extends AbstractModule {

	// -------------------- CONSTANTS --------------------

	private static final Logger log = Logger.getLogger(Greedo.class);

	private final int defaultIterationsPerCycle = 10;

	private final boolean defaultFullTransitPerformanceTransmission = false;

	// -------------------- MEMBERS --------------------

	private final Map<String, Integer> innovationStrategy2possibleVariationCnt = new LinkedHashMap<>();

	private final Set<String> bestResponseInnovationStrategyNames = new LinkedHashSet<>();

	// private final Set<String> randomInnovationStrategyNames = new
	// LinkedHashSet<>();

	private Config config = null;

	private Scenario scenario = null;

	private Controler controler = null;

	// -------------------- CONSTRUCTION --------------------

	public Greedo() {
		this.setBestResponseStrategy("ReRoute");
		this.setRandomInnovationStrategy("TimeAllocationMutator", 20);
		this.setRandomInnovationStrategy("ChangeLegMode", 2); // changes entire plan
		this.setRandomInnovationStrategy("ChangeTripMode", 2); // changes entire plan
		this.setRandomInnovationStrategy("ChangeSingleLegMode", 2 * 4); // changes one out of ~4 legs
		this.setRandomInnovationStrategy("SubtoutModeChoice", 2 * 2); // changes one out of ~2 routes
	}

	// -------------------- IMPLEMENTATION --------------------

	public void setBestResponseStrategy(final String strategyName) {
		this.bestResponseInnovationStrategyNames.add(strategyName);
	}

	public void setRandomInnovationStrategy(final String strategyName, final int numberOfVariations) {
		this.innovationStrategy2possibleVariationCnt.put(strategyName, numberOfVariations);
	}

	// public void addRandomStrategyName(final String strategyName) {
	// this.randomInnovationStrategyNames.add(strategyName);
	// }

	public void meet(final Config config) {

		if (this.config != null) {
			throw new RuntimeException("Have already met a config.");
		}
		this.config = config;

		/*
		 * The following should not be necessary but is currently assumed when handling
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
			// Automagic -- use default values.
			pSimConf.setIterationsPerCycle(this.defaultIterationsPerCycle);
			pSimConf.setFullTransitPerformanceTransmission(this.defaultFullTransitPerformanceTransmission);
			// The following accounts for the pSim iteration overhead.
			config.controler().setLastIteration(config.controler().getLastIteration() * this.defaultIterationsPerCycle);
			config.controler().setWriteEventsInterval(
					config.controler().getWriteEventsInterval() * this.defaultIterationsPerCycle);
			config.controler()
					.setWritePlansInterval(config.controler().getWritePlansInterval() * this.defaultIterationsPerCycle);
			config.controler().setWriteSnapshotsInterval(
					config.controler().getWriteSnapshotsInterval() * this.defaultIterationsPerCycle);
		}

		/*
		 * Ensure a valid Acceleration configuration; fall back to default values if not
		 * available.
		 */
		ConfigUtils.addOrGetModule(config, AccelerationConfigGroup.class);

		/*
		 * Use minimal choice set and always remove the worse plan. This probably as
		 * close as it can get to best-response in the presence of random innovation
		 * strategies.
		 */
		config.strategy().setMaxAgentPlanMemorySize(1);
		config.strategy().setPlanSelectorForRemoval("WorstPlanSelector");

		/*
		 * Keep only plan innovation strategies. Re-weight for maximum pSim efficiency.
		 * 
		 */
		double bestResponseStrategyWeightSum = 0.0;
		double randomStrategyWeightSum = 0.0;
		// for (StrategySettings strategySettings :
		// config.strategy().getStrategySettings()) {
		// strategySettings.setWeight(0);
		// }
		for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
			final String strategyName = strategySettings.getStrategyName();
			if (this.bestResponseInnovationStrategyNames.contains(strategyName)) {
				strategySettings.setWeight(1.0 / pSimConf.getIterationsPerCycle());
				bestResponseStrategyWeightSum += strategySettings.getWeight();
			} else if (this.innovationStrategy2possibleVariationCnt.containsKey(strategyName)) {
				randomStrategyWeightSum += this.innovationStrategy2possibleVariationCnt.get(strategyName);
			} else {
				strategySettings.setWeight(0.0); // i.e., dismiss
			}
		}
		final double randomStrategyFactor = (1.0 - bestResponseStrategyWeightSum) / randomStrategyWeightSum;
		for (StrategySettings strategySettings : config.strategy().getStrategySettings()) {
			final String strategyName = strategySettings.getStrategyName();
			if (this.innovationStrategy2possibleVariationCnt.containsKey(strategyName)) {
				strategySettings.setWeight(
						randomStrategyFactor * this.innovationStrategy2possibleVariationCnt.get(strategyName));
			}
		}

		/*
		 * Add a strategy that decides which of the better-response re-planning
		 * decisions coming out of the pSim is allowed to be implemented.
		 */
		final StrategySettings acceptIntendedReplanningStrategySettings = new StrategySettings();
		acceptIntendedReplanningStrategySettings.setStrategyName(AcceptIntendedReplanningStrategy.STRATEGY_NAME);
		acceptIntendedReplanningStrategySettings.setWeight(0.0); // changed dynamically
		config.strategy().addStrategySettings(acceptIntendedReplanningStrategySettings);
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

		System.out.println("Installing the pSim");
		this.addControlerListenerBinding().toInstance(mobSimSwitcher);
		this.bind(MobSimSwitcher.class).toInstance(mobSimSwitcher);
		this.bindMobsim().toProvider(SwitchingMobsimProvider.class);
		this.bind(WaitTimeCalculator.class).to(PSimWaitTimeCalculator.class);
		this.bind(WaitTime.class).toProvider(PSimWaitTimeCalculator.class);

		this.bind(StopStopTimeCalculator.class).to(PSimStopStopTimeCalculator.class);
		this.bind(StopStopTime.class).toProvider(PSimStopStopTimeCalculator.class);
		this.bind(TravelTimeCalculator.class).to(PSimTravelTimeCalculator.class);
		this.bind(TravelTime.class).toProvider(PSimTravelTimeCalculator.class);
		this.bind(PlanCatcher.class).toInstance(new PlanCatcher());
		this.bind(PSimProvider.class).toInstance(pSimProvider);

		System.out.println("Installing the acceleration");
		this.bind(SearchAccelerator.class).in(Singleton.class);
		this.addControlerListenerBinding().to(SearchAccelerator.class);
		this.addEventHandlerBinding().to(SearchAccelerator.class);
		this.addPlanStrategyBinding(AcceptIntendedReplanningStrategy.STRATEGY_NAME)
				.toProvider(AcceptIntendedReplanningStragetyProvider.class);
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void runCarOnly() {

		/*
		 * TODO The "auto-magic" needs refinement for sub-populations.
		 */

		/*
		 * Create the Greedo. Indicate all relevant plan innovation strategies. TODO Are
		 * all standard strategies pre-configured?
		 */

		Greedo greedo = new Greedo();

		/*
		 * Create Config, Scenario, Controler in the usual order. Let the Greedo meet
		 * each one of them before moving on to the next. Finally, run the (now greedy)
		 * simulation.
		 */

		Config config = ConfigUtils.loadConfig(
				"/Users/GunnarF/NoBackup/data-workspace/searchacceleration//rerun-2015-11-23a_No_Toll_large/matsim-config.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.qsim().setEndTime(48 * 3600); // FIXME QSim seems to interpret zero end time wrong.
		if (greedo != null) {
			greedo.meet(config);
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);
		if (greedo != null) {
			greedo.meet(scenario);
		}

		Controler controler = new Controler(scenario);
		if (greedo != null) {
			greedo.meet(controler);
		}

		controler.run();
	}

	public static void runPT() {
		System.out.println("STARTED ...");

		Greedo greedo = new Greedo();

		Config config = ConfigUtils
				.loadConfig("/Users/GunnarF/NoBackup/data-workspace/pt/production-scenario/config.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		// config.qsim().setEndTime(30 * 3600); // FIXME QSim misinterprets default
		// values.
		if (greedo != null) {
			greedo.meet(config);
			System.out.println("Greedo has met the config.");
		}

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Network network = scenario.getNetwork();
		TransitSchedule schedule = scenario.getTransitSchedule();
		new CreatePseudoNetwork(schedule, network, "tr_").createNetwork();
		for (Link link : network.getLinks().values()) {
			if (link.getId().toString().startsWith("tr_")) {
				System.out.println("updating link " + link.getId());
				link.setCapacity(1e6);
				link.setNumberOfLanes(1e3);
			}
		}

		if (greedo != null) {
			greedo.meet(scenario);
			System.out.println("Greedo has met the scenario.");
		}

		Controler controler = new Controler(scenario);
		if (greedo != null) {
			greedo.meet(controler);
			System.out.println("Greedo has met the controler.");
		}

		controler.addOverridingModule(new ScoreHistogramLogger(scenario.getPopulation(), config));

		double samplesize = config.qsim().getStorageCapFactor();
		PTCapacityAdjusmentPerSample capadjuster = new PTCapacityAdjusmentPerSample();
		capadjuster.adjustStoarageAndFlowCapacity(scenario, samplesize);

		// According to Basil, better add Raptor after the pSim.
		System.out.println("Adding the Raptor...");
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				System.out.println("Installing the Raptor");
				install(new SwissRailRaptorModule());
			}
		});

		// TODO the event based pt router package misinterprets default values
		// System.out.println("bin size = " +
		// config.travelTimeCalculator().getTraveltimeBinSize());
		// System.out.println("qSim start = " + config.qsim().getStartTime());
		// System.out.println("qSim end = " + config.qsim().getEndTime());

		System.out.println("Starting the controler...");
		controler.run();

		System.out.println("... DONE");
	}

	public static void main(String[] args) {
		// runCarOnly();
		runPT();
	}
}
