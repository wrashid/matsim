package org.matsim.contrib.pseudosimulation.searchacceleration.logging;

import org.matsim.contrib.pseudosimulation.searchacceleration.AccelerationAnalyzer;

import floetteroed.utilities.statisticslogging.Statistic;

public class ShareNeverReplanned implements Statistic<AccelerationAnalyzer> {

	public static final String SHARE_NEVER_REPLANNED = "ShareNeverReplanned";
	
	@Override
	public String label() {
		return SHARE_NEVER_REPLANNED;
	}

	@Override
	public String value(AccelerationAnalyzer arg0) {
		return Statistic.toString(arg0.getShareNeverReplanned());
	}

}
