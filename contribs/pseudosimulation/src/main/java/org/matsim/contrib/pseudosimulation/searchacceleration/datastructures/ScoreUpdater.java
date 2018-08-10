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
package org.matsim.contrib.pseudosimulation.searchacceleration.datastructures;

import java.util.Map;

import org.matsim.contrib.pseudosimulation.searchacceleration.AccelerationConfigGroup;
import org.matsim.core.router.util.TravelTime;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.Tuple;

/**
 * The "score" this class refers to is the anticipated change of the search
 * acceleration objective function resulting from setting a single agent's
 * (possibly space-weighted) 0/1 re-planning indicator.
 * 
 * Implements the score used in the greedy heuristic of Merz, P. and Freisleben,
 * B. (2002). "Greedy and local search heuristics for unconstrained binary
 * quadratic programming." Journal of Heuristics 8:197–213.
 * 
 * @author Gunnar Flötteröd
 * 
 * @param L
 *            the space coordinate type
 *
 */
public class ScoreUpdater<L> {

	// -------------------- MEMBERS --------------------

	private final DynamicData<L> interactionResiduals;

	private double inertiaResidual;

	private double regularizationResidual;

	private final double individualUtilityChange;

	private final SpaceTimeCounts<L> individualWeightedChanges;

	private final double scoreChangeIfZero;

	private final double scoreChangeIfOne;

	private boolean residualsUpdated = false;

	private final double deltaForUniformReplanning;

	// -------------------- CONSTRUCTION --------------------

	public ScoreUpdater(final SpaceTimeIndicators<L> currentIndicators, final SpaceTimeIndicators<L> upcomingIndicators,
			final double meanLambda, final double beta, final double delta, final DynamicData<L> interactionResiduals,
			final double inertiaResidual, final double regularizationResidual, final AccelerationConfigGroup replParams,
			final TravelTime travelTimes, final double individualUtilityChange, final double totalUtilityChange) {

		this.interactionResiduals = interactionResiduals;
		this.inertiaResidual = inertiaResidual;
		this.regularizationResidual = regularizationResidual;

		this.individualUtilityChange = individualUtilityChange;

		/*
		 * One has to go beyond 0/1 indicator arithmetics in the following because the
		 * same vehicle may enter the same link multiple times during one time bin.
		 */

		this.individualWeightedChanges = new SpaceTimeCounts<>(upcomingIndicators, replParams, travelTimes);
		this.individualWeightedChanges.subtract(new SpaceTimeCounts<>(currentIndicators, replParams, travelTimes));

		// Update the residuals.

		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.individualWeightedChanges.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double weightedIndividualChange = entry.getValue();
			this.interactionResiduals.add(spaceObj, timeBin, -meanLambda * weightedIndividualChange);
		}

		this.inertiaResidual -= (1.0 - meanLambda) * this.individualUtilityChange;

		this.regularizationResidual -= meanLambda;

		// Compute individual score terms.

		double sumOfWeightedIndividualChanges2 = 0.0;
		double sumOfWeightedIndividualChangesTimesInteractionResiduals = 0.0;

		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.individualWeightedChanges.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			final double weightedIndividualChange = entry.getValue();
			sumOfWeightedIndividualChanges2 += weightedIndividualChange * weightedIndividualChange;
			sumOfWeightedIndividualChangesTimesInteractionResiduals += weightedIndividualChange
					* this.interactionResiduals.getBinValue(spaceObj, timeBin);
		}

		final double sumOfInteractionResiduals2 = CountIndicatorUtils.sumOfEntries2(this.interactionResiduals);

		// Compose the actual score change.

		final double expectedScoreIfOne = this.expectedScore(1.0, sumOfWeightedIndividualChanges2,
				sumOfWeightedIndividualChangesTimesInteractionResiduals, sumOfInteractionResiduals2,
				individualUtilityChange, inertiaResidual, regularizationResidual, beta, delta);
		final double expectedScoreIfZero = this.expectedScore(0.0, sumOfWeightedIndividualChanges2,
				sumOfWeightedIndividualChangesTimesInteractionResiduals, sumOfInteractionResiduals2,
				individualUtilityChange, inertiaResidual, regularizationResidual, beta, delta);
		final double expectedScoreIfMean = this.expectedScore(meanLambda, sumOfWeightedIndividualChanges2,
				sumOfWeightedIndividualChangesTimesInteractionResiduals, sumOfInteractionResiduals2,
				individualUtilityChange, inertiaResidual, regularizationResidual, beta, delta);

		this.scoreChangeIfOne = expectedScoreIfOne - expectedScoreIfMean;
		this.scoreChangeIfZero = expectedScoreIfZero - expectedScoreIfMean;

		// >>> TODO NEW >>>

		final double deltaInteraction = this.expectedInteraction(1.0, sumOfWeightedIndividualChanges2,
				sumOfWeightedIndividualChangesTimesInteractionResiduals, sumOfInteractionResiduals2)
				- this.expectedInteraction(0.0, sumOfWeightedIndividualChanges2,
						sumOfWeightedIndividualChangesTimesInteractionResiduals, sumOfInteractionResiduals2);
		final double deltaInertia = this.expectedInertia(1.0, individualUtilityChange, inertiaResidual)
				- this.expectedInertia(0.0, individualUtilityChange, inertiaResidual);
		this.deltaForUniformReplanning = -(deltaInteraction + beta * deltaInertia);

		// <<< TODO NEW <<<
	}

	private double expectedScore(final double lambda, final double sumOfWeightedIndividualChanges2,
			final double sumOfWeightedIndividualChangesTimesInteractionResiduals,
			final double sumOfInteractionResiduals2, final double individualUtilityChange, final double inertiaResidual,
			final double regularizationResidual, final double beta, final double delta) {
		return this.expectedInteraction(lambda, sumOfWeightedIndividualChanges2,
				sumOfWeightedIndividualChangesTimesInteractionResiduals, sumOfInteractionResiduals2)
				+ beta * this.expectedInertia(lambda, individualUtilityChange, inertiaResidual)
				+ delta * this.expectedRegularization(lambda, regularizationResidual);
	}

	private double expectedInteraction(final double lambda, final double sumOfWeightedIndividualChanges2,
			final double sumOfWeightedIndividualChangesTimesInteractionResiduals,
			final double sumOfInteractionResiduals2) {
		return lambda * lambda * sumOfWeightedIndividualChanges2
				+ 2.0 * lambda * sumOfWeightedIndividualChangesTimesInteractionResiduals + sumOfInteractionResiduals2;
	}

	private double expectedInertia(final double lambda, final double individualUtilityChange,
			final double inertiaResidual) {
		return (1.0 - lambda) * individualUtilityChange + inertiaResidual;
	}

	private double expectedRegularization(final double lambda, final double regularizationResidual) {
		return lambda * lambda + 2.0 * lambda * regularizationResidual
				+ regularizationResidual * regularizationResidual;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void updateResiduals(final double newLambda) {
		if (this.residualsUpdated) {
			throw new RuntimeException("Residuals have already been updated.");
		}
		this.residualsUpdated = true;

		for (Map.Entry<Tuple<L, Integer>, Double> entry : this.individualWeightedChanges.entriesView()) {
			final L spaceObj = entry.getKey().getA();
			final int timeBin = entry.getKey().getB();
			this.interactionResiduals.add(spaceObj, timeBin, newLambda * entry.getValue());
		}
		this.inertiaResidual += (1.0 - newLambda) * this.individualUtilityChange;
		this.regularizationResidual += newLambda;
	}

	// -------------------- GETTERS --------------------

	public double getUpdatedInertiaResidual() {
		if (!this.residualsUpdated) {
			throw new RuntimeException("Residuals have not yet updated.");
		}
		return this.inertiaResidual;
	}

	public double getUpdatedRegularizationResidual() {
		if (!this.residualsUpdated) {
			throw new RuntimeException("Residuals have not yet updated.");
		}
		return this.regularizationResidual;
	}

	public double getScoreChangeIfOne() {
		return this.scoreChangeIfOne;
	}

	public double getScoreChangeIfZero() {
		return this.scoreChangeIfZero;
	}

	public Double getDeltaForUniformReplanning() {
		return this.deltaForUniformReplanning;
	}
}
