package org.matsim.contrib.freight.receiver;

import java.util.Collection;

import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.contrib.freight.carrier.Carrier;

/**
 * A collection of all the orders of a receiver delivered by a single carrier.
 * 
 * @author wlbean
 *
 */

public class ReceiverOrder implements BasicPlan {
	
	private final Receiver receiver;
	
	private final Collection<Order> orders;

	private Double cost = null;
	
	private Carrier carrier ;
	
	public ReceiverOrder(final Receiver receiver, final Collection<Order> orders, final Carrier carrier){
		this.orders = orders;
		this.receiver = receiver;
		this.carrier = carrier;		
	}
	
	public Receiver getReceiver(){
		return receiver;
	}
	
	
	@Override
	public Double getScore() {
		return cost;
	}

	@Override
	public void setScore(Double cost) {
	this.cost  = cost;
	}
	
	public Collection<Order> getReceiverOrders(){
		return orders;
	}
	
	public void setOrderCarrier(Carrier carrier){
		this.carrier = carrier;
	}
	
	public Carrier getOrderCarrier(){
		return this.carrier;
	}
	
}

