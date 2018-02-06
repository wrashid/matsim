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


import org.junit.Rule;
import org.junit.Test;
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
		
		MapMatcher mm = new MapMatcher(network, 2.0, 5.0, 5, utils.getOutputDirectory());
		mm.mapGPStrajectory(utils.getPackageInputDirectory() + "trace.csv.gz");
	}

}
