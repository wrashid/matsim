/* *********************************************************************** *
 * project: org.matsim.*
 * MapMatcherTest.java
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

package org.matsim.contrib.mapMatching.run;


import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.testcases.MatsimTestUtils;

public class MapMatcherTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void testMatchingOne() {
		Network network = NetworkUtils.createNetwork();
		new MatsimNetworkReader(network).readFile(utils.getPackageInputDirectory() + "network.xml.gz");
		
		MapMatcher mm = new MapMatcher(network, 0.5, 1.0, 3, utils.getOutputDirectory());
		List<Link> mappedLinks = mm.mapGPStrajectory(utils.getPackageInputDirectory() + "trace.csv.gz");
		/* I would like the following to actually be a correct match, but it's not. */
		assertEquals("Wrong number of links", 12, mappedLinks.size());
	}

}
