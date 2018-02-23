/**
 * 
 */
package org.matsim.contrib.freight.receiver;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * Returns a new instance of a receiver product with associated information, such as location, order policy parameters (min and max levels) and possibly demand rate (to be included later).
 * 
 * The default values are: min level =  100 000 units, max level = 500 000 units.
 * 
 * @author wlbean
 *
 */
public class ReceiverProduct {

	public static ReceiverProduct newInstance(){
		return new ReceiverProduct();		
	}
	
	/** 
	 * A builder that is used to build the product instance for the receiver.]
	 */
	public static class Builder {
		
		/**
		 * This returns a builder with locationId.
		 */
		
		public static Builder newInstance(){
			return new Builder();
		}
		
		//private Id<Link> locationId;
		private ReceiverProductType productType;
		//private Id<ProductType> typeId = productType.getId();
		private int maxLevel = 500000;
		private int minLevel = 100000;
		//private double reqCap = productType.getRequiredCapacity();
		//private String descr = productType.getDescription();
		
		//public Builder(Id<Link> locationId){
		//	this.locationId = locationId;
		//}
		
		/**
		 * Set relevant receiver product types.
		 * @param productType
		 * @return 
		 */
		
		public Builder setProductType(ReceiverProductType productType){
			this.productType = productType;
			return this;
		}
		
		/**
		 * Set relevant product type id.
		 * @param typeId
		 * @return
		 */
		
		//public Builder setProductTypeId(Id<ProductType> typeId){
		//	this.typeId = typeId;
		//	return this;
		//}
		
		/**
		 * Set the maximum inventory level of the receiver (assuming that the receiver employs a min-max inventory management policy).
		 * @param maxLevel
		 * @return
		 */
		
		public Builder setMaxLevel(int maxLevel){
			this.maxLevel = maxLevel;
			return this;
		}
		
		/**
		 * Set the minimum inventory level (or reorder point) of the receiver (assuming that the receiver employs a min-max inventory management policy).
		 * @param minLevel
		 * @return
		 */
		
		public Builder setMinLevel(int minLevel){
			this.minLevel = minLevel;
			return this;
		}
		
		public ReceiverProduct build(){
			return new ReceiverProduct(this);
		}
	}
	
	
	//private final Id<Link> locationId;
	//private Id<ProductType> typeId;
	private ReceiverProductType productType;
	private int maxLevel;
	private int minLevel;
	//private double reqCap;
	//private String descr;
	private ReceiverCharacteristics receiverChar;
	
	private ReceiverProduct(){
		super();
		//this.locationId = location;
		maxLevel = 500000;
		minLevel = 100000;
	}
	
	private ReceiverProduct(Builder builder){
		//this.locationId = builder.locationId;
		this.productType = builder.productType;
		//this.typeId = builder.typeId;
		this.maxLevel = builder.maxLevel;
		this.minLevel = builder.minLevel;	
		//this.descr = builder.descr;
		//this.reqCap = builder.reqCap;
	}
	
	/** 
	 * Returns receiver product location.
	 * 
	 */
	
	public Id<Link> getLocation(){
		return receiverChar.getLocation();
	//	return locationId;
	}
	

	/**
	 * Returns receiver product type.
	 */
	
	public ReceiverProductType getProductType(){
		return productType;
	}
	
	/**
	 * Returns the product type id.
	 */
	
	Id<ProductType> getProductTypeId(){
		return productType.getId();
	}
	
	/**
	 * Returns the maximum inventory level of the receiver (assuming that the receiver employs a min-max inventory management policy).
	 */
	
	public int getMaxLevel(){
		return maxLevel;
	}
	
	/**
	 * Returns the minimum inventory level (or reorder point) of the receiver (assuming that the receiver employs a min-max inventory management policy).
	 */
	
	public int getMinLevel(){
		return minLevel;
	}
	
	/**
	 * Returns the description of the product type of this particular receiver product.
	 */
	
	public String getDescription(){
		return productType.getDescription();
	}
	
	/**
	 * Returns the required capacity of the product type of this particular receiver product.
	 */
	
	public double getRequiredCapacity(){
		return productType.getRequiredCapacity();
	}
	

}
