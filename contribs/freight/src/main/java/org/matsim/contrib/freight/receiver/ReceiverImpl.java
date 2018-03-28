/**
 * 
 */
package org.matsim.contrib.freight.receiver;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;

/**
 * This returns a receiver that has characteristics and orders.
 * 
 * @author wlbean
 *
 */
public class ReceiverImpl implements Receiver {

	/*
	 * Create a new instance of a receiver.
	 */
	
	public static Receiver newInstance(Id<Receiver> id){
		return new ReceiverImpl(id);
	}
	
	private final Id<Receiver> id;
	private final List<ReceiverOrder> orders;
	private final List<ReceiverProduct> products;
	private ReceiverCharacteristics receiverCharacteristics;
	private ReceiverOrder selectedOrder;
	
	private ReceiverImpl(final Id<Receiver> id){
		super();
		this.id = id;
		this.orders = new ArrayList<ReceiverOrder>();
		this.products = new ArrayList<ReceiverProduct>();
		this.receiverCharacteristics = ReceiverCharacteristics.newInstance();		
	}
	

	@Override
	public boolean addPlan(ReceiverOrder order) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public ReceiverOrder createCopyOfSelectedPlanAndMakeSelected() {
		ReceiverOrder order = ReceiverImpl.copyOrder(this.selectedOrder);
		this.setSelectedPlan(order);
		return order;
	}
	
	/*
	 * Copies a receiver order and the order cost.
	 */

	private static ReceiverOrder copyOrder(ReceiverOrder orderToCopy) {
		List<Order> orders = new ArrayList<Order>();
		for(Order order : orderToCopy.getReceiverOrders()){
			Id<Order> orderId = order.GetId();
			Receiver receiver = order.getReceiver();
			ReceiverProduct product = order.getProduct();
			double time = order.getServiceDuration();
			orders.add(Order.newInstance(orderId, receiver, product, time));
		}
		ReceiverOrder copiedOrder = new ReceiverOrder(orderToCopy.getReceiver(), orders, orderToCopy.getOrderCarrier());
		double cost = orderToCopy.getScore();
		copiedOrder.setScore(cost);
		return copiedOrder;
	}
	
	/*
	 * Removes an order from the receiver's list of orders.
	 */

	@Override
	public boolean removePlan(ReceiverOrder order) {
		return this.orders.remove(order);
	}

	/*
	 * Returns the receiver id.
	 */
	
	@Override
	public Id<Receiver> getId() {
		return id;
	}

	/*
	 * Returns a list of the receiver's orders.
	 */
	
	@Override
	public List<ReceiverOrder> getPlans() {
		return orders;
	}

	/*
	 * Returns a list of the receiver's products.
	 */
	
	@Override
	public List<ReceiverProduct> getProducts() {
		return products;
	}

	@Override
	public ReceiverOrder getSelectedPlan() {
		return selectedOrder;
	}
	
	@Override
	public ReceiverCharacteristics getReceiverCharacteristics() {
		return receiverCharacteristics;
	}
	
	/**
	 * Sets the selected receiver plan.
	 * @param selectedOrder
	 */

	@Override
	public void setSelectedPlan(ReceiverOrder selectedOrder) {
		if(!orders.contains(selectedOrder)) orders.add(selectedOrder);
		this.selectedOrder = selectedOrder;		
	}

	/**
	 * Sets the receiver's characteristics.
	 * @param receiverCharacteristics
	 */
	
	@Override
	public void setReceiverCharacteristics(ReceiverCharacteristics receiverCharacteristics) {
		this.receiverCharacteristics = receiverCharacteristics;		
	}

}
