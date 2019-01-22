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

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.run.AbstractDvrpModeQSimModule;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.core.mobsim.qsim.qnetsimengine.QVehicleImpl;
import org.matsim.vehicles.Vehicle;
import org.matsim.vehicles.VehicleImpl;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleUtils;

/**
 * @author michalm
 */
public class DvrpVehicleImpl implements DvrpVehicle {
	private final Id<Vehicle> id;
	private Link startLink;
	private final int capacity;

	QVehicleImpl delegate;
	
	// time window
	private final double serviceBeginTime;
	private double serviceEndTime;

	private Schedule schedule;

	public DvrpVehicleImpl(Id<Vehicle> id, Link startLink, int capacity, double serviceBeginTime,
			double serviceEndTime, VehicleType vehicleType) {
		this.id = id;
		this.startLink = startLink;
		this.capacity = capacity;
		this.serviceBeginTime = serviceBeginTime;
		this.serviceEndTime = serviceEndTime;

		schedule = new ScheduleImpl(this);
		
		/* i am not sure what vehicle type the delegate should be assigned to. As a temporary solution, the default could get assigned.
		 * Or should the basic VehicleImpl rather be a parameter in the DvrpVehicleImpl constructor?
		 * tschlenther jan' 19
		 * 
		 * until now, the VrpAgentSource was inserting QVehicles of a vehicleType that had to be bound beforehand with annotation VrpAgentSourceQSimModule.DVRP_VEHICLE_TYPE
		 * so i guess, we can assume that the vehicleType is given as a parameter.
		 * maybe give two constructors, one with a VehicleType parameter, one without.
		 * tschlenther jan' 19
		 */
		delegate = new QVehicleImpl(new VehicleImpl(id, vehicleType));
	
	}
	
	public DvrpVehicleImpl(Id<Vehicle> id, Link startLink, int capacity, double serviceBeginTime,
			double serviceEndTime) {
		this(id,startLink, capacity, serviceBeginTime,
			serviceEndTime, VehicleUtils.getDefaultVehicleType());
	}

	@Override
	public Id<Vehicle> getId() {
		return id;
	}

	@Override
	public Link getStartLink() {
		return startLink;
	}

	@Override
	public void setStartLink(Link link) {
		this.startLink = link;
	}

	@Override
	public int getCapacity() {
		return capacity;
	}

	@Override
	public double getServiceBeginTime() {
		return serviceBeginTime;
	}

	@Override
	public double getServiceEndTime() {
		return serviceEndTime;
	}

	@Override
	public Schedule getSchedule() {
		return schedule;
	}

	@Override
	public String toString() {
		return "Vehicle_" + id;
	}

	public void setServiceEndTime(double serviceEndTime) {
		this.serviceEndTime = serviceEndTime;
	}

	@Override
	public void resetSchedule() {
		schedule = new ScheduleImpl(this);
	}
	
	//--------------------------  DELEGATE METHODS  -------------------------

	public double getEarliestLinkExitTime() {
		return delegate.getEarliestLinkExitTime();
	}

	public void setEarliestLinkExitTime(double earliestLinkEndTime) {
		delegate.setEarliestLinkExitTime(earliestLinkEndTime);
	}

	public void setCurrentLink(Link link) {
		delegate.setCurrentLink(link);
	}

	public Vehicle getVehicle() {
		return delegate.getVehicle();
	}

	public void setDriver(DriverAgent driver) {
		delegate.setDriver(driver);
	}

	public double getLinkEnterTime() {
		return delegate.getLinkEnterTime();
	}

	public void setLinkEnterTime(double linkEnterTime) {
		delegate.setLinkEnterTime(linkEnterTime);
	}

	public MobsimDriverAgent getDriver() {
		return delegate.getDriver();
	}

	public double getMaximumVelocity() {
		return delegate.getMaximumVelocity();
	}

	public double getSizeInEquivalents() {
		return delegate.getSizeInEquivalents();
	}

	public double getFlowCapacityConsumptionInEquivalents() {
		return delegate.getFlowCapacityConsumptionInEquivalents();
	}

	public Link getCurrentLink() {
		return delegate.getCurrentLink();
	}

	public boolean addPassenger(PassengerAgent passenger) {
		return delegate.addPassenger(passenger);
	}

	public boolean removePassenger(PassengerAgent passenger) {
		return delegate.removePassenger(passenger);
	}

	public Collection<? extends PassengerAgent> getPassengers() {
		return delegate.getPassengers();
	}

	public int getPassengerCapacity() {
		return delegate.getPassengerCapacity();
	}
}
