/**
 * 
 */
package org.matsim.contrib.freight.receiver;

import org.matsim.api.core.v01.Id;
import org.matsim.utils.objectattributes.attributable.Attributes;

/**
 * This implements the product types of the receiver.
 * 
 * TODO (JWJ, WLB, April 2018: think about how we can/should convert (seemlessly)
 * between volume and weight. Consider the IATA Dimensional Weight Factor. Think
 * about expressing a factor "percentage of cube(meter)-ton". A cubic meter of
 * toilet paper will have a factor of 1.0, and at the same time a brick-size
 * weighing a ton will also have a factor of 1.0. This is necessary as jsprit 
 * can only work with ONE unit throughout its optimisation, i.e. and associate
 * it with vehicle capacity.
 * 
 * @author wlbean
 *
 */
public class ProductTypeImpl implements ProductType{
	
	
//	private static ProductType newInstance(Id<ProductType> typeId) {
//		return new ProductTypeImpl(typeId);
//	}
	
	/**
	 * Set default values.
	 */
	
	private String descr = "";
	private double reqCapacity = 10;
	private Id<ProductType> typeId;
	
	ProductTypeImpl(final Id<ProductType> typeId){
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


	@Override
	public Attributes getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}


}
