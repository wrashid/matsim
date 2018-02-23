package org.matsim.contrib.freight.receiver;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.HasPlansAndId;

/**
 * A receiver.
 * 
 * @author wlbean
 *
 */

public interface Receiver extends HasPlansAndId<ReceiverOrder, Receiver> {
	
	/**
	 * Gets the receiverId.
	 * 
	 */
	
	@Override
	public abstract Id<Receiver> getId();
	
	/**
	 * Gets a collection of receiver orders.
	 */

	@Override
	public abstract List<ReceiverOrder> getPlans();
	
	/**
	 * Gets a collection of receiver products.
	 */
	
	public abstract Collection<ReceiverProduct> getProducts();
	
	/**
	 * Gets the selected receiver order.
	 */
	
	@Override
	public abstract ReceiverOrder getSelectedPlan();
	
	/**
	 * Set the selected receiver order.
	 */
	
	@Override
	public abstract void setSelectedPlan(ReceiverOrder selectedOrder);
	
	/**
	 * Sets receiver characteristics.
	 */

	public abstract void setReceiverCharacteristics(ReceiverCharacteristics receiverCharacteristics);
	
	/**
	 * Gets receiver characteristics.
	 */
	
	public abstract ReceiverCharacteristics getReceiverCharacteristics();
	
}

