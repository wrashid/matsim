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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.TransitDriverStartsEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.TransitDriverStartsEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.pseudosimulation.searchacceleration.datastructures.SpaceTimeIndicators;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.Tuple;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class TransitVehicleUsageListener implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler,
		VehicleDepartsAtFacilityEventHandler, TransitDriverStartsEventHandler {

	// -------------------- CONSTANTS --------------------

	private static final Set<Id<Person>> emptyPassengerSet = Collections.unmodifiableSet(new HashSet<>(0));

	// -------------------- MEMBERS --------------------

	private final TimeDiscretization timeDiscretization;

	private final Map<Id<Vehicle>, Set<Id<Person>>> vehicleId2passengerIds = new LinkedHashMap<>();

	private final Map<Id<Person>, SpaceTimeIndicators<Tuple<Id<Vehicle>, Id<TransitStopFacility>>>> passengerId2vehicleUsageIndicators = new LinkedHashMap<>();

	private boolean debug = true;

	// -------------------- CONSTRUCTION --------------------

	public TransitVehicleUsageListener(final TimeDiscretization timeDiscretization) {
		this.timeDiscretization = timeDiscretization;
	}

	public void setDebug(final boolean debug) {
		this.debug = debug;
	}

	// -------------------- INTERNALS --------------------

	// -------------------- IMPLEMENTATION OF EVENT LISTENERS --------------------

	@Override
	public void handleEvent(TransitDriverStartsEvent event) {
		// links vehicle to line, route, departure and driver
	}

	@Override
	public void handleEvent(final PersonEntersVehicleEvent event) {
		Set<Id<Person>> passengers = this.vehicleId2passengerIds.get(event.getVehicleId());
		if (this.debug && (passengers != null) && passengers.contains(event.getPersonId())) {
			throw new RuntimeException(
					"Vehicle " + event.getVehicleId() + " already contains person " + event.getPersonId() + ".");
		}
		if (passengers == null) {
			passengers = new LinkedHashSet<>();
			this.vehicleId2passengerIds.put(event.getVehicleId(), passengers);
		}
		passengers.add(event.getPersonId());
	}

	@Override
	public void handleEvent(final PersonLeavesVehicleEvent event) {
		Set<Id<Person>> passengers = this.vehicleId2passengerIds.get(event.getVehicleId());
		final boolean wasThere = passengers.remove(event.getPersonId());
		if (this.debug && !wasThere) {
			throw new RuntimeException(
					"Vehicle " + event.getVehicleId() + " did not contain passenger " + event.getPersonId() + ".");
		}
		if (passengers.size() == 0) {
			this.vehicleId2passengerIds.remove(event.getVehicleId());
		}
	}

	@Override
	public void handleEvent(final VehicleDepartsAtFacilityEvent event) {
		for (Id<Person> passengerId : vehicleId2passengerIds.getOrDefault(event.getVehicleId(), emptyPassengerSet)) {
			SpaceTimeIndicators<Tuple<Id<Vehicle>, Id<TransitStopFacility>>> indicators = this.passengerId2vehicleUsageIndicators
					.get(passengerId);
			if (indicators == null) {
				indicators = new SpaceTimeIndicators<>(this.timeDiscretization.getBinCnt());
				this.passengerId2vehicleUsageIndicators.put(passengerId, indicators);
			}
			indicators.visit(new Tuple<>(event.getVehicleId(), event.getFacilityId()),
					this.timeDiscretization.getBin(event.getTime()));
		}
	}
}
