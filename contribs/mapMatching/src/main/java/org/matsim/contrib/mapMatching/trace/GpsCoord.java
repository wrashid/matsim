/* *********************************************************************** *
 * project: org.matsim.contrib.mapMatching*
 * GPSpoint.java
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

package org.matsim.contrib.mapMatching.trace;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.mapMatching.graph.CandidateLink;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * A single Geospatial Positioning System (GPS) coordinate with a time stamp.
 *
 * @author jwjoubert, jbvosloo
 */
public class GpsCoord {
	
	private final Coord coord;
	private final Id<Link> closestLink;
	private final double time;
	private List<CandidateLink> candidateLinks;


	public GpsCoord(double xCoord, double yCoord, double time, Network network){
		this.coord = CoordUtils.createCoord(xCoord, yCoord);
		this.time = time;
		this.closestLink = NetworkUtils.getNearestLink(network, this.coord).getId();
		this.candidateLinks = new ArrayList<CandidateLink>();
	}
	
	
	public GpsCoord(double x, double y, double time) {
		this.coord = CoordUtils.createCoord(x, y);
		this.time = time;
		this.closestLink = null;
		this.candidateLinks = new ArrayList<CandidateLink>();
	}
	
	/**
	 * Location of the GPS trace coordinate.
	 * @return
	 */
	public Coord getCoord() {
		return coord;
	}

	/**
	 * Time (typically in seconds) of the GPS observation.
	 * @return
	 */
	public double getTime() {
		return time;
	}
	
	/**
	 * Closest link to point.
	 * 
	 * @return
	 * @throws RuntimeException if no network was passed when the class was constructed, and no closest
	 * 		   link could be established.
	 */
	public Id<Link> getClosestLinkId() {
		if(closestLink != null) {
			return closestLink;
		} else {
			throw new RuntimeException("Oops, there is now now link associated with this point.");
		}
	}

	
	public List<CandidateLink> getCandidateLinks() {
		return candidateLinks;
	}

	
	public void addCandidateLink(Id<Link> candidateLink, double distance, 
			double gpsErrorMean, double gpsErrorStandardDeviation) {
		this.candidateLinks.add(new CandidateLink(candidateLink, distance, gpsErrorStandardDeviation, gpsErrorMean));
	}

	
}
