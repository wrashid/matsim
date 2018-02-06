/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateScenarioTest.java
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

import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

public class GenerateScenarioIT {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;
	
	@Test
	public void testScenario() {
		/* Remove the output folder. */
		new File(utils.getOutputDirectory()).delete();
		
		String[] args = {
				"10", // Number of nodes in x-direction of grid
				"20", // Number of nodes in y-direction of grid;
				"50", // Link length in grid network;
				"10.0", // Frequency of GPS coordinates (expressed as time-between-coordinates);
				"15.0", // GPS error, expressed in the same units of measure as network;
				utils.getOutputDirectory() // Path where scenario is written to. 
		};
		try {
			GenerateScenario.run(args);
		} catch(Exception e) {
			e.printStackTrace();
			fail("Should not throw exceptions");
		}
		
		
	}

}
