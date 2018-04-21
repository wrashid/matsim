/**
 * 
 */
package org.matsim.contrib.freight.receiver;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * A container that maps Receivers.
 * 
 * @author wlbean
 *
 */
public class Receivers implements Attributable{
	
	private Logger log = Logger.getLogger(Receivers.class);
	private Attributes attributes = new Attributes();
	private String desc = "";
	
	/**
	 * Create empty receiver collection.
	 */
	
	private Map<Id<Receiver>, Receiver> receiverMap = new TreeMap<>();
	private Map<Id<ProductType>, ProductType> productTypeMap = new TreeMap<>();
	
	public Receivers(Collection<Receiver> receivers){
		makeMap(receivers);
	}
	
	/**
	 * Add receivers to the empty collection.
	 * @param receivers
	 */
	
	private void makeMap(Collection<Receiver> receivers){
		for (Receiver r : receivers){
			this.receiverMap.put(r.getId(), r);
		}
	}
	
	public Receivers(){
		
	}
	
	/**
	 * Returns the receivers in the collection.
	 * @return
	 */
	
	public Map<Id<Receiver>, Receiver> getReceivers(){
		return receiverMap;
	}
	
	/**
	 * Add new receivers to the collection.
	 * @param receiver
	 */
	
	public void addReceiver(Receiver receiver){
		if(!receiverMap.containsKey(receiver.getId())){
			receiverMap.put(receiver.getId(), receiver);
		}
		else log.warn("receiver \"" + receiver.getId() + "\" already exists.");
	}
	
	public ProductType createAndAddProductType(final Id<ProductType> id) {
		if(this.productTypeMap.containsKey(id)) {
			throw new IllegalArgumentException("ProductType with id \"" + id + "\" already exists.");
		}
		ProductType pt = new ProductTypeImpl(id);
		this.productTypeMap.put(id, pt);
		return pt;
	}

	public void addProductType(ProductType productType) {
		if(this.productTypeMap.containsKey(productType.getId())) {
			throw new RuntimeException("The product type \"" + productType.getId() 
			+ "\" already exists and cannot be added again.");
		}
		this.productTypeMap.put(productType.getId(), productType);
	}
	
	public ProductType getProductType(Id<ProductType> productId) {
		if(this.productTypeMap.containsKey(productId)) {
			return this.productTypeMap.get(productId);
		} else {
			log.warn("Couldn't find a product of type \"" + productId.toString() 
			+ "\". Returning null.");
		}
		return null;
	}
	
	public final Collection<ProductType> getAllProductTypes(){
		return this.productTypeMap.values();
	}

	@Override
	public Attributes getAttributes() {
		return this.attributes;
	}
	
	public void setDescription(String desc) {
		this.desc = desc;
	}
	
	public String getDescription() {
		return this.desc;
	}
	
	/**
	 * Ensures that each {@link Receiver}'s {@link ReceiverOrder}s are linked
	 * to a {@link Carrier}, and does not simply have a pointer to the 
	 * {@link Carrier}'s {@link Id}.
	 */
	public void linkReceiverOrdersToCarriers(Carriers carriers) {
		for(Receiver receiver : this.receiverMap.values()) {
			for(ReceiverOrder order : receiver.getPlans()) {
				/* Check that the carrier actually exists. */
				if(!carriers.getCarriers().containsKey(order.getCarrierId())) {
					throw new RuntimeException("Cannot find carrier \"" 
							+ order.getCarrierId().toString() + "\" for receiver \"" 
							+ receiver.getId().toString() + "\"'s order. ");
				}

				order.setCarrier(carriers.getCarriers().get(order.getCarrierId()));
			}
		}
	}
	
}
