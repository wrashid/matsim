/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.dvrp.data;

import java.util.*;

import org.matsim.api.core.v01.Id;

/**
 * @author michalm
 */
public class FleetImpl implements Fleet {
	private final Map<Id<DvrpVehicle>, DvrpVehicle> vehicles = new LinkedHashMap<>();

	@Override
	public Map<Id<DvrpVehicle>, ? extends DvrpVehicle> getVehicles() {
		return Collections.unmodifiableMap(vehicles);
	}

	public void addVehicle(DvrpVehicle vehicle) {
		vehicles.put(vehicle.getId(), vehicle);
	}

	public void resetSchedules() {
		for (DvrpVehicle v : vehicles.values()) {
			v.resetSchedule();
		}
	}
}
