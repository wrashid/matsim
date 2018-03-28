/**
 * 
 */
package org.matsim.contrib.freight.usecases.receiver;

import org.matsim.contrib.freight.receiver.Receiver;
import org.matsim.contrib.freight.receiver.ReceiverOrder;
import org.matsim.contrib.freight.receiver.ReceiverOrderStrategyManagerFactory;
import org.matsim.core.replanning.GenericPlanStrategy;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;

/**
 * @author u04416422
 *
 */
public class MyReceiverOrderStrategyManagerFactorImpl implements ReceiverOrderStrategyManagerFactory {

	/* (non-Javadoc)
	 * @see org.matsim.contrib.freight.receiver.ReceiverOrderStrategyManagerFactory#createReceiverStrategyManager()
	 */
	@Override
	public GenericStrategyManager<ReceiverOrder, Receiver> createReceiverStrategyManager() {
		final GenericStrategyManager<ReceiverOrder, Receiver> stratMan = new GenericStrategyManager<>();
		stratMan.setMaxPlansPerAgent(5);
		{
			GenericPlanStrategy<ReceiverOrder, Receiver> strategy = new GenericPlanStrategyImpl<>(new ExpBetaPlanChanger<ReceiverOrder, Receiver>(1.));
			stratMan.addStrategy(strategy, null, 1.0);

		}

		return stratMan;
	}
	
}
