/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.jdeqsim;

import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.core.mobsim.qsim.agents.ActivityDurationUtils;

/**
 * The basic EventMessage type.
 *
 * @author rashid_waraich
 */
public abstract class EventMessage extends Message {
	public Vehicle vehicle;
	public Scheduler scheduler;

	public EventMessage(Scheduler scheduler, Vehicle vehicle) {
		super();
		this.vehicle = vehicle;
		this.scheduler = scheduler;
	}

	public void resetMessage(Scheduler scheduler, Vehicle vehicle) {
		this.scheduler = scheduler;
		this.vehicle = vehicle;
	}

	public void handleAbort() {
		// Agents/vehicles have no concept of "being in an activity": need to check based on plan
		final boolean traveling = ActivityDurationUtils.calculateDepartureTime(
				vehicle.getPreviousActivity(),
				scheduler.getSimTime(),
				vehicle.getActivityEndTimeInterpretation()) <= scheduler.getSimTime();

		eventsManager.processEvent(new PersonStuckEvent(
				scheduler.getSimTime(),
				vehicle.getOwnerPerson().getId(),
				traveling ? vehicle.getCurrentLinkId() : null,
				traveling ? vehicle.getCurrentLeg().getMode() : null));
	}
}
