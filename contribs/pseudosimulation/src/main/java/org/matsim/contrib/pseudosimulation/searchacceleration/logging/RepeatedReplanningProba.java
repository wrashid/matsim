package org.matsim.contrib.pseudosimulation.searchacceleration.logging;

import org.matsim.contrib.pseudosimulation.searchacceleration.AccelerationAnalyzer;

import floetteroed.utilities.statisticslogging.Statistic;

public class RepeatedReplanningProba implements Statistic<AccelerationAnalyzer> {

	public static final String REPEATED_REPLANNING_PROBA = "RepeatedReplanningProba";

	@Override
	public String label() {
		return REPEATED_REPLANNING_PROBA;
	}

	@Override
	public String value(AccelerationAnalyzer arg0) {
		return Statistic.toString(arg0.getRepeatedReplanningProba());
	}

}
