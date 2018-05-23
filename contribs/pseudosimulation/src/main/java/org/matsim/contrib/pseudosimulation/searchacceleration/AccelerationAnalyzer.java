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

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.SpaceTimeIndicators;
import org.matsim.contrib.pseudosimulation.searchacceleration.utils.SetUtils;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class AccelerationAnalyzer {

	// -------------------- MEMBERS --------------------

	private final String compareToUniformReplanningFileName = "acceleration_compare-to-uniform.csv";

	private final ReplanningParameterContainer replParams;

	private final TimeDiscretization timeDiscr;

	private Integer driversInPseudoSim = null;

	private Integer driversInPhysicalSim = null;

	private Double effectiveReplanninRate = null;

	private Double repeatedReplanningProba = null;

	private Double shareNeverReplanned = null;

	private final Set<Id<Person>> everReplanners = new LinkedHashSet<>();

	private Set<Id<Person>> lastReplanners = null;

	private List<Double> bootstrap = null;

	// -------------------- CONSTRUCTION --------------------

	AccelerationAnalyzer(final ReplanningParameterContainer replParams, final TimeDiscretization timeDiscr) {
		this.replParams = replParams;
		this.timeDiscr = timeDiscr;
		try {
			final Path path = Paths.get(this.compareToUniformReplanningFileName);
			Files.deleteIfExists(path);
			Files.createFile(path);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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

	// -------------------- IMPLEMENTATION --------------------

	public void analyze(final Set<Id<Person>> allPersonIds,
			final Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> driverId2physicalSimUsage,
			final Map<Id<Person>, SpaceTimeIndicators<Id<Link>>> driverId2pseudoSimUsage,
			final Set<Id<Person>> replannerIds, final int iteration, final List<Double> bootstrap) {

		this.bootstrap = bootstrap;

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

		// >>> OLD ANALYSIS BELOW >>>

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

		final List<Double> diffList = new ArrayList<>();
		for (Id<Link> linkId : SetUtils.union(uniformDeltaN.keySet(), optimizedDeltaN.keySet())) {
			double sum = 0;
			for (int timeBin = 0; timeBin < this.timeDiscr.getBinCnt(); timeBin++) {
				sum += (optimizedDeltaN.getBinValue(linkId, timeBin) - uniformDeltaN.getBinValue(linkId, timeBin));
			}
			if (Math.abs(sum) >= 1e-3) {
				diffList.add(sum);
			}
		}
		Collections.sort(diffList);

		if (diffList.size() > 0) {
			final Path path = Paths.get(this.compareToUniformReplanningFileName);
			try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.APPEND)) {
				writer.write(diffList.get(0).toString());
				for (int i = 1; i < diffList.size(); i++) {
					writer.write("," + diffList.get(i));
				}
				writer.newLine();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}
