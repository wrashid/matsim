/* *********************************************************************** *
 * project: org.matsim.*
 * AllTests.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.utils;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for org.matsim.utils");
		//$JUnit-BEGIN$
		suite.addTest(org.matsim.utils.charts.AllTests.suite());
		suite.addTest(org.matsim.utils.collections.AllTests.suite());
		suite.addTest(org.matsim.utils.geometry.AllTests.suite());
		suite.addTest(org.matsim.utils.misc.AllTests.suite());
		suite.addTest(org.matsim.utils.vis.routervis.AllTests.suite());
		suite.addTest(org.matsim.utils.vis.snapshots.AllTests.suite());
		suite.addTestSuite(WorldUtilsTest.class);
		//$JUnit-END$
		return suite;
	}

}
