/* *********************************************************************** *
 * project: org.matsim.contrib.mapMatching.graph*
 * GraphCreator.java
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
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.mapMatching.utils.MapMatchingUtils;


public class AccuracyCalculator {
	
	public static boolean saveSpeedAnalysis = true;
	
	
	static List<String[]> combinedAnalysis = new ArrayList<String[]>(){{
		add(new String[]{"Name", "Type", "Value"});
	}};
	
	static HashMap<String, ArrayList<String[]>> combinedChosenPathSpeedAnalysis = new HashMap<String, ArrayList<String[]>>();
	
	
	// Main methods
		// Route analysis
	
	public static ArrayList<String[]> analyseRoute(List<Link> truePath, List<List<Link>> inferredPath, List<String> ID
			, ArrayList<String[]> truePathIDsInOrder, List<ArrayList<String[]>> inferredPathIDsInOrder ){
		if (inferredPath.size() != ID.size()){
			throw new RuntimeException("List of routes to be compared for the route analysis must be of equal size to list of IDs");
		}
		ArrayList<String[]>  efficiencRatios = new ArrayList<String[]>();
		for (int i =0; i < inferredPath.size(); i++) {
			efficiencRatios.addAll(analyseRoute(truePath, inferredPath.get(i), ID.get(i), truePathIDsInOrder, inferredPathIDsInOrder.get(i) ));
		}
		combinedAnalysis.addAll(efficiencRatios);
		return efficiencRatios;
	}
	

	
	/*public static void analyseRoute(String truePathNetworkLocation, String inferredPathNetworkLocation){
		//TODO convert string to network and call network method
	}*/
	
	public static ArrayList<String[]> analyseRoute(List<Link> truePath, List<Link> inferredPath, String ID
			, ArrayList<String[]> truePathIDsInOrder, ArrayList<String[]> inferredPathIDsInOrder){
		return AccuracyCalculator.routeRatios(truePath, inferredPath, ID, truePathIDsInOrder, inferredPathIDsInOrder);

	}
	
		// Speed Analysis
	
	public static void savedSpeednalysis( ArrayList<String[]> chosenPathSpeed, String scenarioID) {
		if (saveSpeedAnalysis) combinedChosenPathSpeedAnalysis.put(scenarioID, chosenPathSpeed);
	}
	
	
	public static void writeOutAccuracyData(String directory){
		MapMatchingUtils.writeDataToCSV(directory + "/Accuracy Data.csv", combinedAnalysis);
	}
	
	public static void writeOutSpeedAccuracyData(String directory) {
		addLinkEntryAndExitTime();
		List<String[]> linkSpeedList = new ArrayList<String[]>(){{
			add(new String[]{"Numb", "ExperimentName", "LinkID", "Length", "CalculatedSpeed", "ActualSpeed", "DeltaN", "DeltaP", "Link Enter Time", "Link Exit Time"});
		}};
		
		for (String id:combinedChosenPathSpeedAnalysis.keySet()) {
			int i = 0;
			for (String[] entry:combinedChosenPathSpeedAnalysis.get(id) ) {
				double speedDelta = Double.valueOf(entry[2]) - Double.valueOf(entry[3]);
				linkSpeedList.add(new String[]{
				String.valueOf(i),
				id,
				entry[0],
				entry[1],
				entry[2],
				entry[3],
				String.valueOf(speedDelta),
				String.valueOf(speedDelta/Double.valueOf(entry[2])),
				entry[4],
				entry[5]
				});
				i ++;
			}
		}
		
		
		MapMatchingUtils.writeDataToCSV(directory + "/Speed Data.csv", linkSpeedList);
	}
	
	private static void addLinkEntryAndExitTime(){
		for (String id:combinedChosenPathSpeedAnalysis.keySet()) {
			int i = 0;
			String[] previousEntry = null;
			for (String[] entry:combinedChosenPathSpeedAnalysis.get(id) ) {
				double distance = Double.valueOf(entry[1]);
				double speed = Double.valueOf(entry[2]);
				//Save the Entry time
				if ( i == 0) {
					entry[4] = String.valueOf(0);
				}
				else {
					entry[4] = previousEntry[5];
				}
				
				//Save the Exit time
				entry[5] = String.valueOf(Double.valueOf(entry[4]) + distance/speed);
				
				previousEntry = entry;
			i ++;
			}
		}
	}
	
	
	// Analysis methods - Might be changed to private
	
	protected static ArrayList<String[]> routeRatios(List<Link> truePath, List<Link> inferredPath, String ID
			, ArrayList<String[]> truePathIDsInOrder, ArrayList<String[]> inferredPathIDsInOrder){
		
		ArrayList<Link> correctlyMatchedLinks = new ArrayList<Link>();
		ArrayList<Link> incorrectlyMatchedLinks = new ArrayList<Link>();
		
		//PReviously used the networks and could compare IDs now that it is links, they are from different networks and the link objects can be compared even though they share the same ID
		// This issue will only be in the experimental networks where the base network is the same
		
		// Find incorrectly matched links in inferred path
		for (Link iLink : inferredPath){
			boolean found = false;
			for (Link tLink : truePath) {
				if (iLink.getId().toString().equals(tLink.getId().toString())){
					found = true;
					break;
				}
			}
			if (!found) {
					incorrectlyMatchedLinks.add(iLink);
			}
		}
		
		//Find Correctly matched route for the true path
		for (Link tLink : truePath ){
			for (Link iLink : inferredPath) {
				if (iLink.getId().toString().equals(tLink.getId().toString())){
					correctlyMatchedLinks.add(iLink);
					break;
				}
			}
		}
		
		double lengthOfTruePath = getLenthOfLinkList(truePath);
		double lenthOfCorrectlyMatchedRoute= getLenthOfLinkList(correctlyMatchedLinks);
		double lenthOfInCorrectlyMatchedRoute= getLenthOfLinkList(incorrectlyMatchedLinks);
		
		
		// Order of output - ARR, IARR, ARRn, Al
		ArrayList<String[]> outputString = new ArrayList<String[]>();
		outputString .add(new String[] {ID, "ARR", String.valueOf((double)lenthOfCorrectlyMatchedRoute/lengthOfTruePath)});
		outputString .add(new String[] {ID, "IARR", String.valueOf((double)lenthOfInCorrectlyMatchedRoute/lenthOfCorrectlyMatchedRoute)});
		outputString .add(new String[] {ID, "ARRn", String.valueOf((double)correctlyMatchedLinks.size()/truePath.size())});
		outputString .add(new String[] {ID, "Al", 
					String.valueOf((double)findLongestCommonSeries(truePathIDsInOrder, truePath , inferredPathIDsInOrder)
							/lengthOfTruePath)});
		
		return outputString;
	}
	
	/*protected static String[] generateHeaders(){
		String[] outputString = new String[]{ "ID", "ARR", "IARR", "ARRn", "Al"};
		return outputString;
	}*/
	
	// Ancillary methods ----------------------------------------------------------------------------------

	private static double getLenthOfLinkList(List<Link> truePath) {
		double x = 0;
		for (Link link:truePath){
			x += link.getLength();
		}
		return x;
	}
	
	
	private static double findLongestCommonSeries(ArrayList<String[]> truePathIDsInOrder, List<Link> truePath, ArrayList<String[]> inferredPathIDsInOrder) {
		//Create a Hash set so that it can be used in the longest common path calculator
		Set<Id<Link>> truePathIDSet = new HashSet<Id<Link>>();
		for (Link link:truePath){
			truePathIDSet.add(link.getId());
		}
		
		 
		double longetCommonPathLength = 0;
		int i = 0;
		String startingLinkInferredPath;
		
 		for (int counter = 0; counter < inferredPathIDsInOrder.size(); counter ++){
			startingLinkInferredPath = inferredPathIDsInOrder.get(counter)[0];
			double tempPathLength = 0;
			ListIterator<String[]> truePathIterator = truePathIDsInOrder.listIterator();
			if (truePathIDSet.contains(Id.createLinkId(startingLinkInferredPath)) ){
				while (truePathIterator.hasNext()) {
					if (truePathIterator.next()[0].equals(startingLinkInferredPath)){
						break;
					}
				}
				// Now that a starting point has been found where both lists of paths share a common segment 
				// we can try and finding the longest sequential segment starting with this segment
				//tempPath.add(startingLinkInferredPath); - otherwise the first one seems to be added in duplicate
				ListIterator<String[]> inferredPathIterator = inferredPathIDsInOrder.listIterator(i);
				truePathIterator.previous();
				while (truePathIterator.hasNext() && inferredPathIterator.hasNext()) {
					if (truePathIterator.next()[0].equals(inferredPathIterator.next()[0])) {
						tempPathLength += Double.parseDouble(inferredPathIDsInOrder.get(inferredPathIterator.previousIndex())[11]); // This is not very robust - need to access the speed from links
					}
					else {
						break;
					}
				}
			}
			if (tempPathLength > longetCommonPathLength){
				longetCommonPathLength = tempPathLength
;
			}
			i++;
		}
		return longetCommonPathLength;
	}

}
