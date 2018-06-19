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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.SpaceTimeIndicators;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class AccelerationAnalyzer {

	// -------------------- MEMBERS --------------------

	private Integer driversInPseudoSim = null;

	private Integer driversInPhysicalSim = null;

	private Double effectiveReplanninRate = null;

	private Double repeatedReplanningProba = null;

	private Double shareNeverReplanned = null;

	private final Set<Id<Person>> everReplanners = new LinkedHashSet<>();

	private Set<Id<Person>> lastReplanners = null;

	private List<Double> bootstrap = null;

	private Double uniformReplanningObjectiveFunctionValue = null;

	private Double shareOfScoreImprovingReplanners = null;

	private Double finalObjectiveFunctionValue = null;

	private Double uniformityExcess = null;

	private Double expectedUniformSamplingObjectiveFunctionValue = null;

	private Double sumOfWeightedCountDifferences2 = null;

	// -------------------- CONSTRUCTION --------------------

	AccelerationAnalyzer() {
	}

	// -------------------- TODO LOGGING GETTERS --------------------

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

	public List<Double> getBootstrap() {
		return this.bootstrap;
	}

	public double getUniformReplanningObjectiveFunctionValue() {
		return this.uniformReplanningObjectiveFunctionValue;
	}

	public Double getShareOfScoreImprovingReplanners() {
		return this.shareOfScoreImprovingReplanners;
	}

	public Double getFinalObjectiveFunctionValue() {
		return this.finalObjectiveFunctionValue;
	}

	public Double getUniformityExcess() {
		return this.uniformityExcess;
	}

	public Double getExpectedUniformSamplingObjectiveFunctionValue() {
		return this.expectedUniformSamplingObjectiveFunctionValue;
	}

	public Double getSumOfWeightedCountDifferences2() {
		return this.sumOfWeightedCountDifferences2;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void analyze(final Set<Id<Person>> allPersonIds,
			final Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> driverId2physicalSimUsage,
			final Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> driverId2pseudoSimUsage,
			final Set<Id<Person>> replannerIds, final List<Double> bootstrap, final Double uniformReplanningObjectiveFunctionValue,
			final Double shareOfScoreImprovingReplanners, final Double finalObjectiveFunctionValue,
			final Double uniformityExcess, final Double expectedUniformSamplingObjectiveFunctionValue, final Double sumOfWeightedCountDifferences2) {

		this.bootstrap = bootstrap;
		this.uniformReplanningObjectiveFunctionValue = uniformReplanningObjectiveFunctionValue;
		this.shareOfScoreImprovingReplanners = shareOfScoreImprovingReplanners;
		this.finalObjectiveFunctionValue = finalObjectiveFunctionValue;
		this.uniformityExcess = uniformityExcess;
		this.expectedUniformSamplingObjectiveFunctionValue = expectedUniformSamplingObjectiveFunctionValue;

		this.sumOfWeightedCountDifferences2 = sumOfWeightedCountDifferences2;

		this.driversInPhysicalSim = driverId2physicalSimUsage.size();
		this.driversInPseudoSim = driverId2pseudoSimUsage.size();

		this.effectiveReplanninRate = ((double) replannerIds.size()) / allPersonIds.size();

		this.everReplanners.addAll(replannerIds);
		this.shareNeverReplanned = 1.0 - ((double) this.everReplanners.size()) / allPersonIds.size();

		if (this.lastReplanners != null) {
			final int lastReplannerCnt = this.lastReplanners.size();
			this.lastReplanners.retainAll(replannerIds);
			this.repeatedReplanningProba = ((double) this.lastReplanners.size()) / lastReplannerCnt;
		}
		this.lastReplanners = new LinkedHashSet<>(replannerIds);
	}
}
