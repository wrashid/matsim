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

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.mapMatching.utils.MapMatchingUtils;
import org.matsim.core.gbl.MatsimRandom;

import com.vividsolutions.jts.algorithm.Angle;
import com.vividsolutions.jts.geom.Coordinate;

public class PathGenerator {
	final private static Logger LOG = Logger.getLogger(PathGenerator.class);
	private static boolean seedSet = true;
	
	//TODO should also do a fastest network - where we look at speed - need to upgrade the shortest path formula in utilities
	
//	public static Network generteShortestStraigthPath(Network network, String directory){
//		Object[] nodeArray = network.getNodes().keySet().toArray();
//		Node fromNode = network.getNodes().get(nodeArray[0]);
//		Node toNode = network.getNodes().get(nodeArray[network.getNodes().keySet().size()-1]);
//		List<Link> links = Utilities.getPath(network, fromNode, toNode, true).links;
//		Network pathNetwork = Utilities.createNetworkFromLinks(network, links);
//		return pathNetwork;
//	}
	
	/**
	 * Generate a random path between randomly sampled nodes (origin and destination) in the network. <br><br>
	 * <b>NOTE:</b> This method should only be used for tests since you can control the random seed. For
	 * normal application, rather use {@link #generateRandomPath(Network, int)}. 
	 * 
	 * @param network
	 * @param numberOfTrips
	 * @param random
	 * @return
	 */
	public static List<Link> generateRandomPath(Network network, int numberOfTrips, Random random){
		boolean plural = numberOfTrips > 1;
			LOG.info("Generating " + numberOfTrips + " random path" + (plural ? "s" : ""));
		if(seedSet) {
			LOG.warn("The path" + (plural ? "s" : "") + " generated " + (plural ? "are" : "is") 
					+ " not truly random." );
			LOG.warn("Ignore this message only if execution is part of a test.");
		}
		List<Link> links = new ArrayList<Link>();
		Object[] nodeArray = network.getNodes().keySet().toArray();
		Node fromNode;
		Node toNode;
		int randomStart;
		int randomEnd;
		
		randomStart = (int) Math.round(network.getNodes().keySet().size()*random.nextDouble());
		for (int i = 0; i < numberOfTrips; i++) {
			randomEnd = (int) Math.round(network.getNodes().keySet().size()*random.nextDouble());
			fromNode = network.getNodes().get(nodeArray[randomStart]);
			toNode = network.getNodes().get(nodeArray[randomEnd]);
			
			links.addAll(MapMatchingUtils.getPath(network, fromNode, toNode, true).links);
			
			randomStart = randomEnd;
		}
		
		return links;
	}
	
	/**
	 * Generate a random path between randomly sampled nodes (origin and destination) in the network. 
	 * @param network
	 * @param numberOfTrips
	 * @return
	 */
	public static List<Link> generateRandomPath(Network network, int numberOfTrips){
		seedSet = false;
		List<Link> path = generateRandomPath(network, numberOfTrips, MatsimRandom.getRandom());
		seedSet = true;
		return path;
	}
	
	
	
	
	
	public static List<Link> generteRandomPath(Network network, String directory, long maxNumberOfUnorthodoxLinks, boolean randomNodes){
		Object[] nodeArray = network.getNodes().keySet().toArray();
		Node fromNode;
		Node toNode;
		if (randomNodes) {
			int randomStart = (int) Math.round(network.getNodes().keySet().size()*Math.random());
			int randomEnd = (int) Math.round(network.getNodes().keySet().size()*Math.random());
			//randomStart = 1;
			//randomEnd = 10000;
			fromNode = network.getNodes().get(nodeArray[randomStart]);
			toNode = network.getNodes().get(nodeArray[randomEnd]);
			
			/* Used for debugging and testing*/
			//fromNode = network.getNodes().get(Id.createNodeId("362837544"));
			//362842056 - start
			// at bend 362837544
			//3231244799 - did not work
			//toNode = network.getNodes().get(Id.createNodeId("1833552149"));
			
		}
		else {
			fromNode = network.getNodes().get(nodeArray[0]);
			toNode = network.getNodes().get(nodeArray[network.getNodes().keySet().size()-1]);
		}
		List<Link> links = new ArrayList<Link>();
		
		//Need to cycle through all the options available from the start choose one randomly - 
		//add it and continue until You get to the end
		long i = 0;
		do  {
			boolean assigned = false;
			ArrayList<Id<Link>> linkList = randowmiseLinks(fromNode.getOutLinks().keySet());
			Link intermediateLink;
			Node intermediateToNode;
			Link bestAlternativeLink = null;
			Node bestAlternativeNode = null;
			double bestAlternativeAngle = 0;
			Node uturnNode = null;
			for(Id<Link> linkId:linkList) {
			//for(Id<Link> linkId:fromNode.getOutLinks().keySet()) { /* Used for debugging and testing*/
				intermediateLink = network.getLinks().get(linkId);
				intermediateToNode = intermediateLink.getToNode();
				if (links.contains(intermediateLink)) {
					continue;
				}
				if (links.size() > 0 && links.get(links.size()-1).getFromNode() == intermediateToNode){
					// If the possible link is traversing back to the previous node, skip this option
					// but save it as an alternative should this be the only one available
					// works for utruns required at dead ends
					if (bestAlternativeLink == null) { // Only if there are no alternatives saved already then are you allowed to store the uturn as an alternative
						bestAlternativeLink = intermediateLink;
						bestAlternativeNode = intermediateToNode;
					}
					uturnNode = intermediateToNode;
					continue; 
				}
				
 				Coordinate tail = new Coordinate(intermediateToNode.getCoord().getX(), intermediateToNode.getCoord().getY());
				Coordinate tip1 = new Coordinate(fromNode.getCoord().getX(), fromNode.getCoord().getY());
				Coordinate tip2 = new Coordinate(toNode.getCoord().getX(), toNode.getCoord().getY());
				
				double angle = Angle.angleBetween(tip1, tail, tip2);
				if (angle >= Math.PI/2){
					links.add(intermediateLink);
					fromNode = intermediateToNode;
					assigned = true;
					break;
				}
				else if (angle > bestAlternativeAngle)  {
					bestAlternativeLink = intermediateLink;
					bestAlternativeNode = intermediateToNode;
					bestAlternativeAngle = angle;
				}
			}
			
			/*
			 * Have option to generate random speed or adjust the speeds before saving it to the links
			 */
			
			
			if (!assigned) { //If there was no link found to adhere to the greater than 90' angle just take the first one of the random set
				links.add(bestAlternativeLink);
				fromNode = bestAlternativeNode;
				assigned = true;
				i++ ;
			}
			
			if (fromNode == uturnNode){
				System.out.println("Uturn");
			}
			System.out.println(i);
			if (fromNode == null) {
				i = maxNumberOfUnorthodoxLinks;
			}
			else if (fromNode.equals(toNode)){
				i = maxNumberOfUnorthodoxLinks;
			}
		} while ( i<maxNumberOfUnorthodoxLinks);
		if ( i >= maxNumberOfUnorthodoxLinks ){ 
			System.out.println("Exit path generation as max number of links has been reached");
		}
			
		Network pathNetwork = MapMatchingUtils.createNetworkFromLinks(network, links);
		if (directory != null){
			new NetworkWriter(pathNetwork).write(directory + "/truePathNetwork.xml");
			
			
			MapMatchingUtils.writeNetworktoCSV(directory + "/truePathNetwork.csv", pathNetwork);
		}
		return links;
	}
	
	@SuppressWarnings("unchecked")
	private static ArrayList<Id<Link>> randowmiseLinks(Set<Id<Link>> set){
		ArrayList<Id<Link>> returnList = new ArrayList<Id<Link>>();
		Object[] linkList = set.toArray();
		int size = set.size();
		if (Math.random() > 0.5) {
			for (int i = 0; i< size; i++){
				returnList.add((Id<Link>) linkList[(size-1)-i]);
			}
		}
		else{
			for (int i = 0; i< size; i++){
				returnList.add((Id<Link>) linkList[i]);
			}
		}
		int iterations = (int) Math.round(Math.random()*10);
		for (int i = 0; i < iterations; i ++){
			int position1 = Math.min((int) Math.round(Math.random()*size), size - 1);
			Id<Link> tempLink = returnList.get(position1);
			returnList.remove(position1);
			returnList.add(tempLink);
		}
		
		return returnList;
	}
	

}

