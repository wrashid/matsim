/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.data.DrtRequest;


import java.util.*;

/**
 * @author michalm
 */
public class DrtStopTask extends org.matsim.contrib.drt.schedule.DrtStopTask {
	private final List<DrtRequest> dropoffRequests = new ArrayList<>();
	private final List<DrtRequest> pickupRequests = new ArrayList<>();

	public DrtStopTask(double beginTime, double endTime, Link link) {
		super(beginTime, endTime, link);
	}

	@Override
	public DrtTaskType getDrtTaskType() {
		return DrtTaskType.STOP;
	}



	public List<DrtRequest> getDrtDropoffRequests() {
		return dropoffRequests;
	}

	public List<DrtRequest> getDrtPickupRequests() {
		return pickupRequests;
	}

	public void addDropoffRequest(DrtRequest request) {
		dropoffRequests.add(request);
		super.addDropoffRequest(request);
	}

	public void addPickupRequest(DrtRequest request) {
		pickupRequests.add(request);
		super.addPickupRequest(request);
	}

	@Override
	protected String commonToString() {
		return "[" + getDrtTaskType().name() + "]" + super.commonToString();
	}
}
