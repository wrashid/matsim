/**
 * 
 */
package org.matsim.contrib.freight.usecases.receiver;

import org.matsim.contrib.freight.receiver.Receiver;
import org.matsim.contrib.freight.receiver.ReceiverScoringFunctionFactory;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.SumScoringFunction;
import org.matsim.core.scoring.SumScoringFunction.MoneyScoring;

/**
 * This is an example implementation of the receiver scoring function factory.
 * 
 * @author wlbean
 *
 */
public class MyReceiverScoringFunctionFactoryImpl implements ReceiverScoringFunctionFactory {

	    public MyReceiverScoringFunctionFactoryImpl() {
	    }

		@Override
		public ScoringFunction createScoringFunction(Receiver receiver) {
	        SumScoringFunction costFunction = new SumScoringFunction();
	        
			MoneyScoring carriertoReceiverCostAllocation = new CarriertoReceiverCostAllocation();
	        costFunction.addScoringFunction(carriertoReceiverCostAllocation);
	        return costFunction;
	    }
		
		static class CarriertoReceiverCostAllocation implements MoneyScoring {
			
			private double cost = 0.0;
			
		 public void reset(){
				this.cost = 0.0;
			}

			@Override
			public void finish() {
			
			}			


			@Override
			public double getScore() {
				return this.cost;
			}

			/*
			 * Adds the carrier cost to the receiver cost based on the number of receivers it serves. Currently only two receivers...this should be updated.
			 */
			@Override
			public void addMoney(double amount) {
				this.cost += amount/2;
			System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
			System.out.print(this.cost);
			System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
			}

			
			
		}

}
