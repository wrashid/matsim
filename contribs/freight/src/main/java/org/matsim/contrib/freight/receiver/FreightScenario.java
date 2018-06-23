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
package org.matsim.contrib.freight.receiver;

import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carriers;

/**
 *
 * @author jwjoubert, wlbean
 */
public interface FreightScenario{
	
	public Scenario getScenario();

	public Carriers getCarriers();
	
	public Receivers getReceivers();
	

}
