/**
 * 
 */
package org.matsim.contrib.freight.receiver;

import org.matsim.api.core.v01.Id;

/**
 * This is a product type.
 * 
 * @author wlbean
 *
 */
public interface ProductType {	
	
	
	public void setProductDescription(String description);

	public void setRequiredCapacity(double reqCapacity);

	public String getDescription();

	public double getRequiredCapacity();

	public Id<ProductType> getId();

}
