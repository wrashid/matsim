/* *********************************************************************** *
 * project: org.matsim.contrib.mapMatching.experiment.TraceGenerator.java  *
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

package org.matsim.contrib.mapMatching.experiment;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.mapMatching.trace.GpsCoord;
import org.matsim.contrib.mapMatching.utils.MapMatchingUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.io.IOUtils;

import com.vividsolutions.jts.algorithm.Angle;

public class TraceGenerator {
	final private static Logger LOG = Logger.getLogger(TraceGenerator.class);

	/* should not make use of a network as then you cant have the same link traversed more than once in the route
	 *  I only keep this method as it is easier to save the truepath as a network for now - and then I can read it in to be used again in experiments
	 */
	/**
	 * Generates a synthetic GPS trace.<br><br>
	 * <b>Note:</b> This method is only useful if the network represents a <i>true path</i>, similar to 
	 * what was generated from {@link PathGenerator} and converted to a network using 
	 * {@link MapMatchingUtils#createNetworkFromLinks(Network, List)}.
	 * 
	 * @param network
	 * @param period
	 * @param gpsError
	 * @return
	 */
	public static List<GpsCoord> generateGpsTrace (Network network, double period, double gpsError){
		List<Link> links = MapMatchingUtils.convertSetIntoList(network.getLinks().keySet(), network);
		return generateGpsTrace(links, period, gpsError);
	}
	
	
	/**
	 * Generates a synthetic GPS trace.<br><br>
	 * <b>NOTE:</b> This class is meant to be deterministic and should only be used for tests. Instead, 
	 * use {@link #generateGpsTrace(List, double, double)}
	 * 
	 * @param links
	 * @param period
	 * @param gpsError
	 * @param random
	 * @return
	 */
	public static List<GpsCoord> generateGpsTrace (List<Link> links, double period, double gpsError, Random random){
		List<GpsCoord> gpsCoords = new ArrayList<>();
		double x;
		double y;
		double globalTime = 0;
		double leftOverTime = 0;
		for(Link link:links){
			double localTime = -leftOverTime; 
			double distanceOnLink;
			while (link.getFreespeed() * (localTime + period) < link.getLength()) {
				localTime += period;
				distanceOnLink = link.getFreespeed() * localTime;
				double a = Angle.angle(
						MapMatchingUtils.convertFromCoordToCoordinate(link.getFromNode().getCoord()),
						MapMatchingUtils.convertFromCoordToCoordinate(link.getToNode().getCoord()));
				x = Math.cos(a) * distanceOnLink + link.getFromNode().getCoord().getX() + random.nextDouble()*2*gpsError-gpsError ;
				y = Math.sin(a) * distanceOnLink + link.getFromNode().getCoord().getY()+ random.nextDouble()*2*gpsError-gpsError;
				GpsCoord	 gc = new GpsCoord(x, y, globalTime+localTime);
				gpsCoords.add(gc);
			} 		
			globalTime += link.getLength() / link.getFreespeed();
			leftOverTime = link.getLength() / link.getFreespeed() - localTime;
		}
		return gpsCoords;
	}
	
	/**
	 * Generates a synthetic GPS trace.
	 * @param links
	 * @param period
	 * @param gpsError
	 * @return
	 */
	public static List<GpsCoord> generateGpsTrace (List<Link> links, double period, double gpsError){
		return generateGpsTrace(links, period, gpsError, MatsimRandom.getLocalInstance());
	}
	
	
	
	

	// Utilities -------------------------------------------------
	public static ArrayList<String[]> extractPeriod (ArrayList<String[]> gpsPoints, double period, String directory){
		ArrayList<String[]> newGPSpoints = new ArrayList<String[]>();
		double currentPeriod = Double.valueOf(gpsPoints.get(1)[2]) - Double.valueOf(gpsPoints.get(0)[2]);
		if ( currentPeriod > period){
			throw new RuntimeException("Cannot alter period as the current period of GPS trajectory is more than requested period");
		}
		int interval = (int) Math.round(period/currentPeriod);

		for (int i=0; i < gpsPoints.size(); i++){
			if (i%interval == 0) {
				newGPSpoints.add(gpsPoints.get(i));
			}
		}
		MapMatchingUtils.writeDataToCSV(directory + "/" + period + "s.csv", newGPSpoints);
		return newGPSpoints;
	}

	public static List<ArrayList<String[]>> extractPeriod (ArrayList<String[]> gpsPoints, double[] periods, String directory){
		List<ArrayList<String[]>> listOfGPSpoints = new ArrayList<ArrayList<String[]>> ();
		for (int i = 0; i < periods.length; i++ ) {
			listOfGPSpoints.add(extractPeriod(gpsPoints, periods[i], directory));
		}
		return listOfGPSpoints;
	}
	
	public static void writeGpsTraceToFile(List<GpsCoord> trace, String filename) {
		LOG.info("Writing GPS trace to file " + filename);
		BufferedWriter bw = IOUtils.getBufferedWriter(filename);
		try {
			for(GpsCoord gc : trace) {
				bw.write(String.format("%f,%f,%f\n", gc.getCoord().getX(), gc.getCoord().getY(), gc.getTime()));
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + filename);
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + filename);
			}
		}
	}

}
