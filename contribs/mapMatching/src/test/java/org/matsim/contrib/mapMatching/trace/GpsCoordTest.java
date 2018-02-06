/* *********************************************************************** *
 * project: org.matsim.*
 * GpsCoordTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.mapMatching.experiment.NetworkGenerator;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.testcases.MatsimTestUtils;

public class GpsCoordTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test 
	public void testGetCoord() {
		Coord c = CoordUtils.createCoord(5.0, 1.0);

		Network network = NetworkGenerator.generateGrid(3, 3, 10.0);
		GpsCoord gWith = new GpsCoord(c.getX(), c.getY(), 0.123, network);
		assertEquals("Wrong coordinate", c, gWith.getCoord());

		GpsCoord gWithout = new GpsCoord(c.getX(), c.getY(), 0.123);
		assertEquals("Wrong coordinate", c, gWithout.getCoord());
	}
	
	@Test 
	public void testGetTime() {
		Coord c = CoordUtils.createCoord(5.0, 1.0);
		
		Network network = NetworkGenerator.generateGrid(3, 3, 10.0);
		GpsCoord gWith = new GpsCoord(c.getX(), c.getY(), 0.123, network);
		assertEquals("Wrong time", 0.123, gWith.getTime(), MatsimTestUtils.EPSILON);

		GpsCoord gWithout = new GpsCoord(c.getX(), c.getY(), 0.123);
		assertEquals("Wrong time", 0.123, gWithout.getTime(), MatsimTestUtils.EPSILON);
	}

	@Test
	public void testGetClosestLinkWithNetwork() {
		Coord c = CoordUtils.createCoord(5.0, 1.0);

		Network network = NetworkGenerator.generateGrid(3, 3, 10.0);
		GpsCoord g = new GpsCoord(c.getX(), c.getY(), 0.123, network);
		try {
			assertEquals("Wrong closest link", Id.createLinkId("0-0_1-0"), g.getClosestLinkId());
		} catch(Exception e) {
			fail("Should be able to get closest link");
		}
	}

	@Test
	public void testConstructorWithoutNetwork() {
		Coord c = CoordUtils.createCoord(5.0, 1.0);
		GpsCoord g = new GpsCoord(c.getX(), c.getY(), 0.123);
		try {
			g.getClosestLinkId();
			fail("Should throw exception if no closest link was associated in constructor");
		} catch(Exception e) {
			/* Should throw exception. */
		}
	}
	
	@Test
	public void testCandidateList() {
		Coord c = CoordUtils.createCoord(5.0, 1.0);
		
		Network network = NetworkGenerator.generateGrid(3, 3, 10.0);
		GpsCoord gWith = new GpsCoord(c.getX(), c.getY(), 0.123, network);
		assertNotNull("Candidate list should not be null", gWith.getCandidateLinks());
		assertEquals("Candidate list should be empty", 0, gWith.getCandidateLinks().size());
		
		GpsCoord gWithout = new GpsCoord(c.getX(), c.getY(), 0.123);
		assertNotNull("Candidate list should not be null", gWithout.getCandidateLinks());
		assertEquals("Candidate list should be empty", 0, gWithout.getCandidateLinks().size());
	}

}
