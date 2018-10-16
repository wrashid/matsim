/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.spatialDrt.vehicle;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.file.ReaderUtils;
import org.matsim.contrib.spatialDrt.schedule.VehicleImpl;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.vehicles.VehicleCapacity;
import org.matsim.vehicles.VehicleCapacityImpl;
import org.matsim.vehicles.VehicleType;
import org.xml.sax.Attributes;
import org.matsim.contrib.dvrp.data.Vehicle;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

/**
 * @author michalm
 */
public class VehicleReader extends MatsimXmlParser {
	private static final String VEHICLE = "vehicle";

	private static final int DEFAULT_CAPACITY = 1;
	private static final double DEFAULT_T_0 = 0;
	private static final double DEFAULT_T_1 = 24 * 60 * 60;
	private static final double DEFAULT_BOARDING = 1.0;
	private static final double DEFAULT_ALIGHTING = 1.0;

	private FleetImpl fleet;
	private Map<Id<Link>, ? extends Link> links;
	private Map<String, VehicleType> vehicleTypes = new HashMap<>();

	public VehicleReader(Network network, FleetImpl fleet) {
		this.fleet = fleet;
		links = network.getLinks();

	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if (VEHICLE.equals(name)) {
			fleet.addVehicle(createVehicle(atts));
		}
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
	}

	private VehicleImpl createVehicle(Attributes atts) {
		Id<Vehicle> id = Id.create(atts.getValue("id"), Vehicle.class);
		Link startLink = links.get(Id.createLinkId(atts.getValue("start_link")));
		int capacity = ReaderUtils.getInt(atts, "capacity", DEFAULT_CAPACITY);
		double t0 = ReaderUtils.getDouble(atts, "t_0", DEFAULT_T_0);
		double t1 = ReaderUtils.getDouble(atts, "t_1", DEFAULT_T_1);
		String mode = ReaderUtils.getString(atts, "mode",null);
		String type = capacity + "V";
		if (!vehicleTypes.containsKey(type)){
			VehicleType vehicleType = new DynVehicleType();
			VehicleCapacity cap = new VehicleCapacityImpl();
			cap.setSeats(capacity);
			vehicleType.setCapacity(cap);
			if (vehicleType instanceof DynVehicleType) {
				if (capacity == 1){
					vehicleType.setLength(3.0);
					((DynVehicleType) vehicleType).setBatteryCapacity(20.0);
				}
				if (capacity == 4){
					vehicleType.setLength(5.0);
					((DynVehicleType) vehicleType).setBatteryCapacity(32.0);
				}
				if (capacity == 10){
					vehicleType.setLength(6.5);
					((DynVehicleType) vehicleType).setBatteryCapacity(60.0);
				}
				if (capacity == 20){
					vehicleType.setLength(9.0);
					((DynVehicleType) vehicleType).setBatteryCapacity(90.0);
				}
			}
			vehicleTypes.put(type, vehicleType);
		}
		VehicleType vehicleType = vehicleTypes.get(type);
		return createVehicle(id, startLink, capacity, t0, t1, mode, vehicleType);
	}

	protected VehicleImpl createVehicle(Id<Vehicle> id, Link startLink, int capacity, double t0, double t1,
			String mode, VehicleType vehicleType) {
		return new VehicleImpl(id, startLink, capacity, t0, t1, mode, vehicleType);
	}
}
