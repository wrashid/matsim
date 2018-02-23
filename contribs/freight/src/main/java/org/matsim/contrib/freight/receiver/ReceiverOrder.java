package org.matsim.contrib.freight.receiver;

import java.util.Collection;

import org.matsim.api.core.v01.population.BasicPlan;

/**
 * A collection of all the orders of a receiver at a single supplier.
 * 
 * @author wlbean
 *
 */

public class ReceiverOrder implements BasicPlan {
	
	private final Receiver receiver;
	
	private final Collection<Order> orders;

	private Double cost = null;
	
	public ReceiverOrder(final Receiver receiver, final Collection<Order> orders){
		this.orders = orders;
		this.receiver = receiver;
		
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
	
}

