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
import org.matsim.contrib.pseudosimulation.searchacceleration.recipes.AccelerationRecipe;
import org.matsim.contrib.pseudosimulation.searchacceleration.recipes.Mah2007Recipe;
import org.matsim.contrib.pseudosimulation.searchacceleration.recipes.Mah2009Recipe;
import org.matsim.contrib.pseudosimulation.searchacceleration.recipes.ReplannerIdentifierRecipe;
import org.matsim.contrib.pseudosimulation.searchacceleration.recipes.UniformReplanningRecipe;
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
	private final Map<Id<Person>, Double> personId2newUtility;
	private final Map<Id<Person>, Double> personId2oldUtility;
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
			final AccelerationConfigGroup.ModeType modeTypeField, final Map<Id<Person>, Double> personId2UtilityChange,
			final Map<Id<Person>, Double> personId2oldUtility, final Map<Id<Person>, Double> personId2newUtility,
			final double totalUtilityChange, final boolean randomizeIfNoImprovement, final double minReplanningRate) {

		this.replanningParameters = replanningParameters;
		this.driverId2physicalLinkUsage = driverId2physicalLinkUsage;
		this.driverId2pseudoSimLinkUsage = driverId2pseudoSimLinkUsage;
		this.population = population;
		this.travelTimes = travelTimes;
		this.modeTypeField = modeTypeField;
		this.personId2utilityChange = personId2UtilityChange;
		this.personId2newUtility = personId2newUtility;
		this.personId2oldUtility = personId2oldUtility;
		this.totalUtilityChange = totalUtilityChange;
		this.randomizeIfNoImprovement = randomizeIfNoImprovement;
		this.minReplanningRate = minReplanningRate;

		this.meanLambda = this.replanningParameters.getMeanReplanningRate(iteration);

		this.currentWeightedCounts = CountIndicatorUtils.newWeightedCounts(timeDiscretization,
				this.driverId2physicalLinkUsage.values(), this.replanningParameters, this.travelTimes);
		this.upcomingWeightedCounts = CountIndicatorUtils.newWeightedCounts(timeDiscretization,
				this.driverId2pseudoSimLinkUsage.values(), this.replanningParameters, this.travelTimes);

		this.sumOfWeightedCountDifferences2 = CountIndicatorUtils.sumOfDifferences2(this.currentWeightedCounts,
				this.upcomingWeightedCounts);
		this.delta = this.replanningParameters.getRegularizationWeight(iteration, this.sumOfWeightedCountDifferences2);
		this.w = 2.0 * this.meanLambda * (this.sumOfWeightedCountDifferences2 + this.delta) / this.totalUtilityChange;
	}

	ReplannerIdentifier(final ReplannerIdentifier parent) {
		this.replanningParameters = parent.replanningParameters;
		this.meanLambda = parent.meanLambda;
		this.delta = parent.delta;
		this.travelTimes = parent.travelTimes;
		this.modeTypeField = parent.modeTypeField;
		this.personId2utilityChange = parent.personId2utilityChange;
		this.personId2newUtility = parent.personId2newUtility;
		this.personId2oldUtility = parent.personId2oldUtility;
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

		// Select the replanning recipe.

		final ReplannerIdentifierRecipe recipe;
		if (AccelerationConfigGroup.ModeType.off == this.modeTypeField) {
			recipe = new UniformReplanningRecipe(this.meanLambda);
		} else if (AccelerationConfigGroup.ModeType.accelerate == this.modeTypeField) {
			recipe = new AccelerationRecipe(this.randomizeIfNoImprovement, this.minReplanningRate, this.meanLambda);
		} else if (AccelerationConfigGroup.ModeType.mah2007 == this.modeTypeField) {
			recipe = new Mah2007Recipe(this.personId2utilityChange, this.meanLambda);
		} else if (AccelerationConfigGroup.ModeType.mah2009 == this.modeTypeField) {
			recipe = new Mah2009Recipe(this.personId2utilityChange, this.meanLambda);
		} else {
			throw new RuntimeException("Unknown mode: " + this.modeTypeField);
		}

		// Go through all vehicles and decide which driver gets to re-plan.

		final Set<Id<Person>> replanners = new LinkedHashSet<>();
		final List<Id<Person>> allPersonIdsShuffled = new ArrayList<>(this.population.getPersons().keySet());
		Collections.shuffle(allPersonIdsShuffled);

		this.score = this.getUniformReplanningObjectiveFunctionValue();
		this.expectedUniformSamplingObjectiveFunctionValue = this.getUniformReplanningObjectiveFunctionValue();

		int scoreImprovingReplanners = 0;

		for (Id<Person> driverId : allPersonIdsShuffled) {

			final ScoreUpdater<Id<Link>> scoreUpdater = new ScoreUpdater<>(
					this.driverId2physicalLinkUsage.get(driverId), this.driverId2pseudoSimLinkUsage.get(driverId),
					this.meanLambda, this.w, this.delta, interactionResiduals, regularizationResidual,
					this.replanningParameters, this.travelTimes, this.personId2utilityChange.get(driverId));

			this.expectedUniformSamplingObjectiveFunctionValue += this.meanLambda * scoreUpdater.getScoreChangeIfOne()
					+ (1.0 - this.meanLambda) * scoreUpdater.getScoreChangeIfZero();

			final boolean scoreImprover = (Math.min(scoreUpdater.getScoreChangeIfOne(),
					scoreUpdater.getScoreChangeIfZero()) < 0);
			if (scoreImprover) {
				scoreImprovingReplanners++;
			}

			final boolean replanner = recipe.isReplanner(driverId, scoreUpdater.getScoreChangeIfOne(),
					scoreUpdater.getScoreChangeIfZero());

			if (replanner) {
				replanners.add(driverId);
				this.score += scoreUpdater.getScoreChangeIfOne();
			} else {
				this.score += scoreUpdater.getScoreChangeIfZero();
			}

			scoreUpdater.updateResiduals(replanner ? 1.0 : 0.0); // interaction residual by reference
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
