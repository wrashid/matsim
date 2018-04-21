/**
 * 
 */
package org.matsim.contrib.freight.receiver;

import org.matsim.api.core.v01.Identifiable;
import org.matsim.utils.objectattributes.attributable.Attributable;

/**
 * General interface for different product types.
 * 
 * @author wlbean, jwjoubert
 */
public interface ProductType extends Identifiable<ProductType>, Attributable{	
	
	public void setProductDescription(String description);

	public void setRequiredCapacity(double reqCapacity);

	public String getDescription();

	public double getRequiredCapacity();


}
