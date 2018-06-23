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
  
package org.matsim.contrib.freight.usecases.receiver;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.contrib.freight.receiver.FreightScenario;

public class ReceiverChessboardScenarioTest {

	@Test
	public void testCreateChessboardScenario() {
		
		FreightScenario fs = null;
		try {
			fs = ReceiverChessboardScenario.createChessboardScenario(1l, 1, false);
		} catch (Exception e) {
			Assert.fail("Should create the scenario without exceptions.");
		}
		
		/* TODO Test the various elements. */
	}

}
