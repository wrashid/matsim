package org.matsim.contrib.freight.receiver;

import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.contrib.freight.carrier.Carrier;

/**
 * A collection of all the orders of a receiver delivered by a single carrier.
 * 
 * @author wlbean, jwjoubert
 */

public class ReceiverOrder implements BasicPlan {
	
	private final Logger log = Logger.getLogger(ReceiverOrder.class);
	private final Id<Receiver> receiverId;
	private final Collection<Order> orders;
	private Double cost = null;
	private final Id<Carrier> carrierId;
	private Carrier carrier = null;

	
	public ReceiverOrder(final Id<Receiver> receiverId, final Collection<Order> orders, final Id<Carrier> carrierId){
		this.orders = orders;
		this.receiverId = receiverId;
		this.carrierId = carrierId;		
	}
	
	/**
	 * Get the back pointer to this {@link ReceiverOrder}'s {@link Receiver}.
	 * @return
	 */
	public Id<Receiver> getReceiverId(){
		return receiverId;
	}
	
	@Override
	public Double getScore() {
		if(cost == null) {
			log.warn("The cost/score has not been set yet. Returning null.");
		}
		return cost;
	}

	@Override
	public void setScore(Double cost) {
	this.cost  = cost;
	}
	
	public Collection<Order> getReceiverOrders(){
		return this.orders;
	}
	
	
	/**
	 * Get the actual {@link Carrier} of this {@link ReceiverOrder}. This will
	 * only be set once FIXME ... has been called to link the receivers and
	 * carriers. 
	 * 
	 * @return
	 */
	public Carrier getCarrier() {
		if(this.carrier == null) {
			log.error("The carriers have not been linked to the receivers yet. Returning null.");
		}
		return this.carrier;
	}
	
	/**
	 * Get the pointer {@link Id} of this {@link ReceiverOrder}'s {@link Carrier}.
	 * 
	 * @return
	 */
	public Id<Carrier> getCarrierId(){
		return this.carrierId;
	}
	
	public ReceiverOrder createCopy() {
		ReceiverOrder newOrder = new ReceiverOrder(receiverId, orders, carrierId);
		newOrder.setScore(cost == null ? null : Double.valueOf(cost));
		return newOrder;
	}
	
	public void setCarrier(final Carrier carrier) {
		this.carrier = carrier;
	}
	
}

