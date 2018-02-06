/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateScenario.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.mapMatching.experiment;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.mapMatching.trace.GpsCoord;
import org.matsim.contrib.mapMatching.utils.Header;
import org.matsim.contrib.mapMatching.utils.MapMatchingUtils;
import org.matsim.core.network.io.NetworkWriter;

/**
 * Class to generate experimental data for the map-matching algorithm. Once the scenario elements are 
 * generated, it is also written to file.
 * 
 * @author jwjoubert
 */
public class GenerateScenario {
	final private static Logger LOG = Logger.getLogger(GenerateScenario.class);

	/**
	 * Executes the scenario generation. The following arguments are required and in the specific order:
	 * <ol>
	 * 		<li> number of network nodes in the x-dimension;
	 * 		<li> number of network nodes in the y-direction;
	 * 		<li> length of links;
	 * 		<li> time between GPS trace points (frequency);
	 * 		<li> error (wiggle) of GPS trace data (typically in meter);
	 *      <li> absolute path of the directory where the output is to be written
	 * </ol>
	 * If these arguments are not passed, then a predefined scenario is being built and written to the
	 * output folder. 
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(GenerateScenario.class, args);
		if(args.length == 5) {
			run(args);
		} else {
			runSmallExample();
		}
		Header.printFooter();
	}
	
	public static void run(String[] args) {
		int nodesX = Integer.parseInt(args[0]);
		int nodesY = Integer.parseInt(args[1]);
		int linkLength = Integer.parseInt(args[2]);
		double gpsTraceTime = Double.parseDouble(args[3]);
		double gpsTraceError = Double.parseDouble(args[4]);
		String path = args[5];
		
		path += path.endsWith("/") ? "" : "/";
		File folder = new File(path);
		if(folder.exists()) {
			LOG.error("Output folder " + path + " exists and will not be overwritten");
			throw new IllegalArgumentException(" First delete output folder");
		}
		folder.mkdirs();
		
		/* Generate the network. */
		Network network = NetworkGenerator.generateGrid(nodesX, nodesY, linkLength);
		new NetworkWriter(network).write(path + "network.xml.gz");
		
		/* Generate a random path. */
		List<Link> randomPath = PathGenerator.generateRandomPath(network, 1);
		Network pathNetwork = MapMatchingUtils.createNetworkFromLinks(network, randomPath);
		new NetworkWriter(pathNetwork).write(path + "truePathNetwork.xml.gz");
		MapMatchingUtils.writeNetworktoCSV(path + "truePath.csv.gz", pathNetwork);
		
		/* Generate GPS trace on known path. */
		List<GpsCoord> trace = TraceGenerator.generateGpsTrace(randomPath, gpsTraceTime, gpsTraceError);
		TraceGenerator.writeGpsTraceToFile(trace, path + "trace.csv.gz");
		
	}
	
	private static void runSmallExample() {
		String[] args = {"10", "20", "50", "10.0", "15.0", "./output/experiment/"};
		run(args);
	}
	

}
