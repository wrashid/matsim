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

package org.matsim.contrib.mapMatching.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.mapMatching.evaluation.EfficiencyCalculator;
import org.matsim.contrib.mapMatching.graph.CandidateLink;
import org.matsim.contrib.mapMatching.graph.GraphCreator;
import org.matsim.contrib.mapMatching.graph.PossibleCandidateLink;
import org.matsim.contrib.mapMatching.trace.GpsCoord;
import org.matsim.contrib.mapMatching.utils.MapMatchingUtils;
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.utils.collections.QuadTree;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.io.File;


/**
 * Overall procedure:
 * <ol>
 * 		<li> Preparation.
 * 		<ol>
 * 			<li> Read in the MATSim network.
 * 		</ol>
 * </ol>
 * @author JBVosloo, 
 */
public class MapMatcher {
	final static Logger LOG = Logger.getLogger(MapMatcher.class);
	private Network network;
	private QuadTree<Id<Node>> qd;
	private double gpsErrorStandardDeviation;
	private double radius;
	private double gpsErrorMean;
	public String directory;
	public int numberOfLinks;
	private int matchCounter = 0;
	public String name = null; //used to change the ToString code for saving efficiency data
	
	
	/*
	 * Program steps
	 * 1) Pre-work
	 * 		1.1) Read in the MatSim network
	 * 		1.2) Save the network nodes into an ArrayList - For initial programming only - 
	 *           TODO - will be removed later
	 * 		1.3) Save the network points into a quad tree
	 * 		1.4) Read in a csv file with a GPS trajectory (Set of coordinates for specific vehicles route) 
	 *           and store in an arrayList by creating instances of the custom class GPSpoint
	 * 2) Map matching
	 * 		2.1) Find all the candidate links for each GPS point based on a set distance (x) from the GPS 
	 *           point.
	 * 			2.1.1) Find all the nodes within a 2x the set distance using the quad tree
	 * 			2.1.2) Cycle through all the possible links, all the in and out links of the possible nodes 
	 *                 identified, to find those that are within the set distance (x)
	 * 				   TODO need to incorporate the direction of the links so that links that does not 
	 *                 match the direction of travel be excluded (This can perhaps also be done on a 
	 *                 probability basis.
	 * 			2.1.3) Save the link and the distance from the point to the link for each GPS point inside 
	 *                 the custom class.
	 * 		2.2) Spatial analysis
	 * 			2.2.1) Determine observation and transmission probability (spatial) this should perhaps be        
	 *                 done for each link and saved to the custom GPS point for each point.
	 * 			2.2.2) Determine the temporal probability (Temporal) - this to be done for each link during 
	 *                 the routing phase in the graph network.
	 * 			2.2.3) Assign an ST value based on the probabilities to each candidate link.
	 * 		2.3) Result matching
	 * 			2.3.1) Construct a graph of all possible candidate links.
	 * 			2.3.2) Find the routes from start to end of trajectory with the highest ST value.
	 */
	
	/**
	 * Constructor details
	 * @param inputNetworkName
	 * @param standardDeviationGPSerror this should be known/given for the GPS
	 * 		  data set, for example 15m.
	 * @param meanGPSerror similar to above.
	 * @param numberOfLinks to be considered as candidates per GPS point.
	 * @param directory absolute path of the location where output is written to.
	 */
	public MapMatcher(String inputNetworkName, double standardDeviationGPSerror, double meanGPSerror, int numberOfLinks, String directory, String name) {
		network = MapMatchingUtils.parseNetwork(inputNetworkName);
		mapMatcherConstructor(network, standardDeviationGPSerror, meanGPSerror, numberOfLinks, directory, name);
	}
	public MapMatcher(Network network, double standardDeviationGPSerror, double meanGPSerror, int numberOfLinks, String directory, String name) {
		mapMatcherConstructor(network, standardDeviationGPSerror, meanGPSerror, numberOfLinks, directory, name);
	}
	public MapMatcher(Network network, double standardDeviationGPSerror, double meanGPSerror, int numberOfLinks, String directory) {
		mapMatcherConstructor(network, standardDeviationGPSerror, meanGPSerror, numberOfLinks, directory, null);
	}
	
	private void mapMatcherConstructor(Network network,  double standardDeviationGPSerror, double meanGPSerror, int numberOfLinks, String directory, String name) {
		this.network = network;
		this.gpsErrorStandardDeviation = standardDeviationGPSerror;
		this.gpsErrorMean = meanGPSerror;
		this.numberOfLinks = numberOfLinks;
		this.directory = directory;
		this.name = name;
		
		// The initial search radius is based on the length of the longest link in the network - 
		//this is to ensure that even on the longest link the correct candidate link will be chose
		
		for(Id<Link> linkId : network.getLinks().keySet() ) {
				radius = Math.max(network.getLinks().get(linkId).getLength(), radius);
		}
		
		//1.3) Create quad tree as it may be used multiple times for different trajectories
		EfficiencyCalculator.logStart(this, "Create Quad Tree");
		qd = MapMatchingUtils.createQuadTree(network);
		EfficiencyCalculator.logEnd(this, "Create Quad Tree", network.getNodes().size()); // We add the nodes as units since quad tree does not consider links, only nodes/points

	}

	// Main Methods ------------------------------------------------------------------------------------
	
	/**
	 * Alternative method for mapping a GPS trajectory unto the network provided curing instantiation of the MapMatcher object
	 * @param gpsTrajectories - a list of GPS trajectories to be matched onto the network
	 * @return The inferred path as a network
	 */
	public List<List<Link>> mapGPStrajectory(List<ArrayList<String[]>> gpsTrajectories, String[] trajectoryFileNames) {
		List<List<Link>> listOfInferredPaths = new ArrayList<List<Link>>();
		
		
		// Create separate folders for each experiment
		String originalDirectory = directory;
		int i = 0;
		String tempName = name;
		for (ArrayList<String[]> gpsTrajectory:gpsTrajectories){
			directory = originalDirectory + "/" + trajectoryFileNames[i];
			File file = new File(directory);
			file.mkdirs();
			name = tempName + "," + trajectoryFileNames[i];
			listOfInferredPaths.add(mapGPStrajectory(gpsTrajectory));
			matchCounter++;
			i ++;
		}
		name = tempName;
		//EfficiencyCalculator.writeOutEfficiencyData(originalDirectory);
		return listOfInferredPaths;
	}
	
	/* TODO find out why is there issues with overloading when types are the same but arguments are actually
	 * different
	 */
	/**
	 * Alternative method for map matching a GPS trajectory unto the network provided curing instantiation of the MapMatcher object
	 * @param inputGPStrajectory - The location within the root folder of the CSV containing the GPS data in the format "x", "y", "time"
	 * @return The inferred path is returned in a network object where the links and nodes in the network refer to the original network
	 * @throws IOException
	 */
	public List<Link> mapGPStrajectory(String inputGPStrajectory) {
		/* Read in the gps trajectory then call the mapMatcher*/
		//EfficiencyCalculator.writeOutEfficiencyData(directory);
		return mapGPStrajectory(MapMatchingUtils.parseToArrayfromCSV(inputGPStrajectory));
		
	}
	
	/**
	 * The main method for matching a GPS trajectory to a network
	 * @param gpsTrajectory - The requirement is an array list of Strings[] in the format "x", "y", "time"
	 * @return The inferred path is returned in a network object where the links and nodes in the network refer to the original network.
	 */
	public List<Link> mapGPStrajectory(List<String[]> gpsTrajectory) {
		
		// Create a graph creator class that we can use later to crate a graph of all the alternative routes and find the most probable one
		GraphCreator graphCreator = new GraphCreator();
		
		EfficiencyCalculator.logStart(this, "MapGPSTrajectory" + matchCounter);
		
		/* Setup the lists to be written. */
		ArrayList<String[]> gpsMatchedTrajectory = new ArrayList<String[]>(); 
		gpsMatchedTrajectory.add(new String[]{"x", "y","time","Nearest Link","CandidateLink", "Distance To Link", "Observation Probability" });
		
		//Create a new instance of the GPS class for each point and save into the list
		ArrayList<GpsCoord> gpsPointList = new ArrayList<GpsCoord>();
		for(String[] gpsPoint:gpsTrajectory){
			gpsPointList.add(new GpsCoord(Double.valueOf(gpsPoint[0]), Double.valueOf(gpsPoint[1]), Double.valueOf(gpsPoint[2]), network));
		}
		
		//2) Map Matching
			//2.1)
		for( GpsCoord gpsPoint:gpsPointList){
				//2.1.1)
			GeometryFactory gf = new GeometryFactory();
			Collection<Id<Node>> possibleNodes = null;
			Collection<Id<Link>> possibleLinks = new ArrayList<Id<Link>>() ;
			double tempRadius = radius;
			do {
				/* TODO This might fall away if link length is given as radius */ 
				possibleNodes = qd.getDisk(gpsPoint.getCoord().getX(), gpsPoint.getCoord().getY(), tempRadius);
				tempRadius += radius/2;
			} while( possibleNodes.size() == 0);;
			
			for(Id<Node> nodeId:possibleNodes){
				Node node = network.getNodes().get(nodeId);
				possibleLinks.addAll(node.getInLinks().keySet());
				possibleLinks.addAll(node.getOutLinks().keySet());
			}
			
			/* Convert GPS node to Vividsolutions Point and check distance from GPS point to link/segment using linestring. 
			 * If found to be within range - then add to possible link set*/
			Point point = gf.createPoint(new Coordinate(gpsPoint.getCoord().getX(), gpsPoint.getCoord().getY()));
			HashSet<Id<Link>> uniqueSetOfLinks = new HashSet<Id<Link>>();
			List<PossibleCandidateLink> possibleCandidateLinks = new ArrayList<PossibleCandidateLink>();
			for (Id<Link> linkId:possibleLinks){
				if (!uniqueSetOfLinks.contains(linkId)){
					uniqueSetOfLinks.add(linkId);
					Link link = network.getLinks().get(linkId);
					/* Convert MATSim link to Vividsolutions LineString to determine the distance from the line */
					Coordinate c1 = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
					Coordinate c2 = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
					Coordinate[] ca = {c1, c2};
					LineString lineString = gf.createLineString(ca);
					if (point.isWithinDistance(lineString, tempRadius)){ 
						possibleCandidateLinks.add(new PossibleCandidateLink(linkId, point.distance(lineString)));
					}
				}
			}
			
			Collections.sort(possibleCandidateLinks);
			// Only add the closest links depending on the setpoint
			int maxCandidateLinks = Math.min(numberOfLinks, possibleCandidateLinks.size());
			for ( int i = 0; i < maxCandidateLinks; i++ ) {
				PossibleCandidateLink posCanLink = possibleCandidateLinks.get(i);
				gpsPoint.addCandidateLink(posCanLink.getLinkId(), posCanLink.getDistanceFromGPSpoint(), gpsErrorMean, gpsErrorStandardDeviation);
			}
			if (gpsPoint.getCandidateLinks().size() == 0){
				System.out.println("ERROR - no Candidate links");
			}
			
			
			
			
			//Save data to arraylist for output
			String [] gpsPointData;
			for (CandidateLink link:gpsPoint.getCandidateLinks() ){
				gpsPointData = new String[7];
				gpsPointData[0] = String.valueOf(gpsPoint.getCoord().getX());
				gpsPointData[1] = String.valueOf(gpsPoint.getCoord().getY());
				gpsPointData[2] = String.valueOf(gpsPoint.getTime());
				gpsPointData[3] = String.valueOf(gpsPoint.getClosestLinkId().toString());
				gpsPointData[4] = String.valueOf(link.getLinkId().toString());
				gpsPointData[5] = String.valueOf(link.getDistanceFromGPSpoint());
				gpsPointData[6] = String.valueOf(link.getObservationProbability());
				gpsMatchedTrajectory.add(gpsPointData);
			}
		}
		
			// 2.3)
		List<Link> chosenPath = graphCreator.findMostProbableRoute(gpsPointList, network, directory);
		if (directory != null) {
			graphCreator.writeOutAttributes(directory); //- Not useful at the moment - might want to convert writing it to CSV
			MapMatchingUtils.writeDataToCSV(directory + "/GPS_OutputData.csv", gpsMatchedTrajectory);
			new NetworkWriter(MapMatchingUtils.createNetworkFromLinks(gpsPointList, network)).write(directory + "/GPS Point Network.xml");
		}
		
		EfficiencyCalculator.logEnd(this, "MapGPSTrajectory" + matchCounter, Double.valueOf(gpsPointList.size()));
		return chosenPath;		
	}
	
	@Override
	public String toString(){
		if (name == null ) {
			return directory;
		}
		else {
			return name;
		}
	}
}


