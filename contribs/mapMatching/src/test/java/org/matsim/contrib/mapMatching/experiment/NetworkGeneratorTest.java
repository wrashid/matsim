/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkGenerationTest.java
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

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.geometry.CoordUtils;

public class NetworkGeneratorTest {

	@Test
	/**
	 * Testing a small grid network.
	 *   Y
	 *   ^
	 *   |
	 * (0-2) <----> (1-2) <----> (2-2)
	 *   ^            ^            ^
	 *   |            |            |
	 *   |            |            |
	 *   v            v            v
	 * (0-1) <----> (1-1) <----> (2-1)
	 *   ^            ^            ^
	 *   |            |            |
	 *   |            |            |
	 *   v            v            v
	 * (0-0) <----> (1-0) <----> (2-0) ---> X
	 */
	public void testGridNetwork() {
		Network nw3 = NetworkGenerator.generateGrid(3, 3, 10);
		
		/* Check network size. */
		Assert.assertEquals("Wrong number of nodes",  9l, nw3.getNodes().size());
		Assert.assertEquals("Wrong number of links",  24l, nw3.getLinks().size());
		Node n12 = nw3.getNodes().get(Id.createNodeId("1-2"));
		Coord c12 = CoordUtils.createCoord(10.0, 20.0);
		Assert.assertEquals("Wrong coordinate", c12, n12.getCoord());
	}

}
