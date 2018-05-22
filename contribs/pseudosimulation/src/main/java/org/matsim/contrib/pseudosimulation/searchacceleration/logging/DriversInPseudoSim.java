package org.matsim.contrib.pseudosimulation.searchacceleration.logging;

import org.matsim.contrib.pseudosimulation.searchacceleration.AccelerationAnalyzer;

import floetteroed.utilities.statisticslogging.Statistic;

public class DriversInPseudoSim implements Statistic<AccelerationAnalyzer> {

	public static final String PSIM_DRIVERS = "PseudoSimDrivers";

	@Override
	public String label() {
		return PSIM_DRIVERS;
	}

	@Override
	public String value(AccelerationAnalyzer arg0) {
		return Statistic.toString(arg0.getDriversInPseudoSim());
	}

}
