/* *********************************************************************** *
 * project: org.matsim.*
 * PathGeneratorTest.java
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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.gbl.MatsimRandom;

public class PathGeneratorTest {

	@Test
	public void test() {
		Network network = NetworkGenerator.generateGrid(10, 10, 10.0);
		Random random = MatsimRandom.getRandom();
		random.setSeed(201802l);
		
		List<Link> path = PathGenerator.generateRandomPath(network, 1, random);
		Assert.assertEquals("Wrong number of links in path", 9, path.size());
		Assert.assertTrue("Wrong first link.", path.get(0).getId().equals(Id.createLinkId("6-1_5-1")));
		Assert.assertTrue("Wrong last link.", path.get(path.size()-1).getId().equals(Id.createLinkId("4-7_4-8")));
	}

}
