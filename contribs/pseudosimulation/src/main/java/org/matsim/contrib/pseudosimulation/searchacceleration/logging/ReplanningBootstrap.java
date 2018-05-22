package org.matsim.contrib.pseudosimulation.searchacceleration.logging;

import java.util.List;

import org.matsim.contrib.pseudosimulation.searchacceleration.AccelerationAnalyzer;

import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.statisticslogging.Statistic;

public class ReplanningBootstrap implements Statistic<AccelerationAnalyzer> {

	public static final String REPLAN_BOOTSTRAP = "ReplanBootstrap";

	@Override
	public String label() {
		return REPLAN_BOOTSTRAP;
	}

	@Override
	public String value(AccelerationAnalyzer arg0) {
		final List<Double> bootstrap = arg0.getBootstrap();
		if ((bootstrap == null) || (bootstrap.size() == 0)) {
			return "";
		} else {
			final StringBuffer result = new StringBuffer(bootstrap.get(0).toString());
			for (int i = 1; i < bootstrap.size(); i++) {
				result.append("|");
				result.append(MathHelpers.round(bootstrap.get(i), 3));
			}
			return result.toString();
		}
	}
}
