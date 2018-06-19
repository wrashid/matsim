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

import java.util.ArrayList;
import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.scoring.ScoringFunction;

/**
 * This keeps track of all receiver agents during simulation.
 * 
 * @author wlbean
 *
 */
public class ReceiverTracker implements EventHandler {

	private final Receivers receivers;
	private final Collection<ReceiverAgent> receiverAgents = new ArrayList<ReceiverAgent>();
	
	public ReceiverTracker(Receivers receivers, ReceiverScoringFunctionFactory scorFuncFac){
		this.receivers = receivers;
		createReceiverAgents(scorFuncFac);
	}
	
	/*
	 * Scores the selected receiver order.
	 */


	public void scoreSelectedOrders() {
		for (Receiver receiver : receivers.getReceivers().values()){
			ReceiverAgent rAgent = findReceiver(receiver.getId());
			rAgent.scoreSelectedOrder();
		}		
	}
		
	
	/*
	 * Creates the list of all receiver agents.
	 */
	
	private void createReceiverAgents(ReceiverScoringFunctionFactory scorFuncFac) {
			for (Receiver receiver: receivers.getReceivers().values()){
				ScoringFunction receiverScorFunc = scorFuncFac.createScoringFunction(receiver);
				ReceiverAgent rAgent = new ReceiverAgent(receiver, receiverScorFunc);
				receiverAgents.add(rAgent);
			}
	}
	
	
	/*
	 * Find a particular receiver agent in the list of receiver agents.
	 */
	
	private ReceiverAgent findReceiver(Id<Receiver> id) {
		for (ReceiverAgent rAgent : receiverAgents){
			if (rAgent.getId().equals(id)){
				return rAgent;
			}
		}
		return null;
	}
	

}
