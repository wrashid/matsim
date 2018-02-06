/* *********************************************************************** *
 * project: org.matsim.contrib.mapMatching*
 * PossibleCandidateLink.java
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

package org.matsim.contrib.mapMatching.graph;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class PossibleCandidateLink implements Comparable <PossibleCandidateLink> {
	private Id<Link> linkId;
	private double distanceFromGPSpoint;

	public PossibleCandidateLink(Id<Link> linkId, double distanceFromGPSpoint) {
		this.linkId = linkId;
		this.distanceFromGPSpoint = distanceFromGPSpoint;
	}
	
	
	@Override
	public int compareTo(PossibleCandidateLink o) {

		if (distanceFromGPSpoint < o.getDistanceFromGPSpoint()){
			return -1;
		}
		else if (distanceFromGPSpoint > o.getDistanceFromGPSpoint()){
			return 1;
		}
		else {
			return 0;
		}
	}

	public Id<Link> getLinkId() {
		return linkId;
	}


	public double getDistanceFromGPSpoint() {
		return distanceFromGPSpoint;
	}
	
}
