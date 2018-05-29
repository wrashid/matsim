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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.mobsim.PSim;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.SpaceTimeIndicators;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.AnticipatedCongestedLinkShare;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.DriversInPhysicalSim;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.DriversInPseudoSim;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.EffectiveReplanningRate;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.ExperiencedCongestedLinkShare;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.FinalObjectiveFunctionValue;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.HypotheticalCongestedLinkShare;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.RepeatedReplanningProba;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.ReplanningBootstrap;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.ShareNeverReplanned;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.ShareScoreImprovingReplanners;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.UniformReplanningObjectiveFunctionValue;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.UniformityExcess;
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
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.ScoringFunctionFactory;

import com.google.inject.Inject;

import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.statisticslogging.StatisticsWriter;
import floetteroed.utilities.statisticslogging.TimeStampStatistic;

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

	// -------------------- NON-INJECTED MEMBERS --------------------

	private final int bootstrapReplications = 0;

	private void log(final Object msg) {
		Logger.getLogger(Controler.class).info("[Acceleration] " + msg);
	}

	private Set<Id<Person>> replanners = null;

	// Delegate for physical mobsim listening. Created upon startup.
	private LinkUsageListener physicalMobsimUsageListener = null;

	// Created upon startup
	private StatisticsWriter<AccelerationAnalyzer> statsWriter = null;

	// Created upon startup
	private AccelerationAnalyzer analyzer = null;

	private PopulationState hypotheticalPopulationState = null;

	// TODO Move this into a persistent analyzer.
	// private final Map<Id<Person>, Integer> personId2lastReplanIteration = new
	// LinkedHashMap<>();

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

		this.analyzer = new AccelerationAnalyzer(this.replanningParameters, this.timeDiscr);

		this.statsWriter = new StatisticsWriter<>(
				new File(this.services.getConfig().controler().getOutputDirectory(), "acceleration.log").toString(),
				false);
		this.statsWriter.addSearchStatistic(new TimeStampStatistic<>());
		this.statsWriter.addSearchStatistic(new DriversInPhysicalSim());
		this.statsWriter.addSearchStatistic(new DriversInPseudoSim());
		this.statsWriter.addSearchStatistic(new EffectiveReplanningRate());
		this.statsWriter.addSearchStatistic(new RepeatedReplanningProba());
		this.statsWriter.addSearchStatistic(new ShareNeverReplanned());
		this.statsWriter.addSearchStatistic(new ExperiencedCongestedLinkShare());
		this.statsWriter.addSearchStatistic(new HypotheticalCongestedLinkShare());
		this.statsWriter.addSearchStatistic(new AnticipatedCongestedLinkShare());
		this.statsWriter.addSearchStatistic(new UniformReplanningObjectiveFunctionValue());
		this.statsWriter.addSearchStatistic(new FinalObjectiveFunctionValue());
		this.statsWriter.addSearchStatistic(new ShareScoreImprovingReplanners());
		this.statsWriter.addSearchStatistic(new UniformityExcess());
		this.statsWriter.addSearchStatistic(new ReplanningBootstrap());
	}

	// --------------- IMPLEMENTATION OF IterationEndsListener ---------------

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {

		this.log("iteration " + event.getIteration() + " ends");

		/*
		 * OBTAIN DATA FROM MOST RECENT PHYSICAL NETWORK LOADING
		 * 
		 * Receive information about what happened in the network during the most recent
		 * real network loading.
		 */
		final Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> driverId2physicalLinkUsage = this.physicalMobsimUsageListener
				.getAndClearIndicators();
		this.log("observed " + driverId2physicalLinkUsage.size() + " drivers in physical mobsim");

		/*
		 * HYPOTHETICAL REPLANNING AND PSEUDOSIM
		 * 
		 * Let every agent re-plan once. Execute all new plans in the pseudoSim.
		 * Memorize the new plans. Make sure that the the actual choice sets and
		 * currently chosen plans are NOT affected by this by immediately re-setting to
		 * the original population state once the re-planning has been implemented.
		 */
		this.log("hypothetical replanning...");

		// Memorize the original state.

		final PopulationState originalPopulationState = new PopulationState(
				this.services.getScenario().getPopulation());

		// Re-plan. The population enters now the hypothetical state.

		this.setWeightOfHypotheticalReplanning(0.0);

		Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> driverId2pSimLinkUsage = null;
		final int pSimItCnt = ConfigUtils.addOrGetModule(this.services.getConfig(), AccelerationConfigGroup.class)
				.getPseudoSimIterations();

		for (int pSimIt = 0; pSimIt < pSimItCnt; pSimIt++) {

			this.log("pSim iteration " + (pSimIt + 1) + " / " + pSimItCnt);

			// re-plan and extract selected plan

			this.services.getStrategyManager().run(this.services.getScenario().getPopulation(), this.replanningContext);
			final Collection<Plan> selectedHypotheticalPlans = new ArrayList<>(
					this.services.getScenario().getPopulation().getPersons().size());
			for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
				selectedHypotheticalPlans.add(person.getSelectedPlan());
			}

			// execute in pseudoSim (TODO don't need to track indicators each time)

			final EventsManager eventsManager = EventsUtils.createEventsManager();
			final LinkUsageListener pSimLinkUsageListener = new LinkUsageListener(this.timeDiscr);
			eventsManager.addHandler(pSimLinkUsageListener);
			final PSim pSim = new PSim(this.services.getScenario(), eventsManager, selectedHypotheticalPlans,
					this.services.getLinkTravelTimes());
			pSim.run();
			driverId2pSimLinkUsage = pSimLinkUsageListener.getAndClearIndicators();
			this.log("identified " + driverId2pSimLinkUsage.size() + " drivers in pSim");
		}

		/*
		 * FIXME There is no scoring here. Will most likely need to submit all of this to the
		 * MATSim loop (like in the pSim), meaning that much of the functionality of this handler
		 * is only used at the end of a (set of) pSim iterations.
		 */

		this.hypotheticalPopulationState = new PopulationState(this.services.getScenario().getPopulation());
		this.setWeightOfHypotheticalReplanning(1e9);

		// Return (for now) to original population state.

		originalPopulationState.set(this.services.getScenario().getPopulation());

		/*
		 * DECIDE WHO GETS TO RE-PLAN.
		 * 
		 * At this point, one has (i) the link usage statistics from the previous MATSim
		 * network loading (vehId2physLinkUsage), and (ii) the hypothetical link usage
		 * statistics that would result from a 100% re-planning rate if network
		 * congestion did not change (vehId2pSimLinkUsage).
		 * 
		 * Now solve an optimization problem that aims at balancing simulation
		 * advancement (changing link usage patterns) and simulation stabilization
		 * (keeping link usage patterns as they are). Most of the code below prepares
		 * the (heuristic) solution of this problem.
		 * 
		 */

		final ReplannerIdentifier replannerIdentifier = new ReplannerIdentifier(this.replanningParameters,
				this.timeDiscr, event.getIteration(), driverId2physicalLinkUsage, driverId2pSimLinkUsage,
				this.services.getScenario().getPopulation(), this.services.getLinkTravelTimes(),
				ConfigUtils.addOrGetModule(this.services.getConfig(), AccelerationConfigGroup.class).getAccelerate());
		this.replanners = replannerIdentifier.drawReplanners();

		final List<Double> bootstrap;
		if (this.bootstrapReplications > 0) {
			bootstrap = replannerIdentifier.bootstrap(this.bootstrapReplications);
		} else {
			bootstrap = null;
		}

		/*
		 * 
		 * // Extract basic statistics.
		 * 
		 * final double meanLambda =
		 * this.replanningParameters.getMeanLambda(event.getIteration()); final double
		 * delta = this.replanningParameters.getDelta(event.getIteration());
		 * 
		 * final DynamicData<Id<Link>> currentTotalCounts =
		 * CountIndicatorUtils.newUnweightedCounts(
		 * this.physicalMobsimUsageListener.getTimeDiscretization(),
		 * driverId2physicalLinkUsage.values()); final DynamicData<Id<Link>>
		 * currentWeightedCounts = CountIndicatorUtils.newWeightedCounts(
		 * this.physicalMobsimUsageListener.getTimeDiscretization(),
		 * driverId2physicalLinkUsage.values(), currentTotalCounts,
		 * this.replanningParameters); final DynamicData<Id<Link>>
		 * upcomingWeightedCounts = CountIndicatorUtils.newWeightedCounts(
		 * pSimLinkUsageListener.getTimeDiscretization(),
		 * driverId2pSimLinkUsage.values(), currentTotalCounts,
		 * this.replanningParameters);
		 * 
		 * final double sumOfCurrentWeightedCounts2 =
		 * CountIndicatorUtils.sumOfEntries2(currentWeightedCounts); if
		 * (sumOfCurrentWeightedCounts2 < 1e-6) { throw new
		 * RuntimeException("There is no (weighted) traffic on the network: " +
		 * sumOfCurrentWeightedCounts2); } final double sumOfWeightedCountDifferences2 =
		 * CountIndicatorUtils.sumOfDifferences2(currentWeightedCounts,
		 * upcomingWeightedCounts); final double w = meanLambda / (1.0 - meanLambda) *
		 * (sumOfWeightedCountDifferences2 + delta) / sumOfCurrentWeightedCounts2;
		 * 
		 * // Initialize score residuals.
		 * 
		 * final DynamicData<Id<Link>> interactionResiduals = CountIndicatorUtils
		 * .newWeightedDifference(upcomingWeightedCounts, currentWeightedCounts,
		 * meanLambda);
		 * 
		 * final DynamicData<Id<Link>> inertiaResiduals = new
		 * DynamicData<>(currentWeightedCounts.getStartTime_s(),
		 * currentWeightedCounts.getBinSize_s(), currentWeightedCounts.getBinCnt()); for
		 * (Id<Link> locObj : currentWeightedCounts.keySet()) { for (int bin = 0; bin <
		 * currentWeightedCounts.getBinCnt(); bin++) { inertiaResiduals.put(locObj, bin,
		 * (1.0 - meanLambda) * currentWeightedCounts.getBinValue(locObj, bin)); } }
		 * double regularizationResidual = meanLambda * sumOfCurrentWeightedCounts2;
		 * 
		 * // Go through all vehicles and decide which driver gets to re-plan.
		 * 
		 * this.log("identify replanners");
		 * 
		 * this.replanners = new LinkedHashSet<>(); double totalScoreChange = 0.0;
		 * 
		 * final List<Id<Person>> allPersonIdsShuffled = new
		 * ArrayList<>(driverId2physicalLinkUsage.keySet());
		 * Collections.shuffle(allPersonIdsShuffled);
		 * 
		 * for (Id<Person> driverId : allPersonIdsShuffled) {
		 * 
		 * this.log("  driver " + driverId);
		 * 
		 * final ScoreUpdater<Id<Link>> scoreUpdater = new
		 * ScoreUpdater<>(driverId2physicalLinkUsage.get(driverId),
		 * driverId2pSimLinkUsage.get(driverId), meanLambda, currentWeightedCounts,
		 * sumOfCurrentWeightedCounts2, w, delta, interactionResiduals,
		 * inertiaResiduals, regularizationResidual, this.replanningParameters,
		 * currentTotalCounts);
		 * 
		 * this.log("    scoreChange if one  = " + (totalScoreChange +
		 * scoreUpdater.getScoreChangeIfOne())); this.log("    scoreChange if zero = " +
		 * (totalScoreChange + scoreUpdater.getScoreChangeIfZero()));
		 * 
		 * final double newLambda; if (scoreUpdater.getScoreChangeIfOne() <=
		 * scoreUpdater.getScoreChangeIfZero()) { newLambda = 1.0;
		 * this.replanners.add(driverId); this.log("  => is a replanner");
		 * totalScoreChange += scoreUpdater.getScoreChangeIfOne();
		 * this.personId2lastReplanIteration.put(driverId, event.getIteration()); } else
		 * { newLambda = 0.0; this.log("  => is NOT a replanner"); totalScoreChange +=
		 * scoreUpdater.getScoreChangeIfZero(); } this.log("  totalScoreChange = " +
		 * totalScoreChange);
		 * 
		 * scoreUpdater.updateResiduals(newLambda); // Interaction- and inertiaResiduals
		 * are updated by reference. regularizationResidual =
		 * scoreUpdater.getUpdatedRegularizationResidual(); }
		 * 
		 */

		// this.log("Effective replanning rate = " + (int) (((double) 100 *
		// this.replanners.size())
		// / this.services.getScenario().getPopulation().getPersons().size()) + "
		// percent");
		//
		// final int[] referenceReplanLagHist = new int[event.getIteration() + 1];
		// for (int i = 0; i <= event.getIteration(); i++) {
		// referenceReplanLagHist[i] = (int) Math.round(Math.pow(1.0 - meanLambda, i) *
		// meanLambda
		// * this.services.getScenario().getPopulation().getPersons().size());
		// }
		//
		// final int[] actualReplanLagHist = new int[event.getIteration() + 1];
		// for (Id<Person> personId :
		// this.services.getScenario().getPopulation().getPersons().keySet()) {
		// final int val;
		// if (this.personId2lastReplanIteration.containsKey(personId)) {
		// val = this.personId2lastReplanIteration.get(personId);
		// } else {
		// val = 0;
		// }
		// actualReplanLagHist[event.getIteration() - val]++;
		// }

		// this.log("\treplanLag\treference\tactual");
		// for (int i = 0; i <= event.getIteration(); i++) {
		// this.log("\t" + i + "\t" + referenceReplanLagHist[i] + "\t" +
		// actualReplanLagHist[i]);
		// }
		//

		this.analyzer.analyze(this.services.getScenario().getPopulation().getPersons().keySet(),
				driverId2physicalLinkUsage, driverId2pSimLinkUsage, this.replanners, event.getIteration(), bootstrap,
				replannerIdentifier.getUniformReplanningObjectiveFunctionValue(),
				replannerIdentifier.getShareOfScoreImprovingReplanners(),
				replannerIdentifier.getFinalObjectiveFunctionValue(), replannerIdentifier.getUniformityExcess(),
				this.services.getLinkTravelTimes());

		// this.log("OPTIMIZED MINUS UNIFORM REALIZED DELTA N");
		// for (Double val : analyzer.diffList) {
		// this.log(val);
		// }
		// System.exit(0);

		this.statsWriter.writeToFile(this.analyzer);
	}

	// -------------------- REPLANNING FUNCTIONALITY --------------------

	public void replan(final HasPlansAndId<Plan, Person> person) {
		if (this.replanners.contains(person.getId())) {
			// TODO This replaces the entire choice set and not just the selected plan.
			this.hypotheticalPopulationState.set(person);
		}
	}

	// -------------------- HELPERS --------------------

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
		 * This interacts with the core re-planning, which appears to be parallel code.
		 * Where does this need to be accounted for?
		 * 
		 * Instances of which strategy-related classes are persistent from one iteration
		 * to the next, and which are created anew in each iteration? Specifically, how
		 * to identify a concrete strategy at runtime, by reference or by iterating
		 * through all available strategies and checking "instanceof" or ... ?
		 * 
		 */

		final boolean accelerate = true;

		System.out.println("STARTED..");

		/*
		 * Load a configuration. Strategies like "keep last selected plan" must receive
		 * a zero weight because they will be dynamically assigned by the search
		 * acceleration. Could do this dynamically by scanning the Config.
		 * 
		 * Configuration for one further strategy that only clones previously made
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
		// if (accelerate) {
		// config =
		// ConfigUtils.loadConfig("/Users/GunnarF/NoBackup/data-workspace/searchacceleration"
		// + "/berlin_2014-08-01_car_1pct/config4opt.xml");
		// } else {
		// config =
		// ConfigUtils.loadConfig("/Users/GunnarF/NoBackup/data-workspace/searchacceleration"
		// + "/berlin_2014-08-01_car_1pct/config.xml");
		// }

		// System.out.println(ConfigUtils.addOrGetModule(config,
		// AccelerationConfigGroup.class));
		// System.exit(0);

		// final String path =
		// "C:/Nobackup/Profilen/git-2018/matsim-code-examples/scenarios/equil-extended/";
		// final Config config;
		// if (accelerate) {
		// config = ConfigUtils.loadConfig(path + "config4acceleration.xml");
		// } else {
		// config = ConfigUtils.loadConfig(path + "config.xml");
		// }
		// config.controler().setLastIteration(10);

		if (accelerate) {
			AcceptIntendedReplanningStrategy.addOwnStrategySettings(config);
		}

		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.qsim().setEndTime(30 * 3600);

		/*
		 * Create scenario and controller.
		 * 
		 */

		final Scenario scenario = ScenarioUtils.loadScenario(config);

		// TODO This does not belong here: Remove all routes to start Berlin scenario
		// run from scratch.
		// for (Person person : scenario.getPopulation().getPersons().values()) {
		// for (Plan plan : person.getPlans()) {
		// for (PlanElement planElement : plan.getPlanElements()) {
		// if (planElement instanceof Leg) {
		// ((Leg) planElement).setRoute(null);
		// }
		// }
		// }
		// }

		final Controler controler = new Controler(scenario);

		// >>> TESTING
		// final Link link =
		// scenario.getNetwork().getLinks().values().iterator().next();
		// System.out.println("link " + link.getId() + " has flow capacity of "
		// + link.getFlowCapacityPerSec() * Units.VEH_H_PER_VEH_S + " veh/h.");
		// System.exit(0);
		// <<< TESTING

		/*
		 * Install the search acceleration logic.
		 * 
		 * TimeDiscretization: Could either come from a new config module or be derived
		 * from other MATSim parameters .
		 * 
		 * ReplanningParameterContainer: Should come from a new config module; could
		 * come in parts from a (somewhat) involved analysis of otherwise present
		 * strategy modules.
		 * 
		 * LinkWeightContainer: There probably is one superior setting that needs to be
		 * experimentally identified (current guess is that "one over capacity" will
		 * work best).
		 */

		final AccelerationConfigGroup accelerationConfig = ConfigUtils.addOrGetModule(config,
				AccelerationConfigGroup.class);
		if (accelerate) {
			final TimeDiscretization timeDiscr = accelerationConfig.getTimeDiscretization();
			final ReplanningParameterContainer replanningParameterProvider = new ConstantReplanningParameters(
					accelerationConfig, scenario.getNetwork());
			controler.addOverridingModule(new SearchAcceleratorModule(timeDiscr, replanningParameterProvider));
		}

		controler.run();

		System.out.println(".. DONE.");
	}
}
