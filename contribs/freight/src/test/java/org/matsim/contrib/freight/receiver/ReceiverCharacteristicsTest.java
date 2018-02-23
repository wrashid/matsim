/* *********************************************************************** *
 * project: org.matsim.*
 * ReceiverCharacteristicsTest.java
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

package org.matsim.contrib.freight.receiver;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;

public class ReceiverCharacteristicsTest {

	@Test
	public void testEmptyBuilder() {
		ReceiverCharacteristics.Builder builder = null;
		try {
			builder = ReceiverCharacteristics.Builder.newInstance();
		} catch(Exception e) {
			e.printStackTrace();
			Assert.fail("Builder must be created without an exception");
		}
		
		ReceiverCharacteristics rc = builder.build();
		
		/* This should be the defaults. */
		Assert.assertNull("Location should not be null", rc.getLocation());
		
		Assert.assertTrue("Products should be empty", rc.getReceiverProducts().isEmpty());
		
		
	}
	
	@Test
	public void testBuilderTimeWindow() {
		ReceiverCharacteristics rc = setup();
		
		Assert.assertEquals("Wrong start time", 
				Time.parseTime("08:00:00"), 
				rc.getTimeWindowStart(), 
				MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong end time", 
				Time.parseTime("12:00:00"), 
				rc.getTimeWindowEnd(), 
				MatsimTestUtils.EPSILON);
	}
	
	@Test
	public void testBuilderLink() {
		
	}
	

	/**
	 * Build a standard instance to use for tests.
	 * @return
	 */
	public ReceiverCharacteristics setup() {
		ReceiverCharacteristics.Builder builder = ReceiverCharacteristics.Builder.newInstance();
		builder.setLocation(Id.createLinkId("1"));
		builder.setTimeWindowStart(Time.parseTime("08:00:00"));
		builder.setTimeWindowEnd(Time.parseTime("12:00:00"));
		ReceiverCharacteristics rc = builder.build();
		
		return rc;
	}
	
	
	

}
