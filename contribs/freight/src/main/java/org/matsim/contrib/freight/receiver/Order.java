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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.receiver.reorderPolicy.ReorderPolicy;
import org.matsim.core.utils.misc.Time;

/**
 * A concrete assignment of a receiver product, order quantity, delivery 
 * time windows, delivery service time and delivery location.
 * 
 * Default value for delivery service time is 1 hour and order quantity is 
 * 1000 units.
 * 
 * @author wlbean
 * 
 * FIXME Receiver is both at Order and ReceiverOrder level. Necessary?
 */

public class Order {
	final private Logger log = Logger.getLogger(Order.class);

	private Id<Order> orderId;
	private String orderName;
	private Receiver receiver;
	private ReceiverProduct receiverProduct;
	private Double orderQuantity;
	private Double serviceTime;
	
	
	/* protected */ 
	/*Order(Id<Order> orderId, String orderName, Receiver receiver, 
			ReceiverProduct receiverProduct, Double orderQuantity, Double serviceTime){
		this.orderId = orderId;
		this.orderName = orderName;
		this.receiver = receiver;
		this.receiverProduct = receiverProduct;
		this.orderQuantity = orderQuantity;
		this.serviceTime = serviceTime;
	}*/
	
	private Order(Builder builder){
		this.orderId = builder.orderId;
		this.orderName = builder.orderName;
		this.orderQuantity = builder.orderQuantity;
		this.receiver = builder.receiver;
		this.receiverProduct = builder.receiverProduct;
		this.serviceTime = builder.serviceTime;
	}


	/**
	 * Returns the receiver product being ordered.
	 */

	public ReceiverProduct getProduct(){
		return receiverProduct;
	}

	/**
	 * Returns the receiver of this order.
	 */

	public Receiver getReceiver(){
		return receiver;
	}

	/**
	 * Returns the order id.
	 */

	public Id<Order> getId(){
		return orderId;
	}

	/**
	 * Returns the delivery service duration for a particular receiver order.
	 * @return
	 */
	public double getServiceDuration(){
		if(this.serviceTime == null) {
			log.warn("No service time set. Returning default of 00:01:00");
			return Time.parseTime("00:01:00");
		}
		return serviceTime;
	}

	/**
	 * Returns the order name.
	 * @return
	 */

	public String getOrderName(){
		return orderName;
	}

	/**
	 * Returns the order quantity.
	 * @return
	 */

	public int getOrderQuantity(){
		return (int) Math.round(orderQuantity);
	}

	/**
	 * Returns a single carrier service containing the order information.
	 * 
	 * FIXME I (jwj) removed the time window since it is no longer a single 
	 * value, but rather a list of time windows.
	 */
	@Override
	public String toString(){
		return "[id=" + orderId + "][linkId=" + receiver.getLinkId() + "][capacityDemand=" + orderQuantity + "][serviceDuration=" + Time.writeTime(serviceTime) + "]";
	}
	
	public Order createCopy() {
		return Order.Builder.newInstance(orderId, receiver, receiverProduct)
				.setOrderQuantity(orderQuantity)
				.setServiceTime(getServiceDuration())
				.build();
	}

	/**
	 * A builder building a receiver order for one product type.
	 * 
	 * @author wlbean
	 *
	 */
	
	public static class Builder {
	
	
		/**
		 * Returns a new instance of a receiver order.
		 */
	
		 public static Builder newInstance(Id<Order> orderId, Receiver receiver, ReceiverProduct receiverProduct){
			return new Builder(orderId, receiver, receiverProduct);
		}
		
		//public static Builder newInstance(){
		//	return new Builder();
		//}
	
		private Receiver receiver;
		private Id<Order> orderId;
		private ReceiverProduct receiverProduct;
		private Double serviceTime = null;
		private String orderName = "service";
		private Double orderQuantity = null;
	
	
		private Builder(final Id<Order> orderId, final Receiver receiver, final ReceiverProduct receiverProduct){
			this.orderId = orderId;
			this.receiver = receiver;
			this.receiverProduct = receiverProduct;
		}
	
		//private Builder(){
		//}
		
		/**
		 * Sets the delivery service time.
		 * @param serviceTime
		 * @return
		 */
		public Builder setServiceTime(double serviceTime){
			this.serviceTime = serviceTime;
			return this;
		}
		
		
		public Builder setOrderQuantity(Double quantity) {
			this.orderQuantity = quantity;
			return this;
		}
		
		//public Builder setOrderName(String name) {
		//	this.orderName = name;
		//	return this;
		//}
		
	
		/**
		 * Determines the order quantity (in kg) based on the receivers min and max inventory levels (in units) and the capacity demand per unit (in kg) and create a new order.
		 * 
		 * This should be expanded later on when including demand rate for products.
		 * @param receiverProduct
		 * @return
		 */
		public Builder calculateOrderQuantity(){
			if(this.receiverProduct == null) {
				throw new RuntimeException("Cannot calculate order quantity before ProductType is set.");
			}
			
			ReorderPolicy policy = this.receiverProduct.getReorderPolicy();
			double orderQuantity = policy.calculateOrderQuantity(receiverProduct.getStockOnHand());
			//			int minLevel = receiverProduct.getMinLevel();
			//			int maxLevel = receiverProduct.getMaxLevel();
			//			double orderQuantity = (maxLevel - minLevel)*receiverProduct.getRequiredCapacity();
			this.orderQuantity = orderQuantity;
			return this;
		}
	
		
		public Order build(){
			return new Order(this);
		}
		/*public Order build(){
			return new Order(orderId, orderName, receiver, receiverProduct, orderQuantity, serviceTime);
		}
		*/

		
		public Order buildWithCalculatedOrderQuantity() {
			this.calculateOrderQuantity();
			return build();
		}
	}

}
