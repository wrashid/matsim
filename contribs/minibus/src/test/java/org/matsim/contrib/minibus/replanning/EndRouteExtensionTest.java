/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.replanning;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.minibus.PConstants;
import org.matsim.contrib.minibus.operator.Operator;
import org.matsim.contrib.minibus.operator.PPlan;
import org.matsim.contrib.minibus.replanning.EndRouteExtension;
import org.matsim.contrib.minibus.routeProvider.PScenarioHelper;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.util.ArrayList;


public class EndRouteExtensionTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	/**
	 * Can be deleted if option use2ndTerminusFromPPlan=true should become default and option use2ndTerminusFromPPlan=false is removed. gl 07-2018
	 */
	@Test
    public final void testRun() {
	
		Operator coop = PScenarioHelper.createCoop2111to2333();
		
		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();

		ArrayList<String> parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.0");
		parameter.add("true");
		
		EndRouteExtension strat = new EndRouteExtension(parameter);
		
		PPlan testPlan = null;
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start stop", "p_2111", coop.getBestPlan().getStopsToBeServedForwardDirection().get(0).getId().toString());
		Assert.assertEquals("Compare end stop", "p_2333", coop.getBestPlan().getStopsToBeServedReturnDirection().get(0).getId().toString());
		Assert.assertNull("Test plan should be null", testPlan);
		
		// buffer too small
		testPlan = strat.run(coop);
		
		Assert.assertNull("Test plan should be null", testPlan);
		
		parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.5");
		parameter.add("true");
		
		strat = new EndRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		// BEGIN TEST
		
		for(TransitStopFacility stop: testPlan.getStopsToBeServedForwardDirection()) {
			System.out.print(stop.getId().toString() + " ");
		}
		System.out.print("return: ");
		for(TransitStopFacility stop: testPlan.getStopsToBeServedReturnDirection()) {
			System.out.print(stop.getId().toString() + " ");
		}
		System.out.println();
		
		// END TEST
		
		Assert.assertNotNull("Test plan should not be null", testPlan);
		
		Assert.assertEquals("Compare start stop", "p_2111", testPlan.getStopsToBeServedForwardDirection().get(0).getId().toString());
		Assert.assertEquals("Compare former end stop", "p_2333", testPlan.getStopsToBeServedForwardDirection().get(1).getId().toString());
		Assert.assertEquals("Compare new end stop", "p_3343", testPlan.getStopsToBeServedReturnDirection().get(0).getId().toString());
		

		parameter = new ArrayList<>();
		parameter.add("2000.0");
		parameter.add("0.5");
		parameter.add("true");
		
		strat = new EndRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2111", testPlan.getStopsToBeServedForwardDirection().get(0).getId().toString());
		Assert.assertEquals("Compare former end stop", "p_2333", testPlan.getStopsToBeServedForwardDirection().get(1).getId().toString());
		Assert.assertEquals("Compare new end stop", "p_3334", testPlan.getStopsToBeServedReturnDirection().get(0).getId().toString());
		
		coop.getBestPlan().setStopsToBeServedForwardDirection(testPlan.getStopsToBeServedForwardDirection());
		coop.getBestPlan().setStopsToBeServedReturnDirection(testPlan.getStopsToBeServedReturnDirection());
		coop.getBestPlan().setLine(testPlan.getLine());
		
		testPlan = strat.run(coop);
		
		// remaining stops are covered now by the buffer of the otherwise wiggly route
		Assert.assertNull("Test plan should be null", testPlan);

	}
	
	@Test
    public final void testRunAddNewStopAlwaysBeforeFormerTerminus() {
	
		Operator coop = PScenarioHelper.createCoopRouteVShaped();
		
		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();

		ArrayList<String> parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.0");
		parameter.add("false");
		parameter.add("false");
		
		EndRouteExtension strat = new EndRouteExtension(parameter);
		
		PPlan testPlan = null;
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start stop", "p_2111", coop.getBestPlan().getStopsToBeServedForwardDirection().get(0).getId().toString());
		Assert.assertEquals("Compare middle stop", "p_3141", coop.getBestPlan().getStopsToBeServedForwardDirection().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3222", coop.getBestPlan().getStopsToBeServedReturnDirection().get(0).getId().toString());
		Assert.assertEquals("Compare middle stop", "p_3141", coop.getBestPlan().getStopsToBeServedReturnDirection().get(1).getId().toString());
		Assert.assertNull("Test plan should be null", testPlan);
		
		// buffer too small
		testPlan = strat.run(coop);
		
		Assert.assertNull("Test plan should be null", testPlan);
		
		parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.8");
		parameter.add("false");
		parameter.add("false");
		
		strat = new EndRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		Assert.assertNotNull("Test plan should not be null", testPlan);
		
		// BEGIN TEST
		
		for(TransitStopFacility stop: testPlan.getStopsToBeServedForwardDirection()) {
			System.out.print(stop.getId().toString() + " ");
		}
		System.out.print("return: ");
		for(TransitStopFacility stop: testPlan.getStopsToBeServedReturnDirection()) {
			System.out.print(stop.getId().toString() + " ");
		}
		System.out.println();
		
		// END TEST
		
		Assert.assertEquals("Compare start stop", "p_2111", testPlan.getStopsToBeServedForwardDirection().get(0).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServedForwardDirection().get(1).getId().toString());
		Assert.assertEquals("Compare former end stop", "p_2223", testPlan.getStopsToBeServedReturnDirection().get(0).getId().toString());
		Assert.assertEquals("Compare new end stop in between", "p_3222", testPlan.getStopsToBeServedReturnDirection().get(1).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServedReturnDirection().get(2).getId().toString());

		parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.8");
		parameter.add("false");
		parameter.add("false");
		
		strat = new EndRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2111", testPlan.getStopsToBeServedForwardDirection().get(0).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServedForwardDirection().get(1).getId().toString());
		Assert.assertEquals("Compare new end stop", "p_1323", testPlan.getStopsToBeServedReturnDirection().get(0).getId().toString());
		Assert.assertEquals("Compare former end stop", "p_3222", testPlan.getStopsToBeServedReturnDirection().get(1).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServedReturnDirection().get(2).getId().toString());
		
		parameter = new ArrayList<>();
		parameter.add("1500.0");
		parameter.add("0.8");
		parameter.add("false");
		parameter.add("false");
		
		strat = new EndRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2111", testPlan.getStopsToBeServedForwardDirection().get(0).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServedForwardDirection().get(1).getId().toString());
		Assert.assertEquals("Compare new end stop", "p_1413", testPlan.getStopsToBeServedReturnDirection().get(0).getId().toString());
		Assert.assertEquals("Compare former end stop", "p_3222", testPlan.getStopsToBeServedReturnDirection().get(1).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServedReturnDirection().get(2).getId().toString());
		
		coop.getBestPlan().setStopsToBeServedForwardDirection(testPlan.getStopsToBeServedForwardDirection());
		coop.getBestPlan().setStopsToBeServedReturnDirection(testPlan.getStopsToBeServedReturnDirection());
		coop.getBestPlan().setLine(testPlan.getLine());
		
		testPlan = strat.run(coop);
		
		// Adds stop 2414
		Assert.assertEquals("Compare new end stop", "p_2414", testPlan.getStopsToBeServedReturnDirection().get(0).getId().toString());
		
		coop.getBestPlan().setStopsToBeServedForwardDirection(testPlan.getStopsToBeServedForwardDirection());
		coop.getBestPlan().setStopsToBeServedReturnDirection(testPlan.getStopsToBeServedReturnDirection());
		coop.getBestPlan().setLine(testPlan.getLine());
		testPlan = strat.run(coop);
		
		// remaining stops are covered now by the buffer of the otherwise wiggly route
		Assert.assertNull("Test plan should be null", testPlan);

	}
	
	@Test
    public final void testRunUse2ndTerminusFromPPlan() {
	
		Operator coop = PScenarioHelper.createCoopRouteVShaped();
		
		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();

		ArrayList<String> parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.0");
		parameter.add("true");
		parameter.add("false");
		
		EndRouteExtension strat = new EndRouteExtension(parameter);
		
		PPlan testPlan = null;
		
		Assert.assertEquals("Compare number of vehicles", 1.0, coop.getBestPlan().getNVehicles(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Compare start stop", "p_2111", coop.getBestPlan().getStopsToBeServedForwardDirection().get(0).getId().toString());
		Assert.assertEquals("Compare middle stop", "p_3141", coop.getBestPlan().getStopsToBeServedForwardDirection().get(1).getId().toString());
		Assert.assertEquals("Compare end stop", "p_3222", coop.getBestPlan().getStopsToBeServedReturnDirection().get(0).getId().toString());
		Assert.assertEquals("Compare middle stop", "p_3141", coop.getBestPlan().getStopsToBeServedReturnDirection().get(1).getId().toString());
		Assert.assertNull("Test plan should be null", testPlan);
		
		// buffer too small
		testPlan = strat.run(coop);
		
		Assert.assertNull("Test plan should be null", testPlan);
		
		parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.8");
		parameter.add("true");
		parameter.add("false");
		
		strat = new EndRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		Assert.assertNotNull("Test plan should not be null", testPlan);
		
		// BEGIN TEST
		
		for(TransitStopFacility stop: testPlan.getStopsToBeServedForwardDirection()) {
			System.out.print(stop.getId().toString() + " ");
		}
		System.out.print("return: ");
		for(TransitStopFacility stop: testPlan.getStopsToBeServedReturnDirection()) {
			System.out.print(stop.getId().toString() + " ");
		}
		System.out.println();
		
		// END TEST
		
		Assert.assertEquals("Compare start stop", "p_2111", testPlan.getStopsToBeServedForwardDirection().get(0).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServedForwardDirection().get(1).getId().toString());
		Assert.assertEquals("Compare former end stop", "p_3222", testPlan.getStopsToBeServedForwardDirection().get(2).getId().toString());
		Assert.assertEquals("Compare new end stop", "p_2223", testPlan.getStopsToBeServedReturnDirection().get(0).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServedReturnDirection().get(1).getId().toString());
		
		parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.8");
		parameter.add("true");
		parameter.add("false");
		
		strat = new EndRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2111", testPlan.getStopsToBeServedForwardDirection().get(0).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServedForwardDirection().get(1).getId().toString());
		Assert.assertEquals("Compare former end stop", "p_3222", testPlan.getStopsToBeServedForwardDirection().get(2).getId().toString());
		Assert.assertEquals("Compare new end stop", "p_1323", testPlan.getStopsToBeServedReturnDirection().get(0).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServedReturnDirection().get(1).getId().toString());
		
		parameter = new ArrayList<>();
		parameter.add("1500.0");
		parameter.add("0.8");
		parameter.add("true");
		parameter.add("false");
		
		strat = new EndRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		Assert.assertNotNull("Test plan should not be null", testPlan);
		Assert.assertEquals("Compare start stop", "p_2111", testPlan.getStopsToBeServedForwardDirection().get(0).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServedForwardDirection().get(1).getId().toString());
		Assert.assertEquals("Compare former end stop", "p_3222", testPlan.getStopsToBeServedForwardDirection().get(2).getId().toString());
		Assert.assertEquals("Compare new end stop", "p_1413", testPlan.getStopsToBeServedReturnDirection().get(0).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServedReturnDirection().get(1).getId().toString());
		
		coop.getBestPlan().setStopsToBeServedForwardDirection(testPlan.getStopsToBeServedForwardDirection());
		coop.getBestPlan().setStopsToBeServedReturnDirection(testPlan.getStopsToBeServedReturnDirection());
		coop.getBestPlan().setLine(testPlan.getLine());
		
		testPlan = strat.run(coop);
		
		// Adds stop 2414
		Assert.assertEquals("Compare new end stop", "p_2414", testPlan.getStopsToBeServedReturnDirection().get(0).getId().toString());
		
		coop.getBestPlan().setStopsToBeServedForwardDirection(testPlan.getStopsToBeServedForwardDirection());
		coop.getBestPlan().setStopsToBeServedReturnDirection(testPlan.getStopsToBeServedReturnDirection());
		coop.getBestPlan().setLine(testPlan.getLine());
		testPlan = strat.run(coop);
		
		// remaining stops are covered now by the buffer of the otherwise wiggly route
		Assert.assertNull("Test plan should be null", testPlan);

	}
	
	@Test
    public final void testRunAddFormerTerminusOnWayBack() {
	
		// Add new stop at the end
		Operator coop = PScenarioHelper.createCoopRouteVShaped();
		
		new File(utils.getOutputDirectory() + PConstants.statsOutputFolder).mkdir();

		ArrayList<String> parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.8");
		parameter.add("false");
		parameter.add("true");
		
		EndRouteExtension strat = new EndRouteExtension(parameter);
		PPlan testPlan = null;
		
		strat = new EndRouteExtension(parameter);
		
		testPlan = strat.run(coop);
		
		Assert.assertNotNull("Test plan should not be null", testPlan);
		
		Assert.assertEquals("Compare start stop", "p_2111", testPlan.getStopsToBeServedForwardDirection().get(0).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServedForwardDirection().get(1).getId().toString());
		Assert.assertEquals("Compare former end stop", "p_3222", testPlan.getStopsToBeServedForwardDirection().get(2).getId().toString());
		Assert.assertEquals("Compare end stop", "p_1323", testPlan.getStopsToBeServedReturnDirection().get(0).getId().toString());
		Assert.assertEquals("Compare former end stop", "p_3222", testPlan.getStopsToBeServedReturnDirection().get(1).getId().toString());
		Assert.assertEquals("Compare most distant stop in between", "p_3141", testPlan.getStopsToBeServedReturnDirection().get(2).getId().toString());
		
		// Add new stop at the start
		coop = PScenarioHelper.createCoop2333to2111();
		parameter = new ArrayList<>();
		parameter.add("1000.0");
		parameter.add("0.5");
		parameter.add("false");
		parameter.add("true");
		
		strat = new EndRouteExtension(parameter);
		testPlan = strat.run(coop);
		
		Assert.assertNotNull("Test plan should not be null", testPlan);
		
		Assert.assertEquals("Compare end stop", "p_3343", testPlan.getStopsToBeServedForwardDirection().get(0).getId().toString());
		Assert.assertEquals("Compare former start stop", "p_2333", testPlan.getStopsToBeServedForwardDirection().get(1).getId().toString());
		Assert.assertEquals("Compare start stop", "p_2111", testPlan.getStopsToBeServedReturnDirection().get(0).getId().toString());
		Assert.assertEquals("Compare former start stop", "p_2333", testPlan.getStopsToBeServedReturnDirection().get(1).getId().toString());
	}
}

