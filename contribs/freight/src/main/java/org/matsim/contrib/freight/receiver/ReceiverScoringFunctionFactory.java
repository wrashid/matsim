/**
 * 
 */
package org.matsim.contrib.freight.receiver;

import org.matsim.core.scoring.ScoringFunction;

/**
 * Scoring functin factory interface for receiver agents;
 * 
 * @author wlbean
 *
 */
public interface ReceiverScoringFunctionFactory {

	ScoringFunction createScoringFunction(Receiver receiver);
	
}
