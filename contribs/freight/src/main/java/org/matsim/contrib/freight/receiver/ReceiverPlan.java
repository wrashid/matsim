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
package org.matsim.contrib.freight.receiver;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * Like a natural {@link Person} a plan contains the intention of a {@link Receiver} 
 * agent.  In consequence, all information is <i>expected</i>. This container 
 * describes a {@link Receiver}'s behaviour in terms of how orders are placed 
 * with different {@link Carrier}s.
 * <p></p>
 * The only thing which is not "expected" in the same sense is the score.
 *  
 * @author jwjoubert
 */
public class ReceiverPlan implements BasicPlan, Attributable {
	private final Logger log = Logger.getLogger(ReceiverPlan.class);
	private Attributes attributes;
	private Receiver receiver = null;
	private Double score;
	private Map<Id<Carrier>, ReceiverOrder> orderMap;
	private boolean selected = false;
	
	private ReceiverPlan() {
		this.attributes = new Attributes();
		orderMap = new TreeMap<>();
	}
	
	
//	public void addReceiverOrder(final ReceiverOrder ro) {
//		if(orderMap.containsKey(ro.getCarrierId())) {
//			throw new IllegalArgumentException("Receiver '" + this.receiver.getId().toString() 
//					+ "' already has an order with carrier '" + ro.getCarrierId().toString() + "'");
//		}
//		orderMap.put(ro.getCarrierId(), ro);
//	}
	
	@Override
	public void setScore(final Double score) {
		this.score = score;
	}
	
	public Double getScore() {
		return this.score;
	}
	
	public final Receiver getReceiver() {
		return this.receiver;
	}
	
	public boolean isSelected() {
		return this.selected;
	}
	
	public void setSelected(boolean selected) {
		this.selected = selected;
	}
	
	/**
	 * Returns the {@link ReceiverOrder} for a given {@link Carrier}.
	 * @param carriedId
	 * @return
	 */
	public final ReceiverOrder getReceiverOrder(Id<Carrier> carriedId) {
		if(!orderMap.containsKey(carriedId)) {
			log.warn("Receiver '" + this.receiver.getId().toString() + 
					"' does not have an order with carrier '" + 
					carriedId.toString() + "'. Returning null");
			return null;
		}
		return this.orderMap.get(carriedId);
	}
	
	
	public String toString() {
		String scoreString = "undefined";
		if(this.score != null) {
			scoreString = this.score.toString();
		}
		
		String receiverString = "undefined";
		if(this.receiver != null) {
			receiverString = this.receiver.getId().toString();
		}
		
		return "[receiver: " + receiverString + "; score: " + scoreString + 
				"; number of orders with carriers: " + orderMap.size() + "]";
	}
	
	public final Collection<ReceiverOrder> getReceiverOrders(){
		return this.orderMap.values();
	}

	@Override
	public Attributes getAttributes() {
		return this.attributes;
	}
	
	public ReceiverPlan createCopy() {
		Builder builder = Builder.newInstance().setReceiver(receiver);
		for(ReceiverOrder ro : this.orderMap.values()) {
			builder = builder.addReceiverOrder(ro);
		}
		return builder.build();
	}

	
	/**
	 * The constructor mechanism for creating a {@link ReceiverPlan}. Once
	 * built the only thing one will be able to change is the score. 
	 *
	 * @author jwjoubert
	 */
	public static class Builder{
		private Receiver receiver = null;
		private Map<Id<Carrier>, ReceiverOrder> map = new HashMap<>();
		private boolean selected = false;
		private Double score = null;
		
		private Builder() {
		}
		
		public static Builder newInstance() {
			return new Builder();
		};
		
		public Builder setReceiver(Receiver receiver) {
			this.receiver = receiver;
			return this;
		}
		
		public Builder addReceiverOrder(ReceiverOrder ro) {
			this.map.put(ro.getCarrierId(), ro);
			return this;
		}
		
		public Builder setSelected(boolean selected) {
			this.selected = selected;
			return this;
		}
		
		public Builder setScore(double score) {
			this.score = score;
			return this;
		}
		
		public ReceiverPlan build() {
			ReceiverPlan plan = new ReceiverPlan();
			plan.receiver = this.receiver;
			plan.selected = this.selected;
			if(this.map.size() > 0) {
				plan.orderMap.putAll(this.map);			
			} else {
				plan.orderMap = this.map;
			}
			plan.score = this.score;
			return plan;
		}
	}

}
