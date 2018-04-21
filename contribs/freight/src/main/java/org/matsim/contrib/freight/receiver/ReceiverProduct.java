/**
 * 
 */
package org.matsim.contrib.freight.receiver;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.receiver.reorderPolicy.ReorderPolicy;
import org.matsim.contrib.freight.receiver.reorderPolicy.SSReorderPolicy;

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
	
	private ReorderPolicy policy = new SSReorderPolicy(100000.0, 500000.0);
	private double stockOnHand = 0.0;
	//private final Id<Link> locationId;
	//private Id<ProductType> typeId;
	private ProductType productType;
	//private double reqCap;
	//private String descr;
	
	ReceiverProduct(){
		super();
	}
	
	private ReceiverProduct(Builder builder){
		//this.locationId = builder.locationId;
		this.productType = builder.productType;
		//this.typeId = builder.typeId;
		this.policy = builder.policy;
		//this.descr = builder.descr;
		//this.reqCap = builder.reqCap;
		this.stockOnHand = builder.onHand;
	}
	

	/**
	 * Returns receiver product type.
	 */
	
	public ProductType getProductType(){
		return productType;
	}
	
	/**
	 * Returns the product type id.
	 */
	
	Id<ProductType> getProductTypeId(){
		return productType.getId();
	}
	
	
	public ReorderPolicy getReorderPolicy() {
		return this.policy;
	}
	
	public double getStockOnHand() {
		return stockOnHand;
	}
	
	public void setStockOnHand(double stockOnHand) {
		this.stockOnHand = stockOnHand;
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

	/** 
	 * A builder that is used to build the product instance for the receiver.
	 * 
	 * FIXME There are multiple ways to create/set things. And, reading from the
	 * XML file means that you must read the ReceiverProduct first, BEFORE you 
	 * get to the ReorderPolicy. 
	 */
	public static class Builder {
		
		/**
		 * This returns a builder with locationId.
		 */
		
		public static Builder newInstance(){
			return new Builder();
		}
		
		private ReorderPolicy policy = new SSReorderPolicy(100000.0, 500000.0);
		private double onHand = 0.0;
		//private Id<Link> locationId;
		private ProductType productType;
		//private Id<ProductType> typeId = productType.getId();
		//private double reqCap = productType.getRequiredCapacity();
		//private String descr = productType.getDescription();
		
		//public Builder(Id<Link> locationId){
		//	this.locationId = locationId;
		//}
		
		/**
		 * Set relevant receiver product types.
		 * @param productType2
		 * @return 
		 */
		
		public Builder setProductType(ProductType productType2){
			this.productType = productType2;
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
		
		public Builder setReorderingPolicy(ReorderPolicy policy) {
			this.policy = policy;
			return this;
		}
		
		
		/**
		 * Set the current (opening) inventory for the product at the receiver. Defaults to 0 units on hand.
		 * @param onHand
		 * @return
		 */
		public Builder setQuantityOnHand(double onHand) {
			this.onHand = onHand;
			return this;
		}
		
		
		public ReceiverProduct build(){
			return new ReceiverProduct(this);
		}
		
		
	}
	

}
