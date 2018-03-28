/**
 * 
 */
package org.matsim.contrib.freight.receiver;

import java.util.ArrayList;
import java.util.Collection;

import javax.inject.Inject;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.ReplanningEvent;
import org.matsim.core.controler.events.ScoringEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.ReplanningListener;
import org.matsim.core.controler.listener.ScoringListener;
import org.matsim.core.replanning.GenericStrategyManager;

/**
 * This controller ensures that each receiver receives a cost (score) per order at the end of each iteration and replans its orders based on the cost of the previous iteration and past iterations.
 * 
 * @author wlbean
 *
 */
public class ReceiverControlerListener implements ScoringListener,
ReplanningListener, BeforeMobsimListener {
	
	private Receivers receivers;
	private ReceiverOrderStrategyManagerFactory stratManFac;
	private ReceiverScoringFunctionFactory scorFuncFac;
	private ReceiverTracker tracker;
	@Inject EventsManager eMan;
	
	/**
	 * This creates a new receiver controler listener for receivers with replanning abilities.
	 * @param receivers
	 * @param stratManFac
	 */
	
	@Inject
	ReceiverControlerListener(Receivers receivers, ReceiverOrderStrategyManagerFactory stratManFac, ReceiverScoringFunctionFactory scorFuncFac){
		this.receivers = receivers;
		this.stratManFac = stratManFac;
		this.scorFuncFac = scorFuncFac;
			}


	@Override
	public void notifyReplanning(ReplanningEvent event) {
	
		GenericStrategyManager<ReceiverOrder, Receiver> stratMan = stratManFac.createReceiverStrategyManager();
		
		Collection<HasPlansAndId<ReceiverOrder, Receiver>> receiverCollection = new ArrayList<>();
		
		for(Receiver receiver : receivers.getReceivers().values()){
			receiverCollection.add(receiver);
		}
		
		stratMan.run(receiverCollection, null, event.getIteration(), event.getReplanningContext());		
	}

	
	/*
	 * Determines the order cost at the end of each iteration.
	 */
	
	@Override
	public void notifyScoring(ScoringEvent event) {
	this.tracker.scoreSelectedOrders();
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		tracker = new ReceiverTracker(receivers, scorFuncFac);
		eMan.addHandler(tracker);		
	}

}
