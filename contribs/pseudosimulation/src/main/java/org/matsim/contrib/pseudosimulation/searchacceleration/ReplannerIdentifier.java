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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.CountIndicatorUtils;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.ScoreUpdater;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.SpaceTimeIndicators;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.util.TravelTime;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class ReplannerIdentifier {

	// -------------------- MEMBERS --------------------

	private final ReplanningParameterContainer replanningParameters;
	private final double meanLambda; // somewhat redundant
	private final double delta; // somewhat redundant
	private final TravelTime travelTimes;
	private final AccelerationConfigGroup.ModeType modeTypeField;
	// private final boolean accelerate;
	private final boolean randomizeIfNoImprovement;
	private final double minReplanningRate;

	private final Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> driverId2physicalLinkUsage;
	private final Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> driverId2pseudoSimLinkUsage;
	private final Population population;

	private final DynamicData<Id<Link>> currentWeightedCounts;
	private final DynamicData<Id<Link>> upcomingWeightedCounts;

	private final double sumOfWeightedCountDifferences2;
	private final double w;

	private final Map<Id<Person>, Double> personId2utilityChange;
	private final double totalUtilityChange;

	private Double shareOfScoreImprovingReplanners = null;
	private Double score = null;
	private Double expectedUniformSamplingObjectiveFunctionValue = null;

	// -------------------- CONSTRUCTION --------------------

	ReplannerIdentifier(final ReplanningParameterContainer replanningParameters,
			final TimeDiscretization timeDiscretization, final int iteration,
			final Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> driverId2physicalLinkUsage,
			final Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> driverId2pseudoSimLinkUsage,
			final Population population, final TravelTime travelTimes,
			final AccelerationConfigGroup.ModeType modeTypeField,
			// final boolean accelerate,
			final Map<Id<Person>, Double> personId2UtilityChange, final double totalUtilityChange,
			final boolean randomizeIfNoImprovement, final double minReplanningRate) {

		this.replanningParameters = replanningParameters;
		this.driverId2physicalLinkUsage = driverId2physicalLinkUsage;
		this.driverId2pseudoSimLinkUsage = driverId2pseudoSimLinkUsage;
		this.population = population;
		this.travelTimes = travelTimes;
		this.modeTypeField = modeTypeField;
		// this.accelerate = accelerate;
		this.personId2utilityChange = personId2UtilityChange;
		this.totalUtilityChange = totalUtilityChange;
		this.randomizeIfNoImprovement = randomizeIfNoImprovement;
		this.minReplanningRate = minReplanningRate;

		this.meanLambda = this.replanningParameters.getMeanLambda(iteration);

		this.currentWeightedCounts = CountIndicatorUtils.newWeightedCounts(timeDiscretization,
				this.driverId2physicalLinkUsage.values(), this.replanningParameters, this.travelTimes);
		this.upcomingWeightedCounts = CountIndicatorUtils.newWeightedCounts(timeDiscretization,
				this.driverId2pseudoSimLinkUsage.values(), this.replanningParameters, this.travelTimes);

		this.sumOfWeightedCountDifferences2 = CountIndicatorUtils.sumOfDifferences2(this.currentWeightedCounts,
				this.upcomingWeightedCounts);
		this.delta = this.replanningParameters.getDelta(iteration, this.sumOfWeightedCountDifferences2);
		this.w = 2.0 * this.meanLambda * (this.sumOfWeightedCountDifferences2 + this.delta) / this.totalUtilityChange;
	}

	ReplannerIdentifier(final ReplannerIdentifier parent) {
		this.replanningParameters = parent.replanningParameters;
		this.meanLambda = parent.meanLambda;
		this.delta = parent.delta;
		this.travelTimes = parent.travelTimes;
		// this.accelerate = parent.accelerate;
		this.modeTypeField = parent.modeTypeField;
		this.personId2utilityChange = parent.personId2utilityChange;
		this.minReplanningRate = parent.minReplanningRate;

		this.driverId2physicalLinkUsage = parent.driverId2physicalLinkUsage;
		this.driverId2pseudoSimLinkUsage = parent.driverId2pseudoSimLinkUsage;
		this.population = parent.population;

		this.currentWeightedCounts = parent.currentWeightedCounts;
		this.upcomingWeightedCounts = parent.upcomingWeightedCounts;

		this.sumOfWeightedCountDifferences2 = parent.sumOfWeightedCountDifferences2;
		this.w = parent.w;
		this.totalUtilityChange = parent.totalUtilityChange;
		this.randomizeIfNoImprovement = parent.randomizeIfNoImprovement;
	}

	// -------------------- IMPLEMENTATION --------------------

	public double getUniformReplanningObjectiveFunctionValue() {
		return (2.0 - this.meanLambda) * this.meanLambda * (this.sumOfWeightedCountDifferences2 + this.delta);
	}

	public double getUniformityExcess() {
		return (2.0 - this.meanLambda) * this.meanLambda * this.sumOfWeightedCountDifferences2 / this.delta;
	}

	public Double getShareOfScoreImprovingReplanners() {
		return this.shareOfScoreImprovingReplanners;
	}

	public Double getFinalObjectiveFunctionValue() {
		return this.score;
	}

	public Double getExpectedUniformSamplingObjectiveFunctionValue() {
		return this.expectedUniformSamplingObjectiveFunctionValue;
	}

	public Double getSumOfWeightedCountDifferences2() {
		return this.sumOfWeightedCountDifferences2;
	}

	Set<Id<Person>> drawReplanners() {

		// Initialize score residuals.

		final DynamicData<Id<Link>> interactionResiduals = CountIndicatorUtils
				.newWeightedDifference(this.upcomingWeightedCounts, this.currentWeightedCounts, this.meanLambda);
		double regularizationResidual = this.meanLambda * this.totalUtilityChange;

		// Go through all vehicles and decide which driver gets to re-plan.

		final Set<Id<Person>> replanners = new LinkedHashSet<>();
		final List<Id<Person>> allPersonIdsShuffled = new ArrayList<>(this.population.getPersons().keySet());
		Collections.shuffle(allPersonIdsShuffled);

		// >>>>> HANI >>>>>

		final Set<Id<Person>> haniReplannerIds;
		if (AccelerationConfigGroup.ModeType.hani == this.modeTypeField) {
			final List<Map.Entry<Id<Person>, Double>> person2utilityGainEntries = new ArrayList<>(
					this.personId2utilityChange.entrySet());
			final Comparator<Map.Entry<Id<Person>, Double>> byUtility = new Comparator() {
				@Override
				public int compare(Object o1, Object o2) {
					final Map.Entry<Id<Person>, Double> entry1 = (Map.Entry<Id<Person>, Double>) o1;
					final Map.Entry<Id<Person>, Double> entry2 = (Map.Entry<Id<Person>, Double>) o2;
					if (entry1.getValue() > entry2.getValue()) {
						return -1;
					} else if (entry1.getValue() < entry2.getValue()) {
						return 1;
					} else {
						return 0;
					}
				}
			};
			Collections.sort(person2utilityGainEntries, byUtility);
			haniReplannerIds = new LinkedHashSet<>();
			for (int i = 0; i < this.meanLambda * this.personId2utilityChange.size(); i++) {
				haniReplannerIds.add(person2utilityGainEntries.get(i).getKey());
			}
			System.out.println(haniReplannerIds);
		} else {
			haniReplannerIds = null;
		}

		// <<<<< HANI <<<<<

		// final Set<Id<Person>> minReplanners = new LinkedHashSet<>();
		// for (Id<Person> personId : allPersonIdsShuffled) {
		// if (MatsimRandom.getRandom().nextDouble() < this.minReplanningRate) {
		// minReplanners.add(personId);
		// allPersonIdsShuffled.remove(personId);
		// }
		// }

		this.score = this.getUniformReplanningObjectiveFunctionValue();
		this.expectedUniformSamplingObjectiveFunctionValue = this.getUniformReplanningObjectiveFunctionValue();

		int scoreImprovingReplanners = 0;

		int i = 0;
		for (Id<Person> driverId : allPersonIdsShuffled) {
			i++;

			final ScoreUpdater<Id<Link>> scoreUpdater = new ScoreUpdater<>(
					this.driverId2physicalLinkUsage.get(driverId), this.driverId2pseudoSimLinkUsage.get(driverId),
					this.meanLambda, this.w, this.delta, interactionResiduals, regularizationResidual,
					this.replanningParameters, this.travelTimes, this.personId2utilityChange.get(driverId));
			final double newLambda;

			this.expectedUniformSamplingObjectiveFunctionValue += this.meanLambda * scoreUpdater.getScoreChangeIfOne()
					+ (1.0 - this.meanLambda) * scoreUpdater.getScoreChangeIfZero();

			final boolean scoreImprover = (Math.min(scoreUpdater.getScoreChangeIfOne(),
					scoreUpdater.getScoreChangeIfZero()) < 0);
			if (scoreImprover) {
				scoreImprovingReplanners++;
			}

			if (((double) i) / allPersonIdsShuffled.size() < this.minReplanningRate) {

				newLambda = 1.0;
				replanners.add(driverId);
				this.score += scoreUpdater.getScoreChangeIfOne();

			} else if (AccelerationConfigGroup.ModeType.hani == this.modeTypeField) {

				if (haniReplannerIds.contains(driverId)) {
					newLambda = 1.0;
					replanners.add(driverId);
					this.score += scoreUpdater.getScoreChangeIfOne();
				} else {
					newLambda = 0;
					this.score += scoreUpdater.getScoreChangeIfZero();
				}

			} else if ((AccelerationConfigGroup.ModeType.accelerate == this.modeTypeField)
					&& (scoreImprover || !this.randomizeIfNoImprovement)) {

				if (scoreUpdater.getScoreChangeIfOne() < scoreUpdater.getScoreChangeIfZero()) {
					newLambda = 1.0;
					replanners.add(driverId);
					this.score += scoreUpdater.getScoreChangeIfOne();
				} else {
					newLambda = 0.0;
					this.score += scoreUpdater.getScoreChangeIfZero();
				}

			} else {

				if (MatsimRandom.getRandom().nextDouble() < this.meanLambda) {
					newLambda = 1.0;
					replanners.add(driverId);
					this.score += scoreUpdater.getScoreChangeIfOne();
				} else {
					newLambda = 0.0;
					this.score += scoreUpdater.getScoreChangeIfZero();
				}
			}

			scoreUpdater.updateResiduals(newLambda); // interaction residual by reference
			regularizationResidual = scoreUpdater.getUpdatedRegularizationResidual();
		}

		this.shareOfScoreImprovingReplanners = ((double) scoreImprovingReplanners) / allPersonIdsShuffled.size();

		return replanners;
	}

	List<Double> bootstrap(final int replications) {

		final Map<Id<Person>, Integer> personId2replanCnt = new LinkedHashMap<>();
		for (Id<Person> personId : this.population.getPersons().keySet()) {
			personId2replanCnt.put(personId, 0);
		}

		for (int repl = 0; repl < replications; repl++) {
			final ReplannerIdentifier identifier = new ReplannerIdentifier(this);
			final Set<Id<Person>> replanners = identifier.drawReplanners();
			for (Id<Person> replannerId : replanners) {
				personId2replanCnt.put(replannerId, personId2replanCnt.get(replannerId) + 1);
			}
		}

		final List<Double> stats = new ArrayList<Double>();
		for (int i = 0; i <= replications; i++) {
			stats.add(0.0);
		}
		for (Integer cnt : personId2replanCnt.values()) {
			stats.set(cnt, stats.get(cnt) + 1.0 / this.population.getPersons().size());
		}
		return Collections.unmodifiableList(stats);
	}
}
