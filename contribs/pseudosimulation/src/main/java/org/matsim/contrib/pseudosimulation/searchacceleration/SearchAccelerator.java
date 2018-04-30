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
package org.matsim.contrib.pseudosimulation.searchacceleration;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.mobsim.PSim;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.CountIndicatorUtils;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.ScoreUpdater;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.SpaceTimeIndicators;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
@Singleton
public class SearchAccelerator
		implements StartupListener, IterationEndsListener, LinkEnterEventHandler, VehicleEntersTrafficEventHandler {

	// -------------------- INJECTED MEMBERS --------------------

	@Inject
	private MatsimServices services;

	@Inject
	private Population population;

	@Inject
	private ReplanningContext replanningContext;

	@Inject
	private Scenario scenario;

	@Inject
	private Config config;

	@Inject
	private Network network;

	@Inject
	private TimeDiscretization timeDiscr;

	@Inject
	private ReplanningParameterContainer replanningParameters;

	@Inject
	private LinkWeightContainer linkWeights;

	// -------------------- NON-INJECTED MEMBERS --------------------

	private Set<Id<Person>> replanners;

	private boolean firstCall = true;

	// Delegating physical mobsim listening to this one. Created upon startup.
	private LinkUsageListener physicalMobsimUsageListener = null;

	private PopulationState hypotheticalPopulationState = null;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	public SearchAccelerator() {
	}

	// -------------------- IMPLEMENTATION OF EventHandlers --------------------

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		this.physicalMobsimUsageListener.handleEvent(event);
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		this.physicalMobsimUsageListener.handleEvent(event);
	}

	// --------------- IMPLEMENTATION OF StartupListener ---------------

	@Override
	public void notifyStartup(final StartupEvent event) {
		this.physicalMobsimUsageListener = new LinkUsageListener(this.timeDiscr);
	}

	// --------------- IMPLEMENTATION OF IterationEndsListener ---------------

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {

		/*
		 * OBTAIN DATA FROM MOST RECENT PHYSICAL NETWORK LOADING
		 * 
		 * Receive information about what happened in the network during the
		 * most recent real network loading.
		 */
		final Map<Id<Vehicle>, SpaceTimeIndicators<Id<Link>>> vehicleId2physicalLinkUsage = this.physicalMobsimUsageListener
				.getAndClearIndicators();
		final Map<Id<Vehicle>, Id<Person>> vehicleId2personId = this.physicalMobsimUsageListener.getAndClearDrivers();

		/*
		 * HYPOTHETICAL REPLANNING
		 * 
		 * Let every agent re-plan once. Memorize the new plans. Make sure that
		 * the the actual choice sets and currently chosen plans are NOT
		 * affected by this by immediately re-setting to the original population
		 * state once the re-planning has been implemented.
		 */

		final PopulationState originalPopulationState = new PopulationState(this.population);
		this.setWeightOfHypotheticalReplanning(0.0);
		this.services.getStrategyManager().run(this.population, this.replanningContext);
		this.setWeightOfHypotheticalReplanning(1e9);
		this.hypotheticalPopulationState = new PopulationState(this.population);
		originalPopulationState.set(this.population);

		/*
		 * PSEUDOSIM
		 * 
		 * Execute all new plans in the pSim.
		 */

		final PlanCatcher planCatcher = new PlanCatcher();
		for (Id<Person> personId : this.population.getPersons().keySet()) {
			planCatcher.addPlansForPsim(this.hypotheticalPopulationState.getSelectedPlan(personId));
		}
		final LinkUsageListener pSimLinkUsageListener = new LinkUsageListener(this.timeDiscr);
		final EventsManager eventsManager = new EventsManagerImpl();
		eventsManager.addHandler(pSimLinkUsageListener);
		final PSim pSim = new PSim(this.scenario, eventsManager, planCatcher.getPlansForPSim(),
				this.services.getLinkTravelTimes());
		pSim.run();
		final Map<Id<Vehicle>, SpaceTimeIndicators<Id<Link>>> vehicleId2pSimLinkUsage = pSimLinkUsageListener
				.getAndClearIndicators();

		/*
		 * DECIDE WHO GETS TO RE-PLAN.
		 * 
		 * At this point, one has (i) the link usage statistics from the
		 * previous MATSim network loading (vehId2physLinkUsage), and (ii) the
		 * hypothetical link usage statistics that would result from a 100%
		 * re-planning rate if network congestion did not change
		 * (vehId2pSimLinkUsage).
		 * 
		 * Now solve an optimization problem that aims at balancing simulation
		 * advancement (changing link usage patterns) and simulation
		 * stabilization (keeping link usage patterns as they are). Most of the
		 * code below prepares the (heuristic) solution of this problem.
		 * 
		 */

		// Extract basic statistics.

		final double meanLambda = this.replanningParameters.getMeanLambda(event.getIteration());
		final double delta = this.replanningParameters.getDelta(event.getIteration());

		final DynamicData<Id<Link>> currentWeightedCounts = CountIndicatorUtils.newWeightedCounts(
				this.physicalMobsimUsageListener.getTimeDiscretization(), vehicleId2physicalLinkUsage.values(),
				this.linkWeights);
		final DynamicData<Id<Link>> upcomingWeightedCounts = CountIndicatorUtils.newWeightedCounts(
				pSimLinkUsageListener.getTimeDiscretization(), vehicleId2pSimLinkUsage.values(), this.linkWeights);

		final double sumOfCurrentWeightedCounts2 = CountIndicatorUtils.sumOfEntries2(currentWeightedCounts);
		if (sumOfCurrentWeightedCounts2 < 1e-6) {
			throw new RuntimeException("There is no traffic on the network.");
		}
		final double sumOfWeightedCountDifferences2 = CountIndicatorUtils.sumOfDifferences2(currentWeightedCounts,
				upcomingWeightedCounts);
		final double w = meanLambda / (1.0 - meanLambda) * (sumOfWeightedCountDifferences2 + delta)
				/ sumOfCurrentWeightedCounts2;

		// Initialize score residuals.

		final DynamicData<Id<Link>> interactionResiduals = CountIndicatorUtils
				.newInteractionResiduals(currentWeightedCounts, upcomingWeightedCounts, meanLambda);
		final DynamicData<Id<Link>> inertiaResiduals = new DynamicData<>(currentWeightedCounts.getStartTime_s(),
				currentWeightedCounts.getBinSize_s(), currentWeightedCounts.getBinCnt());
		for (Id<Link> locObj : currentWeightedCounts.keySet()) {
			for (int bin = 0; bin < currentWeightedCounts.getBinCnt(); bin++) {
				inertiaResiduals.put(locObj, bin, (1.0 - meanLambda) * currentWeightedCounts.getBinValue(locObj, bin));
			}
		}
		double regularizationResidual = meanLambda * sumOfCurrentWeightedCounts2;

		// Go through all vehicles and decide which driver gets to re-plan.

		final Set<Id<Vehicle>> allVehicleIds = new LinkedHashSet<>(vehicleId2physicalLinkUsage.keySet());
		allVehicleIds.addAll(vehicleId2pSimLinkUsage.keySet());
		this.replanners = new LinkedHashSet<>();

		for (Id<Vehicle> vehId : allVehicleIds) {

			final Id<Person> driverId = vehicleId2personId.get(vehId);
			if (driverId == null) {
				throw new RuntimeException("Vehicle " + vehId + " has no (null) driver!");
			}

			final ScoreUpdater<Id<Link>> scoreUpdater = new ScoreUpdater<>(vehicleId2physicalLinkUsage.get(vehId),
					vehicleId2pSimLinkUsage.get(vehId), meanLambda, currentWeightedCounts, sumOfCurrentWeightedCounts2,
					w, delta, interactionResiduals, inertiaResiduals, regularizationResidual, this.linkWeights);

			final double newLambda;
			if (scoreUpdater.getScoreChangeIfOne() < scoreUpdater.getScoreChangeIfZero()) {
				newLambda = 1.0;
				this.replanners.add(driverId);
			} else {
				newLambda = 0.0;
			}

			scoreUpdater.updateResiduals(newLambda);
			// Interaction- and inertiaResiduals are updated by reference.
			regularizationResidual = scoreUpdater.getUpdatedRegularizationResidual();
		}
	}

	// -------------------- REPLANNING FUNCTIONALITY --------------------

	public void replan(final Person person) {
		if (this.replanners.contains(person.getId())) {
			this.hypotheticalPopulationState.set(person);
		}
	}

	// -------------------- HELPERS --------------------

	public static void log(String line, boolean terminate) {
		System.out.println(line);
		if (terminate) {
			System.exit(0);
		}
	}

	private void setWeightOfHypotheticalReplanning(final double weight) {
		final StrategyManager strategyManager = this.services.getStrategyManager();
		for (GenericPlanStrategy<Plan, Person> strategy : strategyManager.getStrategies(null)) {
			if (strategy instanceof CloneHypotheticalReplanningStrategy) {
				strategyManager.changeWeightOfStrategy(strategy, null, weight);
			}
		}		
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {
		System.out.println("STARTED..");

		Config config = ConfigUtils
				.loadConfig("C:/Nobackup/Profilen/git-2018/matsim-code-examples/scenarios/equil/config.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		StrategySettings stratSets = new StrategySettings();
		stratSets.setStrategyName(CloneHypotheticalReplanningStrategy.NAME);
		stratSets.setWeight(1e3);
		config.strategy().addStrategySettings(stratSets);

		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);

		final TimeDiscretization timeDiscr = new TimeDiscretization(0, 3600, 24);
		final ReplanningParameterContainer replanningParameterProvider = new ConstantReplanningParameters(0.1, 1.0);
		final LinkWeightContainer linkWeightProvider = LinkWeightContainer
				.newOneOverCapacityLinkWeights(scenario.getNetwork());
		controler.addOverridingModule(
				new SearchAcceleratorModule(timeDiscr, replanningParameterProvider, linkWeightProvider));

		controler.run();

		System.out.println(".. DONE.");
	}
}
