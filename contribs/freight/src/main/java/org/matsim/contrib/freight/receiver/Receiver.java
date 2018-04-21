package org.matsim.contrib.freight.receiver;

import java.util.Collection;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.utils.objectattributes.attributable.Attributable;

/**
 * A receiver.
 * 
 * @author wlbean
 *
 */

public interface Receiver extends HasPlansAndId<ReceiverOrder, Receiver>, Attributable {

	/**
	 * Gets the receiverId.
	 */
	@Override
	public abstract Id<Receiver> getId();

	public Id<Link> getLinkId();

	/**
	 * Set the link Id from which the receiver is accessed. This is an easy-option 
	 * (similar to using a Builder) to assign multiple characteristics.
	 * @param linkId
	 * @return
	 */
	public Receiver setLinkId(Id<Link> linkId);

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
	 * Gets a specific product for the receiver.
	 * 
	 * @param productType
	 * @return
	 */
	public abstract ReceiverProduct getProduct(Id<ProductType> productType);

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
	 * TODO (2018/04, jwj & wlb): currently we assume that time windows are set per receiver, and not per
	 * product. This may be changed in future if a receiver (specific location) will allow certain products
	 * during certain times of the day. Then the time window must be (re)located to the {@link ReceiverProduct}, 
	 * for example.

	 * @param window
	 * @return
	 */
	public abstract Receiver addTimeWindow(final TimeWindow window);

	public abstract boolean isInTimeWindow(double time);

	public abstract List<TimeWindow> getTimeWindows();
}

