/* *********************************************************************** *
 * project: org.matsim.*
 * MapMatchingUtilsTest.java
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

package org.matsim.contrib.mapMatching.utils;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.mapMatching.experiment.NetworkGenerator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.testcases.MatsimTestUtils;

public class MapMatchingUtilsTest {

	
	@Test
	public void testCreateQuadTree() {
		Network network = NetworkGenerator.generateGrid(10, 10, 100);
		QuadTree<Id<Node>> qt = MapMatchingUtils.createQuadTree(network);
		Assert.assertEquals("Wrong number of nodes.", 100, qt.size());
		
		/* Check random node. */
		Id<Node> node = qt.getClosest(310.0, 310.0);
		Assert.assertTrue("Wrong node closest to (310, 310).", node.equals(Id.createNodeId("3-3")));
	}
	
	@Test
	public void testGetPath() {
		Network network = NetworkGenerator.generateGrid(10, 10, 100);
		Node nodeFrom = network.getNodes().get(Id.createNodeId("0-0"));
		Node nodeTo = network.getNodes().get(Id.createNodeId("0-3"));
		
		Path path = null;
		try{
			path = MapMatchingUtils.getPath(network, nodeFrom, nodeTo, true); 
		} catch(Exception e) {
			e.printStackTrace();
			Assert.fail("Should not throw exceptions when calculating shortest path.");
		}
		Assert.assertEquals("Wrong number of links in shortest path.", 3, path.links.size());
		Assert.assertEquals("Wrong path length.", 300.0, path.travelCost, MatsimTestUtils.EPSILON);
	}

}
