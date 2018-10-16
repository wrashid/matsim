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

package org.matsim.contrib.spatialDrt.run;

import org.matsim.contrib.spatialDrt.parkingStrategy.insertionOptimizer.DefaultUnplannedRequestInserter;
import org.apache.log4j.*;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.vis.otfvis.OTFVisConfigGroup;


import java.io.IOException;

/**
 * @author michalm
 */
public class RunDrtScenario {
	public static void run(String configFile, boolean otfvis) throws IOException {
		Config config = ConfigUtils.loadConfig(configFile, new AtodConfigGroup(),new DrtConfigGroup(), new DvrpConfigGroup(),
				new OTFVisConfigGroup());
        Logger.getLogger("org.matsim.core.mobsim.qsim.changeeventsengine.NetworkChangeEventsEngine").setLevel(Level.ERROR);
        Logger.getLogger(DefaultUnplannedRequestInserter.class).setLevel(Level.ERROR);
		createControler(config, otfvis).run();
	}

	public static Controler createControler(Config config, boolean otfvis) throws IOException {
		return DrtControlerCreator.createControler(config, otfvis);
	}

	public static void main(String[] args) throws IOException {
		RunDrtScenario.run(args[0], false);
	}
}
