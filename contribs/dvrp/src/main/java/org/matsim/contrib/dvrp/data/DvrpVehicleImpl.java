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
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.core.mobsim.framework.DriverAgent;
import org.matsim.core.mobsim.framework.MobsimDriverAgent;
import org.matsim.core.mobsim.framework.PassengerAgent;
import org.matsim.vehicles.Vehicle;

/**
 * @author michalm
 */
public class DvrpVehicleImpl implements DvrpVehicle {
	private final Id<Vehicle> id;
	private Link startLink;
	private final int capacity;

	// time window
	private final double serviceBeginTime;
	private double serviceEndTime;

	private Schedule schedule;

	public DvrpVehicleImpl(Id<Vehicle> id, Link startLink, int capacity, double serviceBeginTime,
			double serviceEndTime) {
		this.id = id;
		this.startLink = startLink;
		this.capacity = capacity;
		this.serviceBeginTime = serviceBeginTime;
		this.serviceEndTime = serviceEndTime;

		schedule = new ScheduleImpl(this);
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
		
		//---------------------------------------------------

	}

	@Override
	public void setCurrentLink(Link link) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDriver(DriverAgent driver) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getLinkEnterTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setLinkEnterTime(double linkEnterTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public double getMaximumVelocity() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getFlowCapacityConsumptionInEquivalents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getEarliestLinkExitTime() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setEarliestLinkExitTime(double earliestLinkEndTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Vehicle getVehicle() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MobsimDriverAgent getDriver() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getSizeInEquivalents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public Link getCurrentLink() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean addPassenger(PassengerAgent passenger) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removePassenger(PassengerAgent passenger) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Collection<? extends PassengerAgent> getPassengers() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getPassengerCapacity() {
		// TODO Auto-generated method stub
		return 0;
	}
	
}
