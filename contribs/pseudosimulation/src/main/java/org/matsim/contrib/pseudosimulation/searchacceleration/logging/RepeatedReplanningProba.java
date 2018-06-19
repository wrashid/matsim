package org.matsim.contrib.pseudosimulation.searchacceleration.logging;

import org.matsim.contrib.pseudosimulation.searchacceleration.ReplannerIdentifier;

import floetteroed.utilities.statisticslogging.Statistic;

public class RepeatedReplanningProba implements Statistic<ReplannerIdentifier> {

	public static final String REPEATED_REPLANNING_PROBA = "RepeatedReplanningProba";

	@Override
	public String label() {
		return REPEATED_REPLANNING_PROBA;
	}

	@Override
	public String value(ReplannerIdentifier arg0) {
		return Statistic.toString(arg0.getRepeatedReplanningProba());
	}

}
