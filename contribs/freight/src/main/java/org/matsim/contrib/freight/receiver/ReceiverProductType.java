/**
 * 
 */
package org.matsim.contrib.freight.receiver;

import org.matsim.api.core.v01.Id;

/**
 * The receiver product type that contains, amongst others, required capacity information.
 * 
 * @author wlbean
 *
 */

public class ReceiverProductType implements ProductType{
	
	/**
	 * Builds a new receiver product type with typeId. The default required capacity information is 10kg per item.
	 *
	 */
	
	private ProductType productType;
	
	public ReceiverProductType(ProductType productType){
		super();
		this.productType = productType;
	}

	
	public static class Builder {
		
	
		public static Builder newInstance(Id<ProductType> typeId){
			return new Builder(typeId);
		}
		
		private Id<ProductType> typeId;
		private double reqCapacity = 10;
		private String description;
		
		private Builder(Id<ProductType> typeId){
			this.typeId = typeId;
		}
		
		/**
		 * Sets the required capacity for product type (kg per single item of a specific type).
		 * 
		 * The default is 10 kg per item.
		 * 
		 */
		
		public Builder setRequiredCapacity(double reqCapacity) {
			this.reqCapacity = reqCapacity;
			return this;
			
		}
		
		/**
		 * Sets the product type description
		 * 
		 */
		
		public Builder setProductDescription(String description) {
			this.description = description;
			return this;			
		}
		
		
		/**
		 * Build the receiver product type.
		 * 
		 */
			
		public ReceiverProductType build(){
			return new ReceiverProductType(this);
		}
	}


	private String description;
	private double reqCapacity;
	
	private ReceiverProductType(Builder builder) {
			ProductTypeImpl.newInstance(builder.typeId);
			this.description = builder.description;
			this.reqCapacity = builder.reqCapacity;
			
		}	
		
		/**
		 * Returns the description of this product type.
		 */
		
		public String getDescription(){
			return description;
		}
		
		/**
		 * Returns the required capacity of this product type.
		 */
		
		public double getRequiredCapacity(){
			return reqCapacity;
		}
		
		/**
		 * Implementing Product type methods - Get product id.
		 */

		@Override
		public Id<ProductType> getId() {
			return productType.getId();
		}

		/**
		 * Implementing Product type methods - set product type description.
		 */
		
		@Override
		public void setProductDescription(String description) {
			productType.setProductDescription(description);
			
		}

		/**
		 * Implementing Product type methods - set product type required capacity.
		 */
		
		@Override
		public void setRequiredCapacity(double reqCapacity) {
			productType.setRequiredCapacity(reqCapacity);
			
		}




}