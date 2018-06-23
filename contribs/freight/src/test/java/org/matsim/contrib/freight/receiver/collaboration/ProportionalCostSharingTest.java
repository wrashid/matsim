/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
  
package org.matsim.contrib.freight.receiver.collaboration;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.receiver.FreightScenario;
import org.matsim.contrib.freight.receiver.Receiver;
import org.matsim.contrib.freight.usecases.receiver.ReceiverChessboardScenario;
import org.matsim.testcases.MatsimTestUtils;

public class ProportionalCostSharingTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	private FreightScenario fs;
	

	@Test
	public void test() {
		setup();
		
		Carrier carrier = fs.getCarriers().getCarriers().get(Id.create("Carrier1", Carrier.class));
		double carrierCost = carrier.getSelectedPlan().getScore();
		
		double pOne = 25.0/44.0;
		double pTwo = 1-pOne;
		
		ProportionalCostSharing pcs = new ProportionalCostSharing();
		pcs.allocateCoalitionCosts(fs);
		Id<Receiver> r1Id = Id.create("1", Receiver.class);
		Assert.assertEquals("Wrong cost allocated to receiver 1",
				pOne*carrierCost, 
				fs.getReceivers().getReceivers().get(r1Id).getSelectedPlan().getScore(), 
				MatsimTestUtils.EPSILON);
		
		Id<Receiver> r2Id = Id.create("2", Receiver.class);
		Assert.assertEquals("Wrong cost allocated to receiver 1",
				pTwo*carrierCost, 
				fs.getReceivers().getReceivers().get(r2Id).getSelectedPlan().getScore(), 
				MatsimTestUtils.EPSILON);
	}
	
	private void setup() {
		this.fs = ReceiverChessboardScenario.createChessboardScenario(1l, 1, false);
	}

}
