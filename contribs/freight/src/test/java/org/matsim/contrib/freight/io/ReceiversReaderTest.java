/* *********************************************************************** *
 * project: org.matsim.*
 * ReceiversReaderTest.java
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
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.receiver.Order;
import org.matsim.contrib.freight.receiver.ProductType;
import org.matsim.contrib.freight.receiver.Receiver;
import org.matsim.contrib.freight.receiver.ReceiverOrder;
import org.matsim.contrib.freight.receiver.ReceiverProduct;
import org.matsim.contrib.freight.receiver.Receivers;
import org.matsim.contrib.freight.receiver.reorderPolicy.ReorderPolicy;
import org.matsim.contrib.freight.receiver.reorderPolicy.SSReorderPolicy;
import org.matsim.core.utils.misc.Time;
import org.matsim.testcases.MatsimTestUtils;

public class ReceiversReaderTest {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testV1() {
		Receivers receivers = new Receivers();
		try {
		new ReceiversReader(receivers).readFile(utils.getClassInputDirectory() + "receivers.xml");
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Should read without exception.");
		}
		/* Receivers */
		Assert.assertTrue("Wrong description.", "Chessboard".equalsIgnoreCase(receivers.getDescription()));
		Object r = receivers.getAttributes().getAttribute("date");
		Assert.assertNotNull("No attribute", r);
		
		/* Product types */
		Assert.assertEquals("Wrong number of product types.", 2l, receivers.getAllProductTypes().size());
		ProductType pt1 = receivers.getProductType(Id.create("P1", ProductType.class));
		Assert.assertNotNull("Could not find ProductType \"P1\"", pt1);
		Assert.assertNotNull("Could not find ProductType \"P2\"", receivers.getProductType(Id.create("P2", ProductType.class)));
		
		Assert.assertTrue("Wrong ProductType description", pt1.getDescription().equalsIgnoreCase("Product 1"));
		Assert.assertEquals("Wrong capacity.", 5.0, pt1.getRequiredCapacity(), MatsimTestUtils.EPSILON);
		
		/* Receiver */
		Assert.assertEquals("Wrong number of receivers.", 2, receivers.getReceivers().size());
		Receiver r1 = receivers.getReceivers().get(Id.create("1", Receiver.class));
		Assert.assertNotNull("Should find receiver \"1\"", r1);
		/*TODO need to test receiver attributes. */
		
		/* Time window */
		Assert.assertEquals("Wrong number of time windows.", 1, r1.getTimeWindows().size());
		TimeWindow t1 = r1.getTimeWindows().get(0);
		Assert.assertEquals("Wrong time window start time.", Time.parseTime("10:00:00"), t1.getStart(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong time window end time.", Time.parseTime("14:00:00"), t1.getEnd(), MatsimTestUtils.EPSILON);
		Assert.assertEquals("Wrong number of products.", 2, r1.getProducts().size());
		
		/* Receiver product */
		ReceiverProduct rp1 = r1.getProduct(Id.create("P1", ProductType.class));
		Assert.assertNotNull("Could not find receiver product \"P1\"", rp1);
		Assert.assertEquals("Wrong stock on hand.", 0.0, rp1.getStockOnHand(), MatsimTestUtils.EPSILON);
		
		/* Reorder policy */
		ReorderPolicy policy = rp1.getReorderPolicy();
		Assert.assertNotNull("Could not find reorder policy.", policy);
		Assert.assertTrue("Wrong policy type", policy instanceof SSReorderPolicy);
		Assert.assertTrue("Wrong policy name.", policy.getPolicyName().equalsIgnoreCase("(s,S)"));
		Assert.assertNotNull("Could not find attribute \"s\"", policy.getAttributes().getAttribute("s"));
		Assert.assertNotNull("Could not find attribute \"S\"", policy.getAttributes().getAttribute("S"));
		
		/* (Receiver)Orders */
		Assert.assertEquals("Wrong number of orders/plans", 1, r1.getPlans().size());
		ReceiverOrder ro = r1.getPlans().get(0);
		Assert.assertTrue("Wrong carrier", ro.getCarrierId().equals(Id.create("Carrier1", Carrier.class)));
		Assert.assertTrue("Wrong receiver", ro.getReceiverId().equals(Id.create("1", Receiver.class)));
		Assert.assertNull("Should not have score now", ro.getScore());
		
		/* Order (items) */
		Assert.assertEquals("Wrong number of order (items)", 2, ro.getReceiverOrders().size());
		Order o = ro.getReceiverOrders().iterator().next();
		Assert.assertTrue("Wrong order id.", o.getId().equals(Id.create("Order1", Order.class)));
		Assert.assertTrue("Wrong order name.", o.getOrderName().equalsIgnoreCase("service"));
		Assert.assertEquals("Wrong order quantity.", 1000, o.getOrderQuantity());
		Assert.assertTrue("Wrong product type.", o.getProduct().getProductType().getId().equals(Id.create("P1", ProductType.class)));
		Assert.assertTrue("Wrong receiver.", o.getReceiver().getId().equals(Id.create("1", Receiver.class)));
		Assert.assertEquals("Wrong service duration.", Time.parseTime("00:30:00"), o.getServiceDuration(), MatsimTestUtils.EPSILON);
	}

}
