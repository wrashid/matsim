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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.SpaceTimeIndicators;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.TimeDiscretization;

/**
 * Keeps track of when every single PT passenger enters which vehicle.
 * 
 * @author Gunnar Flötteröd
 *
 */
public class TransitVehicleUsageListener implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler {

	// -------------------- MEMBERS --------------------

	// TODO redundant; is also encoded in vehicleUsages
	private final TimeDiscretization timeDiscretization;

	private final Population population;

	// Maps a person on all vehicle-time-slots used by that person.
	private final Map<Id<Person>, SpaceTimeIndicators<Id<Vehicle>>> passengerId2Entryindicators = new LinkedHashMap<>();

	// Keeps track of when a person last entered a vehicle.
	private final Map<Id<Person>, Double> passengerId2lastEntryTime = new LinkedHashMap<>();

	// Keeps track of each vehicle's on-board times.
	private final Map<Id<Vehicle>, Double> vehicleId2sumOfOnboardTimes = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public TransitVehicleUsageListener(final TimeDiscretization timeDiscretization, final Population population) {
		this.timeDiscretization = timeDiscretization;
		this.population = population;
	}

	// -------------------- IMPLEMENTATION --------------------

	Map<Id<Vehicle>, Double> getTransitVehicle2sumOfOnboardTimesView() {
		return Collections.unmodifiableMap(this.vehicleId2sumOfOnboardTimes);
	}

	Map<Id<Person>, SpaceTimeIndicators<Id<Vehicle>>> getAndClearIndicators() {
		final Map<Id<Person>, SpaceTimeIndicators<Id<Vehicle>>> result = new LinkedHashMap<>(
				this.passengerId2Entryindicators);
		this.passengerId2Entryindicators.clear();
		return result;
	}

	// --------------- IMPLEMENTATION OF EventHandler INTERFACES ---------------

	@Override
	public void reset(int iteration) {
		this.passengerId2Entryindicators.clear();
		this.passengerId2lastEntryTime.clear();
		this.vehicleId2sumOfOnboardTimes.clear();
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		final double time_s = event.getTime();
		final Id<Person> passengerId = event.getPersonId();
		if ((time_s >= this.timeDiscretization.getStartTime_s()) && (time_s < this.timeDiscretization.getEndTime_s())
				&& (this.population.getPersons().containsKey(passengerId))) {
			SpaceTimeIndicators<Id<Vehicle>> indicators = this.passengerId2Entryindicators.get(passengerId);
			if (indicators == null) {
				indicators = new SpaceTimeIndicators<Id<Vehicle>>(this.timeDiscretization.getBinCnt());
				this.passengerId2Entryindicators.put(passengerId, indicators);
			}
			final int bin = this.timeDiscretization.getBin(time_s);
			indicators.visit(event.getVehicleId(), bin);
			this.passengerId2lastEntryTime.put(passengerId, time_s);
		}
	}

	@Override
	public void handleEvent(final PersonLeavesVehicleEvent event) {
		final double time_s = event.getTime();
		if ((time_s >= this.timeDiscretization.getStartTime_s()) && (time_s < this.timeDiscretization.getEndTime_s())
				&& (this.population.getPersons().containsKey(event.getPersonId()))) {
			final double entryTime_s = this.passengerId2lastEntryTime.remove(event.getPersonId());
			final double newOnBoardTimeSum_s = this.vehicleId2sumOfOnboardTimes.getOrDefault(event.getVehicleId(), 0.0)
					+ (event.getTime() - entryTime_s);
			this.vehicleId2sumOfOnboardTimes.put(event.getVehicleId(), newOnBoardTimeSum_s);
		}
	}
}
