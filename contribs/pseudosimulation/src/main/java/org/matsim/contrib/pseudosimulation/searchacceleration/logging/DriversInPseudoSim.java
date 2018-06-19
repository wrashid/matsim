package org.matsim.contrib.pseudosimulation.searchacceleration.logging;

import org.matsim.contrib.pseudosimulation.searchacceleration.ReplannerIdentifier;

import floetteroed.utilities.statisticslogging.Statistic;

public class DriversInPseudoSim implements Statistic<ReplannerIdentifier> {

	public static final String PSIM_DRIVERS = "PseudoSimDrivers";

	@Override
	public String label() {
		return PSIM_DRIVERS;
	}

	@Override
	public String value(ReplannerIdentifier arg0) {
		return Statistic.toString(arg0.getDriversInPseudoSim());
	}

}
