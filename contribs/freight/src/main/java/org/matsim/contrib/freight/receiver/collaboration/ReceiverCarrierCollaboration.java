/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
  
/**
 * 
 */
package org.matsim.contrib.freight.receiver.collaboration;

import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.receiver.FreightScenario;
import org.matsim.contrib.freight.receiver.Receivers;
import org.matsim.utils.objectattributes.attributable.Attributable;

/**
 *
 * @author jwjoubert, wlbean
 */
public interface ReceiverCarrierCollaboration extends Attributable {
	
	
	/**
	 * This method is provided with a complete {@link FreightScenario} with its
	 * {@link Carriers} and {@link Receivers}. The coalition costs are calculated
	 * and assigned to the <i>same</i> coalition members. The same container 
	 * is then returned with the (possibly adjusted) costs.
	 *  
	 * @param scenario
	 * @return
	 */
	public FreightScenario allocateCoalitionCosts(FreightScenario scenario);
	
	public String getDescription();
}
