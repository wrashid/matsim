/* *********************************************************************** *
 * project: org.matsim.contrib.mapMatching.matching*
 * EfficiencyCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.mapMatching.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.contrib.mapMatching.utils.MapMatchingUtils;

public class EfficiencyCalculator {
	static List<String[]> timings = new ArrayList<String[]>(){{
		add(new String[]{"Name", "Activity", "Duration", "Units"});
	}};
	static HashMap<Object, MMListener> objectMap = new HashMap<Object, MMListener>();
	
	public static void logStart(Object object, String activity) {
		if (objectMap.containsKey(object)) {
			objectMap.get(object).logStart(activity);
		}
		else {
			MMListener listener = new MMListener(object);
			objectMap.put(object, listener);
			listener.logStart(activity);
		}
	}
	public static void logEnd(Object object, String activity, double units) {
		if (objectMap.containsKey(object)) {
			timings.add(objectMap.get(object).logEnd(activity, units));
		}
		else {
			throw new RuntimeException("Cannot log an end time if this object has no previous activities logged");
		}
	}
	
	public static void writeOutEfficiencyData(String directory){
		MapMatchingUtils.writeDataToCSV(directory + "/EfficiencyData.csv", timings);
	}
	public static double getLastDuration(){
		return Double.valueOf(timings.get(timings.size()-1)[2]);
	}
	
	public static List<String[]> getEfficiencyList() {
		return timings;
	}
	
	// Enum Class not used at the moment //TODO can specify exactly the activity names that can be recorded
	public enum Activities{
		
	}
	static class MMListener {
		private HashMap<String, Long> activityStartList = new HashMap<String, Long>();
		private Object object;
		private Logger LOG;
		
		protected MMListener(Object object){
			this.object = object;
			LOG = Logger.getLogger(object.toString());
		}
		protected void logStart(String activity) {
			if (activityStartList.containsKey(activity)) {
				throw new RuntimeException("There is a duplicate activity being recorded for this object");
			}
			activityStartList.put(activity, System.nanoTime());
			LOG.info(activity + " - Start");
			
		}
		protected String[] logEnd(String activity, double units) {
			long startTime = activityStartList.get(activity);
			long endTime =  System.nanoTime(); 
			LOG.info(activity + " - End");
			
			return new String[] {
				object.toString(),
				activity,
				//String.valueOf((startTime/1000000)),
				//String.valueOf((endTime/1000000)),
				String.valueOf((endTime - startTime)/1000000),
				String.valueOf(units)
			};
		}
	}
	
}
