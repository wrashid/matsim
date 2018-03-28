/**
 * 
 */
package org.matsim.contrib.freight.receiver;

import org.matsim.core.replanning.GenericStrategyManager;

/**
 * @author u04416422
 *
 */
public interface ReceiverOrderStrategyManagerFactory {

	public GenericStrategyManager<ReceiverOrder, Receiver> createReceiverStrategyManager();

}
