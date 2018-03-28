package org.matsim.contrib.freight.usecases.receiver;

import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.usecases.chessboard.CarrierScoringFunctionFactoryImpl;
import org.matsim.core.scoring.ScoringFunction;

public class MyCarrierScoringFunctionFactoryImpl implements CarrierScoringFunctionFactory {
	
	private Network network;
	
	public MyCarrierScoringFunctionFactoryImpl(Network network) {
		this.network = network;
	}

	@Override
	public ScoringFunction createScoringFunction(Carrier carrier) {
		CarrierScoringFunctionFactoryImpl carrierScoringFuncFactImpl = new CarrierScoringFunctionFactoryImpl(network);
		ScoringFunction carrierScoringFunc = carrierScoringFuncFactImpl.createScoringFunction(carrier);
		
		return carrierScoringFunc;
	}

}
