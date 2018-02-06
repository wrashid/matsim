/* *********************************************************************** *
 * project: org.matsim.contrib.mapMatching*
 * CandidateLink.java
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

public class CandidateLink {
	private Id<Link> linkId;
	private double distanceFromGPSpoint;
	private double observationProbability;
	
	public CandidateLink(Id<Link> linkId, double distanceFromGPSpoint, double standardDeviationGPSerror, double meanGPSerror ){
		this.linkId = linkId;
		this.distanceFromGPSpoint = distanceFromGPSpoint;
		double temp1 = 1/(Math.sqrt(2*Math.PI)*standardDeviationGPSerror);
		double temp2 = Math.pow(distanceFromGPSpoint-meanGPSerror,2)/(2*Math.pow(standardDeviationGPSerror, 2));
		double temp3 = Math.pow(Math.E,-temp2);
		double x = temp1 * temp3;
		observationProbability = 
				(1/
				(Math.sqrt(2*Math.PI)*standardDeviationGPSerror))
				*Math.pow(Math.E,
						-(Math.pow(distanceFromGPSpoint-meanGPSerror,2))/(2*Math.pow(standardDeviationGPSerror, 2))
						);
		// Ensuring correct formula usage
		//		(1 / ( (sqrt(2*pi)) x sigma) x 1 / e^( (x_i^j-mu)^2 / 2*(sigma^2) ) : one line formula
		// In formatted thing
		//		(1 / 
		//		( (sqrt(2*pi)) x sigma) 
		//		x 1 / e^( (x_i^j-mu)^2 / 2*(sigma^2) )
	}
	
	//Getters

	public Id<Link> getLinkId() {
		return linkId;
	}

	public double getDistanceFromGPSpoint() {
		return distanceFromGPSpoint;
	}

	public double getObservationProbability() {
		return observationProbability;
	}
	
}
