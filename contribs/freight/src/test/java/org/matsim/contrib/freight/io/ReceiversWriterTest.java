/* *********************************************************************** *
 * project: org.matsim.*
 * ReceiversWriterTest.java
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

package org.matsim.contrib.freight.io;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.receiver.Receivers;
import org.matsim.contrib.freight.usecases.receiver.ReceiverChessboardScenario;
import org.matsim.testcases.MatsimTestUtils;

public class ReceiversWriterTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testV1() {
		Scenario sc = ReceiverChessboardScenario.setupChessboardScenario(1l, 1);
		Receivers receivers = ReceiverChessboardScenario.createChessboardReceivers(sc);
		Carriers carriers = ReceiverChessboardScenario.createChessboardCarriers(sc);
		ReceiverChessboardScenario.createReceiverOrders(receivers, carriers);
		
		/* Now the receiver is 'complete', and we can write it to file. */
		try {
			new ReceiversWriter(receivers).writeV1(utils.getOutputDirectory() + "receivers.xml");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Should write without exception.");
		}
	}

}
