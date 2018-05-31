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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Singleton;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.pseudosimulation.MobSimSwitcher;
import org.matsim.contrib.pseudosimulation.PSimConfigGroup;
import org.matsim.contrib.pseudosimulation.mobsim.PSim;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.SpaceTimeIndicators;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.AnticipatedCongestedLinkShare;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.DriversInPhysicalSim;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.DriversInPseudoSim;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.EffectiveReplanningRate;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.ExpectedUniformSamplingObjectiveFunctionValue;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.ExperiencedCongestedLinkShare;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.FinalObjectiveFunctionValue;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.HypotheticalCongestedLinkShare;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.RepeatedReplanningProba;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.ReplanningBootstrap;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.ShareNeverReplanned;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.ShareScoreImprovingReplanners;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.UniformReplanningObjectiveFunctionValue;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.UniformityExcess;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.UnweightedCountDifferences2;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.WeightedCountDifferences2;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.StrategyManager;

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
	private TimeDiscretization timeDiscr;

	@Inject
	private ReplanningParameterContainer replanningParameters;

	/*
	 * We know if we are in a pSim iteration or in a "real" iteration. The
	 * MobsimSwitcher is updated at iterationStarts, i.e. always *before* the mobsim
	 * (or psim) is executed. The SearchAccelerator, on the other hand, is invoked
	 * at iterationEnds, i.e. *after* the corresponding mobsim (or psim) run.
	 * 
	 */
	@Inject
	private MobSimSwitcher mobsimSwitcher;

	// -------------------- NON-INJECTED MEMBERS --------------------

	private final int bootstrapReplications = 0;

	private void log(final Object msg) {
		Logger.getLogger(Controler.class).info("[Acceleration] " + msg);
	}

	private Set<Id<Person>> replanners = null;

	// Delegate for mobsim listening. Created upon startup.
	private LinkUsageListener matsimMobsimUsageListener = null;

	// Created upon startup
	private StatisticsWriter<AccelerationAnalyzer> statsWriter = null;

	// Created upon startup
	private AccelerationAnalyzer analyzer = null;

	private PopulationState hypotheticalPopulationState = null;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	public SearchAccelerator() {
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

	// --------------- IMPLEMENTATION OF StartupListener ---------------

	@Override
	public void notifyStartup(final StartupEvent event) {

		this.matsimMobsimUsageListener = new LinkUsageListener(this.timeDiscr);

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
		this.statsWriter.addSearchStatistic(new ExpectedUniformSamplingObjectiveFunctionValue());
		this.statsWriter.addSearchStatistic(new UniformReplanningObjectiveFunctionValue());
		this.statsWriter.addSearchStatistic(new FinalObjectiveFunctionValue());
		this.statsWriter.addSearchStatistic(new ShareScoreImprovingReplanners());
		this.statsWriter.addSearchStatistic(new UniformityExcess());
		this.statsWriter.addSearchStatistic(new UnweightedCountDifferences2());
		this.statsWriter.addSearchStatistic(new WeightedCountDifferences2());
		this.statsWriter.addSearchStatistic(new ReplanningBootstrap());
	}

	// -------------------- IMPLEMENTATION OF EventHandlers --------------------

	@Override
	public void reset(final int iteration) {
		this.log("reset of event handling in iteration " + iteration);
		this.matsimMobsimUsageListener.reset(iteration);
	}

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		this.matsimMobsimUsageListener.handleEvent(event);
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		this.matsimMobsimUsageListener.handleEvent(event);
	}

	// --------------- IMPLEMENTATION OF IterationEndsListener ---------------

	private PopulationState lastPhysicalPopulationState = null;
	private Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> lastPhysicalLinkUsages = null;

	private int pseudoSimIterationCnt = 0;

	// for testing
	private boolean nextMobsimIsExpectedToBePhysical = true;

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {

//		for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
//			if (person.getPlans().size() > 1) {
//				throw new RuntimeException("person " + person.getId() + " has " + person.getPlans().size() + " plans");
//			}
//		}

		if (this.mobsimSwitcher.isQSimIteration()) {
			this.log("physical mobsim run in iteration " + event.getIteration() + " ends");
			if (!this.nextMobsimIsExpectedToBePhysical) {
				throw new RuntimeException("Did not expect a physical mobsim run!");
			}
			this.lastPhysicalPopulationState = new PopulationState(this.services.getScenario().getPopulation());
			this.lastPhysicalLinkUsages = this.matsimMobsimUsageListener.getAndClearIndicators();
			this.pseudoSimIterationCnt = 0;
		} else {
			this.log("pseudoSim run in iteration " + event.getIteration() + " ends");
			if (this.nextMobsimIsExpectedToBePhysical) {
				throw new RuntimeException("Expected a physical mobsim run!");
			}
			this.pseudoSimIterationCnt++;
		}

		if (this.pseudoSimIterationCnt == (ConfigUtils.addOrGetModule(this.services.getConfig(), PSimConfigGroup.class)
				.getIterationsPerCycle() - 1)) {

			/*
			 * Extract, for each agent, the expected (hypothetical) score change.
			 */
			double deltaScoreTotal = 0.0;
			final Map<Id<Person>, Double> personId2deltaScore = new LinkedHashMap<>();
			for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
				final double oldScore = this.lastPhysicalPopulationState.getSelectedPlan(person.getId()).getScore();
				final double newScore = person.getSelectedPlan().getScore();
				final double deltaScore = newScore - oldScore;
				personId2deltaScore.put(person.getId(), deltaScore);
				deltaScoreTotal += deltaScore;
			}

			/*
			 * Extract hypothetical selected plans.
			 */

			final Collection<Plan> selectedHypotheticalPlans = new ArrayList<>(
					this.services.getScenario().getPopulation().getPersons().size());
			for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
				selectedHypotheticalPlans.add(person.getSelectedPlan());
			}

			/*
			 * Execute one pSim with the full population.
			 */

			final EventsManager eventsManager = EventsUtils.createEventsManager();
			final LinkUsageListener pSimLinkUsageListener = new LinkUsageListener(this.timeDiscr);
			eventsManager.addHandler(pSimLinkUsageListener);
			final PSim pSim = new PSim(this.services.getScenario(), eventsManager, selectedHypotheticalPlans,
					this.services.getLinkTravelTimes());
			pSim.run();
			final Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> lastPseudoSimLinkUsages = pSimLinkUsageListener
					.getAndClearIndicators();

			this.log("observed " + this.lastPhysicalLinkUsages.size() + " physical drivers");
			this.log("observed " + lastPseudoSimLinkUsages.size() + " drivers in pSim");

			/*
			 * Memorize the most recent hypothetical population state re-set the population
			 * to its most recent physical state.
			 */

			this.hypotheticalPopulationState = new PopulationState(this.services.getScenario().getPopulation());
			this.lastPhysicalPopulationState.set(this.services.getScenario().getPopulation());

			/*
			 * DECIDE WHO GETS TO RE-PLAN.
			 * 
			 * At this point, one has (i) the link usage statistics from the last physical
			 * MATSim network loading (lastPhysicalLinkUsages), and (ii) the hypothetical
			 * link usage statistics that would result from a 100% re-planning rate if
			 * network congestion did not change (lastPseudoSimLinkUsages).
			 * 
			 * Now solve an optimization problem that aims at balancing simulation
			 * advancement (changing link usage patterns) and simulation stabilization
			 * (keeping link usage patterns as they are). Most of the code below prepares
			 * the (heuristic) solution of this problem.
			 * 
			 */

			final ReplannerIdentifier replannerIdentifier = new ReplannerIdentifier(this.replanningParameters,
					this.timeDiscr, event.getIteration(), this.lastPhysicalLinkUsages, lastPseudoSimLinkUsages,
					this.services.getScenario().getPopulation(), this.services.getLinkTravelTimes(), ConfigUtils
							.addOrGetModule(this.services.getConfig(), AccelerationConfigGroup.class).getAccelerate(),
					personId2deltaScore, deltaScoreTotal);
			this.replanners = replannerIdentifier.drawReplanners();

			final List<Double> bootstrap;
			if (this.bootstrapReplications > 0) {
				bootstrap = replannerIdentifier.bootstrap(this.bootstrapReplications);
			} else {
				bootstrap = null;
			}

			this.analyzer.analyze(this.services.getScenario().getPopulation().getPersons().keySet(),
					this.lastPhysicalLinkUsages, lastPseudoSimLinkUsages, this.replanners, event.getIteration(),
					bootstrap, replannerIdentifier.getUniformReplanningObjectiveFunctionValue(),
					replannerIdentifier.getShareOfScoreImprovingReplanners(),
					replannerIdentifier.getFinalObjectiveFunctionValue(), replannerIdentifier.getUniformityExcess(),
					this.services.getLinkTravelTimes(),
					replannerIdentifier.getExpectedUniformSamplingObjectiveFunctionValue(),
					replannerIdentifier.getSumOfWeightedCountDifferences2(),
					replannerIdentifier.getSumOfUnweightedCountDifferences2());

			this.statsWriter.writeToFile(this.analyzer);

			this.nextMobsimIsExpectedToBePhysical = true;
			this.setWeightOfHypotheticalReplanning(1e9);

		} else {

			this.nextMobsimIsExpectedToBePhysical = false;
			this.setWeightOfHypotheticalReplanning(0);

		}
	}

	// -------------------- REPLANNING FUNCTIONALITY --------------------

	public void replan(final HasPlansAndId<Plan, Person> person) {
		if (this.replanners.contains(person.getId())) {
			// This replaces the entire choice set and not just the selected plan. Why not.
			this.hypotheticalPopulationState.set(person);
		}
	}
}
