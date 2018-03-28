/**
 * 
 */
package org.matsim.contrib.freight.receiver;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.Time;

/**
 * A concrete assignment of a receiver product, order quantity, delivery time windows, delivery service time and delivery location.
 * 
 * Default value for delivery service time is 1 hour and order quantity is 1000 units.
 * 
 * @author wlbean
 *
 */

public class Order {

	public static Order newInstance(Id<Order> orderId, Receiver receiver, ReceiverProduct receiverProduct, double serviceTime){
		return new Order(orderId, receiver, receiverProduct, serviceTime);		
	}
	
	public Order(Id<Order> orderId, Receiver receiver, ReceiverProduct receiverProduct, double serviceTime){
		super();
		this.orderId = orderId;
		this.receiver = receiver;
		this.receiverProduct = receiverProduct;
		this.orderName = "service";
		this.serviceTime = 1.0;
		this.orderQuantity = 1000;
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
		
		public static Builder newInstance(Id<Order> orderId, Receiver receiver, ReceiverProduct receiverProduct, double serviceTime){
			return new Builder(orderId, receiver, receiverProduct, serviceTime);
		}
		
		private ReceiverProduct receiverProduct;
		private double serviceTime = 0.5;
		private Receiver receiver;
		private String orderName = "service";
		private Id<Order> orderId;
		
			
		private Builder(Id<Order> orderId, Receiver receiver, ReceiverProduct receiverProduct, double serviceTime){
			super();
			this.orderId = orderId;
			this.receiver = receiver;
			this.receiverProduct = receiverProduct;
			this.serviceTime = serviceTime;
		}

		
		/**
		 * Sets the delivery service time.
		 * @param serviceTime
		 * @return
		 */
		
		public Builder setServiceTime(double serviceTime){
			this.serviceTime = serviceTime;
			return this;
		}
		
		/**
		 * Sets the order Id.
		 * @param orderId
		 * @return
		 */
		
		public Builder setOrderId(Id<Order> orderId){
			this.orderId = orderId;
			return this;
		}
		/**
		 * Determines the order quantity (in kg) based on the receivers min and max inventory levels (in units) and the capacity demand per unit (in kg) and create a new order.
		 * 
		 * This should be expanded later on when including demand rate for products.
		 * @param receiverProduct
		 * @return
		 */
		
		public double calculateOrderQuantity(ReceiverProduct receiverProduct){
			int minLevel = receiverProduct.getMinLevel();
			int maxLevel = receiverProduct.getMaxLevel();
			double orderQuantity = (maxLevel - minLevel)*receiverProduct.getRequiredCapacity();
			return orderQuantity;
		}
		
				
		
		public Order build(){
			return new Order(this);
		}
	}
	
	private final ReceiverProduct receiverProduct;
	private final double orderQuantity;
	private final double serviceTime;
	private final Receiver receiver;
	private final String orderName;
	private final Id<Order> orderId;
	
	private Order(Builder builder){
		this.serviceTime = builder.serviceTime;
		this.receiverProduct = builder.receiverProduct;
		this.receiver = builder.receiver;
		this.orderName = builder.orderName;
		this.orderId = builder.orderId;
		this.orderQuantity = builder.calculateOrderQuantity(receiverProduct);
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
	
	public Id<Order> GetId(){
		return orderId;
	}
	
	/**
	 * Returns the delivery service duration for a particular receiver order.
	 * @return
	 */
	
	public double getServiceDuration(){
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
	 * Returns the receiver order time window start time.
	 * @return
	 */
	
	public double getOrderTimeWindowStart(){
		return receiver.getReceiverCharacteristics().getTimeWindowStart();
	}
	
	/**
	 * Returns the receiver order time window end time.
	 * @return
	 */
	
	public double getOrderTimeWindowEnd(){
		return receiver.getReceiverCharacteristics().getTimeWindowEnd();
	}
	
	/**
	 * Returns the order delivery location.
	 * @return
	 */
	
	public Id<Link> getOrderDeliveryLocation(){
		return receiver.getReceiverCharacteristics().getLocation();
	}
	
	/**
	 * Returns a single carrier service containing the order information.
	 */
	@Override
	public String toString(){
		return "[id=" + orderId + "][locationId=" + receiver.getReceiverCharacteristics().getLocation() + "][capacityDemand=" + orderQuantity + "][serviceDuration=" + Time.writeTime(serviceTime) + "][earliestStart="+ Time.writeTime(receiver.getReceiverCharacteristics().getTimeWindowStart()) + "][latestEnd="+ Time.writeTime(receiver.getReceiverCharacteristics().getTimeWindowEnd()) + "]";
	}

}
