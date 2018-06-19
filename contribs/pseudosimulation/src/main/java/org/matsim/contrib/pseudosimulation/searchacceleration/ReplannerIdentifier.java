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

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ReplannerIdentifier {

	// -------------------- MEMBERS --------------------

	private final AccelerationConfigGroup replanningParameters;
	private final TravelTime travelTimes;
	private final double currentLambda; // current
	private final double currentDelta; // current

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

	private Integer driversInPseudoSim = null;

	private Integer driversInPhysicalSim = null;

	private Double effectiveReplanninRate = null;

	private Double repeatedReplanningProba = null;

	private Double shareNeverReplanned = null;

	// -------------------- GETTERS (FOR LOGGING) --------------------

	public Integer getDriversInPseudoSim() {
		return this.driversInPseudoSim;
	}

	public Integer getDriversInPhysicalSim() {
		return this.driversInPhysicalSim;
	}

	public Double getEffectiveReplanninRate() {
		return this.effectiveReplanninRate;
	}

	public Double getRepeatedReplanningProba() {
		return this.repeatedReplanningProba;
	}

	public Double getShareNeverReplanned() {
		return this.shareNeverReplanned;
	}

	public double getUniformReplanningObjectiveFunctionValue() {
		return (2.0 - this.currentLambda) * this.currentLambda
				* (this.sumOfWeightedCountDifferences2 + this.currentDelta);
	}

	public Double getUniformityExcess() {
		return (2.0 - this.currentLambda) * this.currentLambda * this.sumOfWeightedCountDifferences2
				/ this.currentDelta;
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

	public Double getMeanReplanningRate() {
		return this.currentLambda;
	}

	public Double getRegularizationWeight() {
		return this.currentDelta;
	}

	// -------------------- CONSTRUCTION --------------------

	ReplannerIdentifier(final AccelerationConfigGroup replanningParameters, final int iteration,
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
		this.personId2utilityChange = personId2UtilityChange;
		this.personId2newUtility = personId2newUtility;
		this.personId2oldUtility = personId2oldUtility;
		this.totalUtilityChange = totalUtilityChange;

		this.currentWeightedCounts = CountIndicatorUtils.newWeightedCounts(this.driverId2physicalLinkUsage.values(),
				this.replanningParameters, this.travelTimes);
		this.upcomingWeightedCounts = CountIndicatorUtils.newWeightedCounts(this.driverId2pseudoSimLinkUsage.values(),
				this.replanningParameters, this.travelTimes);

		this.sumOfWeightedCountDifferences2 = CountIndicatorUtils.sumOfDifferences2(this.currentWeightedCounts,
				this.upcomingWeightedCounts);
		this.currentLambda = this.replanningParameters.getMeanReplanningRate(iteration);
		this.currentDelta = this.replanningParameters.getRegularizationWeight(iteration,
				this.sumOfWeightedCountDifferences2);
		this.w = 2.0 * this.currentLambda * (this.sumOfWeightedCountDifferences2 + this.currentDelta)
				/ this.totalUtilityChange;
	}

	// -------------------- IMPLEMENTATION --------------------

	Set<Id<Person>> drawReplanners() {

		// Initialize score residuals.

		final DynamicData<Id<Link>> interactionResiduals = CountIndicatorUtils
				.newWeightedDifference(this.upcomingWeightedCounts, this.currentWeightedCounts, this.currentLambda);
		double regularizationResidual = this.currentLambda * this.totalUtilityChange;

		// Select the replanning recipe.

		final ReplannerIdentifierRecipe recipe;
		if (AccelerationConfigGroup.ModeType.off == this.replanningParameters.getModeTypeField()) {
			recipe = new UniformReplanningRecipe(this.currentLambda);
		} else if (AccelerationConfigGroup.ModeType.accelerate == this.replanningParameters.getModeTypeField()) {
			recipe = new AccelerationRecipe(this.replanningParameters.getRandomizeIfNoImprovement(),
					this.replanningParameters.getBaselineReplanningRate(), this.currentLambda);
		} else if (AccelerationConfigGroup.ModeType.mah2007 == this.replanningParameters.getModeTypeField()) {
			recipe = new Mah2007Recipe(this.personId2utilityChange, this.currentLambda);
		} else if (AccelerationConfigGroup.ModeType.mah2009 == this.replanningParameters.getModeTypeField()) {
			recipe = new Mah2009Recipe(this.personId2utilityChange, this.currentLambda);
		} else {
			throw new RuntimeException("Unknown mode: " + this.replanningParameters.getModeTypeField());
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
					this.currentLambda, this.w, this.currentDelta, interactionResiduals, regularizationResidual,
					this.replanningParameters, this.travelTimes, this.personId2utilityChange.get(driverId),
					this.totalUtilityChange);

			this.expectedUniformSamplingObjectiveFunctionValue += this.currentLambda
					* scoreUpdater.getScoreChangeIfOne()
					+ (1.0 - this.currentLambda) * scoreUpdater.getScoreChangeIfZero();

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

	public void analyze(final Set<Id<Person>> allPersonIds,
			final Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> driverId2physicalSimUsage,
			final Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> driverId2pseudoSimUsage,
			final Set<Id<Person>> replannerIds, final Set<Id<Person>> everReplanners,
			final Set<Id<Person>> lastReplanners) {

		this.driversInPhysicalSim = driverId2physicalSimUsage.size();
		this.driversInPseudoSim = driverId2pseudoSimUsage.size();

		this.effectiveReplanninRate = ((double) replannerIds.size()) / allPersonIds.size();

		everReplanners.addAll(replannerIds);
		this.shareNeverReplanned = 1.0 - ((double) everReplanners.size()) / allPersonIds.size();

		final int lastReplannerCnt = lastReplanners.size();
		lastReplanners.retainAll(replannerIds);
		this.repeatedReplanningProba = ((double) lastReplanners.size()) / lastReplannerCnt;

		lastReplanners.clear();
		lastReplanners.addAll(replannerIds);
	}

}
