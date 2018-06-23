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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.freight.carrier.Carriers;

/**
 *
 * @author jwjoubert, wlbean
 */
public class MutableFreightScenario implements FreightScenario {
	final private Logger log = Logger.getLogger(MutableFreightScenario.class);
	private Scenario sc;
	private Carriers carriers;
	private Receivers receivers;
	
	public MutableFreightScenario(Scenario sc, Carriers carriers) {
		this.sc = sc;
		this.carriers = carriers;
	}

	
	@Override
	public Scenario getScenario() {
		return this.sc;
	}

	
	@Override
	public Carriers getCarriers() {
		return this.carriers;
	}

	public void setReceivers(Receivers receivers) {
		this.receivers = receivers;
	}
	
	@Override
	public Receivers getReceivers() {
		if(this.receivers == null) {
			log.error("No receivers were set. Returning new, empty receivers.");
			return new Receivers();
		}
		
		return this.receivers;
	}

}
