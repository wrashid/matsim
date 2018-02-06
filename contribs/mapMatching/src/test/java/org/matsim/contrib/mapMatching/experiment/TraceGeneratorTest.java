/* *********************************************************************** *
 * project: org.matsim.*
 * TraceGeneratorTest.java
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

package org.matsim.contrib.mapMatching.experiment;

import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.mapMatching.trace.GpsCoord;
import org.matsim.core.gbl.MatsimRandom;

public class TraceGeneratorTest {

	@Test
	public void testTraceGenerator() {
		double frequency = 5.0; // seconds
		double gpsError = 10.0; // meter
		Random random = MatsimRandom.getRandom();
		random.setSeed(201802l);
		
		Network network = NetworkGenerator.generateGrid(10, 20, 10);
		List<Link> links = PathGenerator.generateRandomPath(network, 1, random);
		List<GpsCoord> trace = TraceGenerator.generateGpsTrace(links, frequency, gpsError, random);
		
		Assert.assertEquals("Wrong number of trace coordinates", 33, trace.size());
		Assert.assertTrue("Wrong time for trace point 1", trace.get(0).getTime() == 5.0);
		Assert.assertTrue("Wrong time for trace point 2", trace.get(1).getTime() == 10.0);
	}

}
