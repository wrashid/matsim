/**
 * 
 */
package org.matsim.contrib.freight.receiver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * This returns a receiver that has characteristics and orders.
 * 
 * @author wlbean, jwjoubert
 *
 */
public class ReceiverImpl implements Receiver {
	final private Logger log = Logger.getLogger(Receiver.class);
	
	private Attributes attributes = new Attributes();
	private Id<Link> location;
	private List<TimeWindow> timeWindows = new ArrayList<>();
	
	
	
	/*
	 * Create a new instance of a receiver.
	 */
	public static ReceiverImpl newInstance(Id<Receiver> id){
		return new ReceiverImpl(id);
	}
	
	private final Id<Receiver> id;
	private final List<ReceiverOrder> orders;
	private final List<ReceiverProduct> products;
	private ReceiverOrder selectedOrder;
	
	private ReceiverImpl(final Id<Receiver> id){
//		super();
		this.id = id;
		this.orders = new ArrayList<ReceiverOrder>();
		this.products = new ArrayList<ReceiverProduct>();
	}
	
	
	
	
	

	@Override
	public boolean addPlan(ReceiverOrder order) {
		throw new RuntimeException("not implemented");
	}

	@Override
	public ReceiverOrder createCopyOfSelectedPlanAndMakeSelected() {
		ReceiverOrder order = selectedOrder.createCopy();
		this.setSelectedPlan(order);
		return order;
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
	public Attributes getAttributes() {
		return this.attributes;
	}

	
	public Receiver setLocation(Id<Link> linkId) {
		this.location = linkId;
		return this;
	}
	
	/**
	 * Returns the link from which the receiver is accessed.
	 * TODO One may consider changing this so that it is a list of links.
	 */
	@Override
	public Id<Link> getLinkId() {
		return this.location;
	}

	
	@Override
	public Receiver addTimeWindow(TimeWindow window) {
		/*TODO May want to check/consolidate time windows. */
		this.timeWindows.add(window);
		return this;
	}
	
	
	/**
	 * Checks if a given time is within the allowable time window(s).
	 * 
	 * @return true if the time is within at leats one of the set time 
	 * window(s), or <i>if no time windows are set</i>.
	 */
	@Override
	public boolean isInTimeWindow(double time) {
		if(this.timeWindows.isEmpty()) {
			log.warn("No time windows are set! Assuming any time is uitable.");
			return true;
		}
		
		boolean inTimeWindow = false;
		Iterator<TimeWindow> iterator = this.timeWindows.iterator();
		
		while(!inTimeWindow & iterator.hasNext()) {
			TimeWindow tw = iterator.next();
			if(time >= tw.getStart() && time <= tw.getEnd()) {
				inTimeWindow = true;
			}
		}
		return false;
	}
	
	
	public List<TimeWindow> getTimeWindows(){
		return this.timeWindows;
	}

	
	@Override
	public Receiver setLinkId(Id<Link> linkId) {
		this.location = linkId;
		return this;
	}

	@Override
	public ReceiverProduct getProduct(Id<ProductType> productType) {
		ReceiverProduct product = null;
		Iterator<ReceiverProduct> iterator = this.products.iterator();
		while(product == null & iterator.hasNext()) {
			ReceiverProduct thisProduct = iterator.next();
			if(thisProduct.getProductTypeId().equals(productType)) {
				product = thisProduct;
			}
		}
		if(product == null) {
			log.warn("Receiver \"" + this.id.toString() 
			+ "\" does not have the requested product type \"" + productType.toString() + "\". Returning null.");
		}
		return product;
	}

}
