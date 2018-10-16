/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.spatialDrt.schedule;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.schedule.DrtStopTask;
import org.matsim.contrib.dvrp.data.Request;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.core.mobsim.framework.MobsimPassengerAgent;

/**
 * @author michalm
 */
public class AtodRequest extends DrtRequest {


	private String mode;

	public AtodRequest(Id<Request> id, MobsimPassengerAgent passenger, Link fromLink, Link toLink,
					   double earliestStartTime, double latestStartTime, double latestArrivalTime, double submissionTime,String mode) {
		super(id, passenger, fromLink, toLink, earliestStartTime, latestStartTime, latestArrivalTime, submissionTime);
		this.mode = mode;
	}



	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}


	@Override
	public String toString() {
		return Request.toString(this);
	}
}
