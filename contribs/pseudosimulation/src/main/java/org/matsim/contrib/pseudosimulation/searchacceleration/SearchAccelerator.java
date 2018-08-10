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
import java.util.LinkedHashSet;
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
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.DeltaForUniformReplanning;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.DriversInPhysicalSim;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.DriversInPseudoSim;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.EffectiveReplanningRate;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.FinalObjectiveFunctionValue;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.MeanReplanningRate;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.RegularizationWeight;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.RepeatedReplanningProba;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.ReplanningEfficiency;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.ShareNeverReplanned;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.ShareScoreImprovingReplanners;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.TTSum;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.UniformReplanningObjectiveFunctionValue;
import org.matsim.contrib.pseudosimulation.searchacceleration.logging.UniformityExcess;
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
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Inject;

import floetteroed.utilities.Tuple;
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

	// -------------------- CONSTANTS --------------------

	private static final Logger log = Logger.getLogger(Controler.class);

	// -------------------- INJECTED MEMBERS --------------------

	@Inject
	private MatsimServices services;

	/*
	 * We know if we are in a pSim iteration or in a "real" iteration. The
	 * MobsimSwitcher is updated at iterationStarts, i.e. always *before* the mobsim
	 * (or psim) is executed. The SearchAccelerator, on the other hand, is invoked
	 * at iterationEnds, i.e. *after* the corresponding mobsim (or psim) run.
	 * 
	 */
	@Inject
	private MobSimSwitcher mobsimSwitcher;

	@Inject
	private TravelTime linkTravelTimes;

	// -------------------- NON-INJECTED MEMBERS --------------------

	private Set<Id<Person>> replanners = null;

	// the following only for bookkeeping
	private final Set<Id<Person>> everReplanners = new LinkedHashSet<>();
	// private final Set<Id<Person>> lastReplanners = new LinkedHashSet<>();
	private final Map<Id<Person>, Tuple<Double, Double>> lastReplannerIds2lastAndExpectedScore = new LinkedHashMap<>();

	// Delegate for mobsim listening. Created upon startup.
	private LinkUsageListener matsimMobsimUsageListener = null;

	// Created upon startup
	private StatisticsWriter<ReplannerIdentifier> statsWriter = null;

	private PopulationState hypotheticalPopulationState = null;

	// -------------------- CONSTRUCTION --------------------

	@Inject
	public SearchAccelerator() {
	}

	// -------------------- HELPERS --------------------

	private AccelerationConfigGroup replanningParameters() {
		return ConfigUtils.addOrGetModule(this.services.getConfig(), AccelerationConfigGroup.class);
	}

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

		this.matsimMobsimUsageListener = new LinkUsageListener(this.replanningParameters().getTimeDiscretization());

		this.statsWriter = new StatisticsWriter<>(
				new File(this.services.getConfig().controler().getOutputDirectory(), "acceleration.log").toString(),
				false);
		this.statsWriter.addSearchStatistic(new TimeStampStatistic<>());
		this.statsWriter.addSearchStatistic(new DriversInPhysicalSim());
		this.statsWriter.addSearchStatistic(new DriversInPseudoSim());
		this.statsWriter.addSearchStatistic(new RegularizationWeight());
		this.statsWriter.addSearchStatistic(new MeanReplanningRate());
		this.statsWriter.addSearchStatistic(new EffectiveReplanningRate());
		this.statsWriter.addSearchStatistic(new RepeatedReplanningProba());
		this.statsWriter.addSearchStatistic(new ShareNeverReplanned());
		// this.statsWriter.addSearchStatistic(new
		// ExpectedUniformSamplingObjectiveFunctionValue());
		this.statsWriter.addSearchStatistic(new UniformReplanningObjectiveFunctionValue());
		this.statsWriter.addSearchStatistic(new FinalObjectiveFunctionValue());
		this.statsWriter.addSearchStatistic(new ShareScoreImprovingReplanners());
		this.statsWriter.addSearchStatistic(new UniformityExcess());
		this.statsWriter.addSearchStatistic(new WeightedCountDifferences2());

		this.statsWriter.addSearchStatistic(new TTSum());

		this.statsWriter.addSearchStatistic(new ReplanningEfficiency());
		
		this.statsWriter.addSearchStatistic(new DeltaForUniformReplanning(50));
		this.statsWriter.addSearchStatistic(new DeltaForUniformReplanning(90));
		this.statsWriter.addSearchStatistic(new DeltaForUniformReplanning(95));
		this.statsWriter.addSearchStatistic(new DeltaForUniformReplanning(99));
		this.statsWriter.addSearchStatistic(new DeltaForUniformReplanning(100));
	}

	// -------------------- IMPLEMENTATION OF EventHandlers --------------------

	@Override
	public void reset(final int iteration) {
		this.matsimMobsimUsageListener.reset(iteration);
	}

	@Override
	public void handleEvent(final VehicleEntersTrafficEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()) {
			this.matsimMobsimUsageListener.handleEvent(event);
		}
	}

	@Override
	public void handleEvent(final LinkEnterEvent event) {
		if (this.mobsimSwitcher.isQSimIteration()) {
			this.matsimMobsimUsageListener.handleEvent(event);
		}
	}

	// --------------- IMPLEMENTATION OF IterationEndsListener ---------------

	private PopulationState lastPhysicalPopulationState = null;
	private Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> lastPhysicalLinkUsages = null;

	private int pseudoSimIterationCnt = 0;

	// A safeguard.
	private boolean nextMobsimIsExpectedToBePhysical = true;

	@Override
	public void notifyIterationEnds(final IterationEndsEvent event) {

		if (this.mobsimSwitcher.isQSimIteration()) {
			log.info("physical mobsim run in iteration " + event.getIteration() + " ends");
			if (!this.nextMobsimIsExpectedToBePhysical) {
				throw new RuntimeException("Did not expect a physical mobsim run!");
			}
			this.lastPhysicalPopulationState = new PopulationState(this.services.getScenario().getPopulation());
			this.lastPhysicalLinkUsages = this.matsimMobsimUsageListener.getAndClearIndicators();
			this.pseudoSimIterationCnt = 0;
		} else {
			log.info("pseudoSim run in iteration " + event.getIteration() + " ends");
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
			final Map<Id<Person>, Double> personId2oldScore = new LinkedHashMap<>();
			final Map<Id<Person>, Double> personId2newScore = new LinkedHashMap<>();
			for (Person person : this.services.getScenario().getPopulation().getPersons().values()) {
				final double oldScore = this.lastPhysicalPopulationState.getSelectedPlan(person.getId()).getScore();
				personId2oldScore.put(person.getId(), oldScore);
				final double newScore = person.getSelectedPlan().getScore();
				personId2newScore.put(person.getId(), newScore);
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

			// this.services.getLinkTravelTimes(); // Just to check if this has side
			// effects.

			// TESTING

			// for (int i = 1; i <= 1000000; i++) {
			// this.services.getLinkTravelTimes();
			// System.out.println("instance " + i + ", remaining memory = " +
			// Runtime.getRuntime().freeMemory());
			// }

			// TESTING

			// if (this.linkTravelTimes == null) {
			// this.linkTravelTimes = this.services.getLinkTravelTimes();
			// }

			double ttSum_h = 0;
			for (Link link : this.services.getScenario().getNetwork().getLinks().values()) {
				for (int time_s = 0; time_s < 24 * 3600; time_s += 15 * 60) {
					ttSum_h += this.linkTravelTimes.getLinkTravelTime(link, time_s, null, null);
				}
			}
			ttSum_h /= 3600;

			final Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> lastPseudoSimLinkUsages;
			{
				final LinkUsageListener pSimLinkUsageListener = new LinkUsageListener(
						this.replanningParameters().getTimeDiscretization());
				final EventsManager eventsManager = EventsUtils.createEventsManager();
				eventsManager.addHandler(pSimLinkUsageListener);
				final PSim pSim = new PSim(this.services.getScenario(), eventsManager, selectedHypotheticalPlans,
						// this.services.getLinkTravelTimes()
						// new TravelTime() {
						// @Override
						// public double getLinkTravelTime(Link link, double time, Person person,
						// Vehicle vehicle) {
						// return link.getLength() / link.getFreespeed();
						// }
						// }
						this.linkTravelTimes);
				pSim.run();
				lastPseudoSimLinkUsages = pSimLinkUsageListener.getAndClearIndicators();
			}

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

			final AccelerationConfigGroup accConf = ConfigUtils.addOrGetModule(this.services.getConfig(),
					AccelerationConfigGroup.class);

			final ReplannerIdentifier replannerIdentifier = new ReplannerIdentifier(this.replanningParameters(),
					event.getIteration(), this.lastPhysicalLinkUsages, lastPseudoSimLinkUsages,
					this.services.getScenario().getPopulation(), null, // this.services.getLinkTravelTimes(),
					accConf.getModeTypeField(), personId2deltaScore, personId2oldScore, personId2newScore,
					deltaScoreTotal, accConf.getRandomizeIfNoImprovement(), accConf.getBaselineReplanningRate(),
					ttSum_h);
			this.replanners = replannerIdentifier.drawReplanners();

			replannerIdentifier.analyze(this.services.getScenario().getPopulation().getPersons().keySet(),
					this.lastPhysicalLinkUsages, lastPseudoSimLinkUsages, this.replanners, this.everReplanners,
					// this.lastReplanners, 
					this.lastReplannerIds2lastAndExpectedScore);

			this.statsWriter.writeToFile(replannerIdentifier);

			this.nextMobsimIsExpectedToBePhysical = true;
			this.setWeightOfHypotheticalReplanning(1e9);

		} else {

			this.nextMobsimIsExpectedToBePhysical = false;
			this.setWeightOfHypotheticalReplanning(0);

		}
	}

	// -------------------- REPLANNING FUNCTIONALITY --------------------

	public void replan(final HasPlansAndId<Plan, Person> person) {
		if ((this.replanners != null) && this.replanners.contains(person.getId())) {
			// This replaces the entire choice set and not just the selected plan. Why not.
			this.hypotheticalPopulationState.set(person);
		}
	}
}
