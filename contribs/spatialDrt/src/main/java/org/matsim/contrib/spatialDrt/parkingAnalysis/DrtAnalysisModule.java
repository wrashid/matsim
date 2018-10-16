/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package org.matsim.contrib.spatialDrt.parkingAnalysis;


import org.matsim.contrib.drt.analysis.DrtAnalysisControlerListener;
import org.matsim.contrib.drt.analysis.DrtRequestAnalyzer;
import org.matsim.contrib.drt.analysis.DynModePassengerStats;
import org.matsim.contrib.drt.util.stats.DrtVehicleOccupancyProfileWriter;
import org.matsim.core.controler.AbstractModule;

/**
 * @author jbischoff
 *
 */
public class DrtAnalysisModule extends AbstractModule {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.matsim.core.controler.AbstractModule#install()
	 */
	@Override
	public void install() {
		bind(DynModePassengerStats.class).asEagerSingleton();
		bind(DrtRequestAnalyzer.class).asEagerSingleton();
		bind(DemandSupplyAnalysis.class).asEagerSingleton();
		addControlerListenerBinding().to(DemandSupplyAnalysis.class).asEagerSingleton();
		addControlerListenerBinding().to(DrtAnalysisControlerListener.class).asEagerSingleton();
		addMobsimListenerBinding().to(DrtVehicleOccupancyProfileWriter.class);
	}

}
