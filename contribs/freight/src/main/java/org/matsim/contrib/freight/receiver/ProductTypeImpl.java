/**
 * 
 */
package org.matsim.contrib.freight.receiver;

import org.matsim.api.core.v01.Id;

/**
 * This implements the product types of the receiver.
 * 
 * @author wlbean
 *
 */
public class ProductTypeImpl implements ProductType{
	
	
	public static ProductType newInstance(Id<ProductType> typeId) {
		return new ProductTypeImpl(typeId);
	}
	
	/**
	 * Set default values.
	 */
	
	private String descr = "";
	private double reqCapacity = 10;
	private Id<ProductType> typeId;
	
	private ProductTypeImpl(final Id<ProductType> typeId){
		this.typeId = typeId;
	}

	
	@Override
	public void setProductDescription(String description){
		this.descr = description;
	}
	
	@Override
	public void setRequiredCapacity(double reqCapacity){
		this.reqCapacity = reqCapacity;
	}
	
	@Override 
	public String getDescription(){
		return descr;
	}
	
	@Override
	public double getRequiredCapacity(){
		return reqCapacity;
	}
	
	@Override
	public Id<ProductType> getId(){
		return typeId;
	}


}
