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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.SpaceTimeIndicators;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
class AccelerationAnalyzer {

	// -------------------- MEMBERS --------------------

	private final ReplanningParameterContainer replParams;

	private final TimeDiscretization timeDiscr;

	// -------------------- CONSTRUCTION --------------------

	AccelerationAnalyzer(final ReplanningParameterContainer replParams, final TimeDiscretization timeDiscr) {
		this.replParams = replParams;
		this.timeDiscr = timeDiscr;
	}

	// -------------------- IMPLEMENTATION --------------------

	public void analyze(final Set<Id<Person>> allPersonIds,
			final Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> driverId2physicalSimUsage,
			final Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> driverId2pseudoSimUsage,
			final Set<Id<Person>> replannerIds, final int iteration) {

		final double meanLambda = this.replParams.getMeanLambda(iteration);

		final DynamicData<Id<Link>> uniformDeltaN = new DynamicData<>(this.timeDiscr);
		for (Id<Person> personId : allPersonIds) {
			for (int timeBin = 0; timeBin < this.timeDiscr.getBinCnt(); timeBin++) {
				if (driverId2pseudoSimUsage.containsKey(personId)) {
					for (Id<Link> newLink : driverId2pseudoSimUsage.get(personId).getVisitedSpaceObjects(timeBin)) {
						uniformDeltaN.add(newLink, timeBin, meanLambda);
					}
				}
				if (driverId2physicalSimUsage.containsKey(personId)) {
					for (Id<Link> oldLink : driverId2physicalSimUsage.get(personId).getVisitedSpaceObjects(timeBin)) {
						uniformDeltaN.add(oldLink, timeBin, -meanLambda);
					}
				}
			}
		}

		final DynamicData<Id<Link>> optimizedDeltaN = new DynamicData<>(this.timeDiscr);
		for (Id<Person> personId : replannerIds) {
			for (int timeBin = 0; timeBin < this.timeDiscr.getBinCnt(); timeBin++) {
				if (driverId2pseudoSimUsage.containsKey(personId)) {
					for (Id<Link> newLink : driverId2pseudoSimUsage.get(personId).getVisitedSpaceObjects(timeBin)) {
						optimizedDeltaN.add(newLink, timeBin, 1.0);
					}
				}
				if (driverId2physicalSimUsage.containsKey(personId)) {
					for (Id<Link> oldLink : driverId2physicalSimUsage.get(personId).getVisitedSpaceObjects(timeBin)) {
						optimizedDeltaN.add(oldLink, timeBin, -1.0);
					}
				}
			}
		}

		// TODO effect of optimized re-planning

		this.diffList.clear();
		for (Id<Link> linkId : uniformDeltaN.keySet()) {
			double sum = 0;
			for (int timeBin = 0; timeBin < this.timeDiscr.getBinCnt(); timeBin++) {
				sum += (optimizedDeltaN.getBinValue(linkId, timeBin) - uniformDeltaN.getBinValue(linkId, timeBin));
			}
			this.diffList.add(sum);
		}
		Collections.sort(this.diffList);
	}

	// TODO
	public final List<Double> diffList = new ArrayList<>();
}
