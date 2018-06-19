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

package org.matsim.contrib.freight.receiver;

import org.matsim.core.controler.AbstractModule;

public class ReceiverModule extends AbstractModule {

	private Receivers receivers;
	private ReceiverScoringFunctionFactory sFuncFac;
	private ReceiverOrderStrategyManagerFactory stratManFac;
	
	
	public ReceiverModule(Receivers receivers, ReceiverScoringFunctionFactory sFuncFac, ReceiverOrderStrategyManagerFactory stratManFac){
		this.receivers = receivers;
		this.sFuncFac = sFuncFac;
		this.stratManFac = stratManFac;
	};
	
	
	@Override
	public void install() {

		
		bind(Receivers.class).toInstance(receivers);
		
		if (sFuncFac != null){
		bind(ReceiverScoringFunctionFactory.class).toInstance(sFuncFac);
		}
		
		if (stratManFac != null){
		bind(ReceiverOrderStrategyManagerFactory.class).toInstance(stratManFac);
		}
		
		/*
		 * Need a controler listener. Keeping it simple for now.
		 */
		
       addControlerListenerBinding().to(ReceiverControlerListener.class);
		
    }
       
		
}
