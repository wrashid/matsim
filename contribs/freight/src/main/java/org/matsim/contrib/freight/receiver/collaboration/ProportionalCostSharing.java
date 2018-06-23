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
  
/**
 * 
 */
package org.matsim.contrib.freight.receiver.collaboration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.receiver.FreightScenario;
import org.matsim.contrib.freight.receiver.Order;
import org.matsim.contrib.freight.receiver.Receiver;
import org.matsim.contrib.freight.receiver.ReceiverOrder;
import org.matsim.contrib.freight.receiver.ReceiverPlan;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * A proportional cost allocation between receivers and carriers.
 * 
 * Currently (Jun 2018) we only implement a version based on order quantity/volume. 
 * 
 * @author jwjoubert, wlbean
 */
public class ProportionalCostSharing implements ReceiverCarrierCollaboration {
	final private Logger log = Logger.getLogger(ProportionalCostSharing.class);
	private Attributes attributes;
	private String descr = "Proportional sharing of costs between carrier(s) and receiver(s)";
	
	
	
	
	public ProportionalCostSharing() {
		
	}
	

	@Override
	public Attributes getAttributes() {
		return this.attributes;
	}

	@Override
	public String getDescription() {
		return this.descr;
	}
	
	public void setDescription(String descr) {
		this.descr = descr;
	}


	@Override
	public FreightScenario allocateCoalitionCosts(FreightScenario scenario) {
		log.info("Performing proportional cost allocation based on volume.");
		
		/* Get all the the cross-referenced receiver-carriers. */
		log.info("   Cross-referencing all carrier-receiver relationships...");
		Map<Id<Carrier>, List<Id<Receiver>>> carrierCustomers = new HashMap<>();
		for(Receiver receiver : scenario.getReceivers().getReceivers().values()) {
			ReceiverPlan plan = receiver.getSelectedPlan();
			
			for(ReceiverOrder ro : plan.getReceiverOrders()) {
				Id<Carrier> carrierId = ro.getCarrierId();
				
				if(!carrierCustomers.containsKey(carrierId)) {
					carrierCustomers.put(carrierId, new ArrayList<>());
				}
				carrierCustomers.get(carrierId).add(receiver.getId());
			}
		}
		
		/* Calculate the proportional volume. */
		Map<Id<Carrier>, Map<Id<Receiver>, Double>> proportionalMap = new HashMap<>();
		log.info("   Calculating receivers' proportional volume from each carrier's perspective...");
		for(Id<Carrier> carriedId : carrierCustomers.keySet()) {
			double totalVolume = 0.0;
			/* Calculate this receiver's total volume with the carrier. */
			for(Id<Receiver> receiverId : carrierCustomers.get(carriedId)) {
				Receiver thisReceiver = scenario.getReceivers().getReceivers().get(receiverId);
				ReceiverOrder ro = thisReceiver.getSelectedPlan().getReceiverOrder(carriedId);
				totalVolume += getReceiverOrderTotal(ro);
			}
			
			/* Now calculate each receiver's proportion of the total volume. */ 
			for(Id<Receiver> receiverId : carrierCustomers.get(carriedId)) {
				Receiver thisReceiver = scenario.getReceivers().getReceivers().get(receiverId);
				
				double thisVolume = 0.0;
				ReceiverOrder ro = thisReceiver.getSelectedPlan().getReceiverOrder(carriedId);
				for(Order order : ro.getReceiverOrders()) {
					thisVolume += order.getOrderQuantity()*order.getProduct().getRequiredCapacity();
				}
				
				if(!proportionalMap.containsKey(carriedId)) {
					proportionalMap.put(carriedId, new HashMap<>());
				}
				
				/* The essence of the proportional assignment. */
				proportionalMap.get(carriedId).put(receiverId, thisVolume / totalVolume);
			}
		}

		/* Score the individual receiver plans. */
		log.info("  Scoring the individual receivers...");
		for(Receiver receiver : scenario.getReceivers().getReceivers().values()) {
			double total = 0.0;
			
			ReceiverPlan plan = receiver.getSelectedPlan();
			for(ReceiverOrder ro : plan.getReceiverOrders()) {
				double cost = ro.getCarrier().getSelectedPlan().getScore() * proportionalMap.get(ro.getCarrierId()).get(receiver.getId());
				ro.setScore(cost);
				total += cost;
			}
			plan.setScore(total);
		}
		
		log.info("Done with proportional cost calculation.");
		return scenario;
	}

	/**
	 * TODO Check that this is indeed correct. There seems to be two ways of 
	 * getting the 'requiredCapacity':<br><br>
	 * 
	 *  <code>order.getProduct().getRequiredCapacity(); </code>
	 *  <br>&nbsp&nbsp or<br>
	 *  <code>order.getProduct().getProductType().getRequiredCapacity(); </code>
	 */
	private double getReceiverOrderTotal(ReceiverOrder ro) {
		double total = 0.0;
		for(Order order : ro.getReceiverOrders()) {
			total += order.getOrderQuantity()*order.getProduct().getProductType().getRequiredCapacity();
		}
		return total;
	}
	
	
	
	
}
