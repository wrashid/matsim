package org.matsim.contrib.pseudosimulation.searchacceleration.logging;

import org.matsim.contrib.pseudosimulation.searchacceleration.AccelerationAnalyzer;

import floetteroed.utilities.statisticslogging.Statistic;

public class DriversInPhysicalSim implements Statistic<AccelerationAnalyzer> {

	public static final String PHYSICAL_DRIVERS = "PhysicalDrivers";

	@Override
	public String label() {
		return PHYSICAL_DRIVERS;
	}

	@Override
	public String value(AccelerationAnalyzer arg0) {
		return Statistic.toString(arg0.getDriversInPhysicalSim());
	}

}
