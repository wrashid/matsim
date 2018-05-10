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

import java.util.*;

import javax.inject.Singleton;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.pseudosimulation.mobsim.PSim;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.CountIndicatorUtils;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.ScoreUpdater;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.SpaceTimeIndicators;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.scenario.ScenarioUtils;

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
	private ReplanningContext replanningContext;

	@Inject
	private TimeDiscretization timeDiscr;

	@Inject
	private ReplanningParameterContainer replanningParameters;

	@Inject
	private LinkWeightContainer linkWeights;

	// -------------------- NON-INJECTED MEMBERS --------------------

	private final String prefix = "[Acceleration] ";
	private final Logger log = Logger.getLogger(Controler.class);


	private Set<Id<Person>> replanners = null;

	// Delegate for physical mobsim listening. Created upon startup.
	private LinkUsageListener physicalMobsimUsageListener = null;

	private PopulationState hypotheticalPopulationState = null;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	public SearchAccelerator() {
		log.setLevel(Level.INFO);
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

		this.log.info(this.prefix + " iteration " + event.getIteration() + " ends");

		/*
		 * OBTAIN DATA FROM MOST RECENT PHYSICAL NETWORK LOADING
		 *
		 * Receive information about what happened in the network during the
		 * most recent real network loading.
		 */
		final Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> driverId2physicalLinkUsage = this.physicalMobsimUsageListener
				.getAndClearIndicators();
		this.log.info(this.prefix + "observed " + driverId2physicalLinkUsage.size() + " drivers in physical mobsim");

		// final Map<Id<Vehicle>, Id<Person>> vehicleId2personIdPhysical =
		// this.physicalMobsimUsageListener
		// .getAndClearDrivers();
		// this.log.info(this.prefix + "identified " +
		// vehicleId2personIdPhysical.size() + " drivers in physical mobsim");

		/*
		 * HYPOTHETICAL REPLANNING
		 *
		 * Let every agent re-plan once. Memorize the new plans. Make sure that
		 * the the actual choice sets and currently chosen plans are NOT
		 * affected by this by immediately re-setting to the original population
		 * state once the re-planning has been implemented.
		 */

		this.log.info(this.prefix + "hyothetical replanning...");
		final PopulationState originalPopulationState = new PopulationState(
				this.services.getScenario().getPopulation());
		this.setWeightOfHypotheticalReplanning(0.0);
		this.services.getStrategyManager().run(this.services.getScenario().getPopulation(), this.replanningContext);
		this.setWeightOfHypotheticalReplanning(1e9);
		this.hypotheticalPopulationState = new PopulationState(this.services.getScenario().getPopulation());
		originalPopulationState.set(this.services.getScenario().getPopulation());

		/*
		 * PSEUDOSIM
		 *
		 * Execute all new plans in the pSim.
		 */

		this.log.info(prefix + "psim");

		final PlanCatcher planCatcher = new PlanCatcher(); // replace this by
		// just filling a
		// set?
		for (Id<Person> personId : this.services.getScenario().getPopulation().getPersons().keySet()) {
			planCatcher.addPlansForPsim(this.hypotheticalPopulationState.getSelectedPlan(personId));
		}
		this.log.info(prefix + "plans in planCatcher = " + planCatcher.getPlansForPSim().size());

		final LinkUsageListener pSimLinkUsageListener = new LinkUsageListener(this.timeDiscr);
		final EventsManager eventsManager = EventsUtils.createEventsManager();
		eventsManager.addHandler(pSimLinkUsageListener);

		final PSim pSim = new PSim(this.services.getScenario(), eventsManager, planCatcher.getPlansForPSim(),
				this.services.getLinkTravelTimes());
		pSim.run();
		final Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> driverId2pSimLinkUsage = pSimLinkUsageListener
				.getAndClearIndicators();
		this.log.info(prefix + "identified " + driverId2pSimLinkUsage.size() + " drivers in pSim");
		// final Map<Id<Vehicle>, Id<Person>> vehicleId2personIdpSim =
		// pSimLinkUsageListener.getAndClearDrivers();
		// this.log.info(this.prefix + "identified " +
		// vehicleId2personIdpSim.size() + " drivers in pSim");

		// Check that drivers in physical simulation and pSim were the same.
		if ((!driverId2physicalLinkUsage.keySet().containsAll(driverId2pSimLinkUsage.keySet()))
				|| (driverId2physicalLinkUsage.keySet().size() != driverId2pSimLinkUsage.keySet().size())) {
			throw new RuntimeException("Different drivers in physical simulation and in pSim.");
		}

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
				this.physicalMobsimUsageListener.getTimeDiscretization(), driverId2physicalLinkUsage.values(),
				this.linkWeights);
		final DynamicData<Id<Link>> upcomingWeightedCounts = CountIndicatorUtils.newWeightedCounts(
				pSimLinkUsageListener.getTimeDiscretization(), driverId2pSimLinkUsage.values(), this.linkWeights);

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

		this.log.info(this.prefix + "identify replanners");

		this.replanners = new LinkedHashSet<>();
		double totalScoreChange = 0.0;

		final List<Id<Person>> allPersonIdsShuffled = new ArrayList<>(driverId2physicalLinkUsage.keySet());
		Collections.shuffle(allPersonIdsShuffled);

		for (Id<Person> driverId : allPersonIdsShuffled) {

			this.log.debug(this.prefix + "  driver " + driverId);

			final ScoreUpdater<Id<Link>> scoreUpdater = new ScoreUpdater<>(driverId2physicalLinkUsage.get(driverId),
					driverId2pSimLinkUsage.get(driverId), meanLambda, currentWeightedCounts,
					sumOfCurrentWeightedCounts2, w, delta, interactionResiduals, inertiaResiduals,
					regularizationResidual, this.linkWeights);

			this.log.debug(
					this.prefix + "  scoreChange if one  = " + (totalScoreChange + scoreUpdater.getScoreChangeIfOne()));
			this.log.debug(this.prefix + "  scoreChange if zero = "
					+ (totalScoreChange + scoreUpdater.getScoreChangeIfZero()));

			final double newLambda;
			if (scoreUpdater.getScoreChangeIfOne() < scoreUpdater.getScoreChangeIfZero()) {
				newLambda = 1.0;
				this.replanners.add(driverId);
				this.log.debug(this.prefix + "  => is a replanner");
				totalScoreChange += scoreUpdater.getScoreChangeIfOne();
			} else {
				newLambda = 0.0;
				this.log.debug(this.prefix + "  => is NOT a replanner");
				totalScoreChange += scoreUpdater.getScoreChangeIfZero();
			}
			this.log.debug(this.prefix + "  totalScoreChange = " + totalScoreChange);

			scoreUpdater.updateResiduals(newLambda);
			// Interaction- and inertiaResiduals are updated by reference.
			regularizationResidual = scoreUpdater.getUpdatedRegularizationResidual();
		}

		this.log.info(this.prefix + "Effective replanning rate = " + (int) (((double) 100 * this.replanners.size())
				/ this.services.getScenario().getPopulation().getPersons().size()) + " percent");
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
			if (strategy instanceof AcceptIntendedReplanningStrategy) {
				strategyManager.changeWeightOfStrategy(strategy, null, weight);
			}
		}
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		/*
		 * TODO (general):
		 *
		 * This interacts with the core re-planning, which appears to be
		 * parallel code. Where does this need to be accounted for?
		 *
		 * Instances of which strategy-related classes are persistent from one
		 * iteration to the next, and which are created anew in each iteration?
		 * Specifically, how to identify a concrete strategy at runtime, by
		 * reference or by iterating through all available strategies and
		 * checking "instanceof" or ... ?
		 *
		 * SearchAccelerator operates on "Person", whereas general re-planning
		 * operates on "HasPlansAndId<Plan, Person>". Switch to the latter
		 * throughout.
		 *
		 * Allow for parametrization through Config.
		 */

		System.out.println("STARTED..");

		/*
		 * Load a configuration. Strategies like "keep last selected plan" must
		 * receive a zero weight because they will be dynamically assigned by
		 * the search acceleration. Could do this dynamically by scanning the
		 * Config.
		 *
		 * Configuration for one further strategy that only clones previously
		 * made hypothetical re-planning decisions (evaluated within the search
		 * acceleration framework) is added. To switch it off/almost certainly
		 * on, its weight is set to either zero or a very large number within
		 * the accelerated simulation process.
		 *
		 * TODO It seems as if the pSim does not interpret the simulation end
		 * time "00:00:00" (which appears to indicate "run until everyone is
		 * done") right.
		 */

		// final Config config =
		// ConfigUtils.loadConfig("C:/Nobackup/Profilen/git-2018/vsp-playgrounds/gunnar/"
		// + "testdata/berlin_2014-08-01_car_1pct/config.xml");
		Config config = ConfigUtils
				.loadConfig("examples/scenarios/berlin/config.xml");
//				.loadConfig("C:/Nobackup/Profilen/git-2018/matsim-code-examples/scenarios/equil/config.xml");

		config.controler().setLastIteration(100);


		AcceptIntendedReplanningStrategy.addStrategySettings(config);
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.qsim().setEndTime(24 * 3600);

		/*
		 * Create scenario and controller as usually.
		 */

		final Scenario scenario = ScenarioUtils.loadScenario(config);
		final Controler controler = new Controler(scenario);

		//yoyoyo this to ensure that a scenario  starts naive and transit-free
		{
			config.transit().setUseTransit(false);

			for (Person person : scenario.getPopulation().getPersons().values()) {
				for (Plan plan : person.getPlans()) {
					Iterator<PlanElement> iterator = plan.getPlanElements().iterator();
					while (iterator.hasNext()) {
						PlanElement planElement = iterator.next();
						if (planElement instanceof Leg) {
							Leg leg = (Leg) planElement;
							leg.setRoute(null);
							leg.setMode(TransportMode.car);
						}
					}
				}

			}
		}
		/*
		 * Install the search acceleration logic.
		 *
		 * TimeDiscretization: Could either come from a new config module or be
		 * derived from other MATSim parameters .
		 *
		 * ReplanningParameterContainer: Should come from a new config module;
		 * could come in parts from a (somewhat) involved analysis of otherwise
		 * present strategy modules.
		 *
		 * LinkWeightContainer: There probably is one superior setting that
		 * needs to be experimentally identified (current guess is that
		 * "one over capacity" will work best).
		 */

		final TimeDiscretization timeDiscr = new TimeDiscretization(0, 3600, 24);
		final ReplanningParameterContainer replanningParameterProvider = new ConstantReplanningParameters(0.2, 1.0);
		final LinkWeightContainer linkWeightProvider = LinkWeightContainer
				.newOneOverCapacityLinkWeights(scenario.getNetwork());

		controler.addOverridingModule(
				new SearchAcceleratorModule(timeDiscr, replanningParameterProvider, linkWeightProvider));

		controler.run();

		System.out.println(".. DONE.");
	}
}
