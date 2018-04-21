/**
 * 
 */
package org.matsim.contrib.freight.receiver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.utils.misc.Time;

/**
 * This contains relevant characteristics of the receiver. Such as product types, specific receiver product instances, prescribed delivery time windows, etc.
 * 
 * Default time window start and end times are 06:00 and 18:00 respectively. Default link id = "1".
 * 
 * @author wlbean
 *
 */
public class ReceiverCharacteristics {
	
	/**
	 * Builds a new instance of a receiver with relevant characteristics.
	 *
	 */
	
	public static class Builder {
		
		public static Builder newInstance(){ return new Builder(); }
		
		private Collection<ReceiverProductType> productTypes = new ArrayList<ReceiverProductType>();
		
		private Collection<ReceiverProduct> products = new ArrayList<ReceiverProduct>();
		
		private Set<Id<ProductType>> productTypeIds = new HashSet<>();
		
		private double timeWindowStart = Time.parseTime("06:00:00");
		
		private double timeWindowEnd = Time.parseTime("18:00:00");
		
		private Id<Link> location = Id.createLinkId("1");
		
		public Builder addProductType(ReceiverProductType productType){
			if(!productTypeIds.contains(productType.getId())){
				productTypes.add(productType);
				productTypeIds.add(productType.getId());
			}
			return this;
		}
		
		public Builder addProduct(ReceiverProduct product){
			products.add(product);
			if(product.getProductType() != null) addProductType(product.getProductType());
			return this;
		}
		
		/**
		 * Sets the start time of the receiver delivery timewindow.
		 * @param timeWindowStartTime
		 * @return
		 */
		
		public Builder setTimeWindowStart(double timeWindowStart){
			this.timeWindowStart = timeWindowStart;
			return this;
		}
		
		/**
		 * Sets the end time of the receiver delivery timewindow.
		 * @param timeWindowEndTime
		 * @return
		 */
		
		public Builder setTimeWindowEnd(double timeWindowEnd){
			this.timeWindowEnd = timeWindowEnd;
			return this;
		}
		/**
		 * Sets the receiver facility location.
		 * @param location
		 * @return
		 */
		
		public Builder setLocation(Id<Link> location){
			this.location = location;
			return this;
		}
		
		public ReceiverCharacteristics build(){
			return new ReceiverCharacteristics(this);
		}
		
	}
	
	private Collection<ReceiverProduct> receiverProducts = new ArrayList<ReceiverProduct>();
	private Collection<ReceiverProductType> receiverProductTypes = new ArrayList<ReceiverProductType>();
	private double timeWindowStart;
	private double timeWindowEnd;
	private Id<Link> location;
	
	/**
	 * Creates a new instance of a receiver without any characteristics.
	 * 
	 */

	
	public static ReceiverCharacteristics newInstance(){
		return new ReceiverCharacteristics();
	}
	
	private ReceiverCharacteristics(){}
	
	private ReceiverCharacteristics(Builder builder) {
		this.receiverProducts = builder.products;
		this.receiverProductTypes = builder.productTypes;
		this.timeWindowStart = builder.timeWindowStart;
		this.timeWindowEnd = builder.timeWindowEnd;
		this.location = builder.location;
	}
	
	/**
	 * Returns a collection of products - with demand rates and order policy parameters - a receiver uses.
	 * 
	 */
	
	public Collection<ReceiverProduct> getReceiverProducts(){
		return receiverProducts;
	}
	
	/**
	 * Returns a collection of the product types a receiver uses.
	 */
	
	public Collection<ReceiverProductType> getProductTypes(){
		return receiverProductTypes;
		
	}
	
	/**
	 * Returns the receiver's prescribed delivery time window start time.
	 */
	
	public double getTimeWindowStart(){
		return timeWindowStart;
	}
	
	/**
	 * Returns the receiver's prescribed delivery time window end time.
	 */
	
	public double getTimeWindowEnd(){
		return timeWindowEnd;
	}
	
	/**
	 * Returns the receiver facility location.
	 */
	
	public Id<Link> getLocation(){
		return location;
	}

}


