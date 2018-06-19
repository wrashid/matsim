package org.matsim.contrib.pseudosimulation.searchacceleration.logging;

import org.matsim.contrib.pseudosimulation.searchacceleration.ReplannerIdentifier;

import floetteroed.utilities.statisticslogging.Statistic;

public class EffectiveReplanningRate implements Statistic<ReplannerIdentifier> {

	public static final String EFFECTIVE_REPLANNING_RATE = "EffectiveReplanningRate";
	
	@Override
	public String label() {
		return EFFECTIVE_REPLANNING_RATE;
	}

	@Override
	public String value(ReplannerIdentifier arg0) {
		return Statistic.toString(arg0.getEffectiveReplanninRate());
	}

}
