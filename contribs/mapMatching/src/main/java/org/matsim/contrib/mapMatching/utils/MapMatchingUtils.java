/* *********************************************************************** *
 * project: org.matsim.contrib.mapMatching*
 * MapMatchingUtils.java
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

package org.matsim.contrib.mapMatching.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.mapMatching.graph.CandidateLink;
import org.matsim.contrib.mapMatching.trace.GpsCoord;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.TransformationFactory;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.Vehicle;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.quadtree.Quadtree;

public class MapMatchingUtils {
	
	public static void readAllLinks(Scenario sc, GeometryFactory gf, Logger log){
		Quadtree vQuadTree = new Quadtree();
		
		/* Links. */
		for(Id<Link> linkId : sc.getNetwork().getLinks().keySet()){
			Link link = sc.getNetwork().getLinks().get(linkId);
			log.info( linkId.toString() + ": From " + link.getFromNode().getId().toString() + "; To " + link.getToNode().getId().toString());
			
			/* Convert MATSim link to Vividsolutions LineString. */
			Coordinate c1 = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
			Coordinate c2 = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
			Coordinate[] ca = {c1, c2};
			LineString ls = gf.createLineString(ca);
			
			/* use the vivid solutions quadtree and add the edges  - but this adds rectangles not applicable*/
			vQuadTree.insert(new Envelope(c1.x, c2.x, c1.y, c2.y), link);
		}
		log.info("The quadtree size is " + vQuadTree.size());
	}
	
	public void readAllNodesAndCreateQuadTree(Scenario sc, GeometryFactory gf, Logger log){
	
		/* Set up QuadTree extent. */
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;
		
		
		/* Node. */
		for(Id<Node> nodeId : sc.getNetwork().getNodes().keySet()){
			Node node = sc.getNetwork().getNodes().get(nodeId);
			Coord c = node.getCoord();
			log.info(nodeId.toString() + ": (" + c.getX() + ";" + c.getY() + ")");
			
			/* Convert MATSim node to Vividsolutions Point. */
			Point p = gf.createPoint(new Coordinate(c.getX(), c.getY()));
			
			/* Just get the closest link in MATSim. */
			Link closestLink = NetworkUtils.getNearestLink(sc.getNetwork(), c);
			log.info("   -> Closest link: " + closestLink.getId().toString());
			
			/* Update QuadTree extent. */
			minX = Math.min(minX, c.getX());
			minY = Math.min(minY, c.getY());
			maxX = Math.max(maxX, c.getX());
			maxY = Math.max(maxY, c.getY());
		}
		
		
		/* Now we can create, and populate the QuadTree. */
		QuadTree<Id<Node>> qd = new QuadTree<>(minX, minY, maxX, maxY);
		for(Id<Node> nodeId : sc.getNetwork().getNodes().keySet()){
			Node node = sc.getNetwork().getNodes().get(nodeId);
			Coord c = node.getCoord();
			
			qd.put(c.getX(), c.getY(), node.getId());
		}
	}
	
	public static QuadTree<Id<Node>> createQuadTree(Network network){
		
		/* Set up QuadTree extent. */
		double minX = Double.POSITIVE_INFINITY;
		double minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY;
		double maxY = Double.NEGATIVE_INFINITY;

		for(Id<Node> nodeId : network.getNodes().keySet()){
			Node node = network.getNodes().get(nodeId);
			Coord c = node.getCoord();
			
			/* Update QuadTree extent. */
			minX = Math.min(minX, c.getX());
			minY = Math.min(minY, c.getY());
			maxX = Math.max(maxX, c.getX());
			maxY = Math.max(maxY, c.getY());
		}
		
		/* Now we can create, and populate the QuadTree. */
		QuadTree<Id<Node>> qd = new QuadTree<>(minX, minY, maxX, maxY);
		for(Id<Node> nodeId : network.getNodes().keySet()){
			Node node = network.getNodes().get(nodeId);
			Coord c = node.getCoord();
			
			qd.put(c.getX(), c.getY(), node.getId());
		}
		return qd;
	}
	
	public static void writeDataToCSV(String fileName, List<String[]> inputArray){
		
		BufferedWriter bw = IOUtils.getBufferedWriter(fileName);
		try {
			for(String[] sa : inputArray) {
				for(int i = 0; i < sa.length-1; i++) {
					bw.write(sa[i]);
					bw.write(",");
				}
				bw.write(sa[sa.length-1]);
				bw.newLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot write to " + fileName);
		} finally {
			try {
				bw.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + fileName);
			}
		}
}


	
	public static ArrayList<String[]> convertGPSpointsToArray(ArrayList<String[]> GPSpoints){
		ArrayList<String[]> gpsConfigList = new ArrayList<String[]>(); 
		gpsConfigList.add(new String[]{"x","y","time","lon", "lat" });
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		
		for(String[] gps : GPSpoints){
			
			Coord c = CoordUtils.createCoord(Double.valueOf(gps[0]), Double.valueOf(gps[1]) );;
			Coord cWgs = ct.transform(c);
			
			String[] idInfo = new String[]{
				String.valueOf(c.getX()), 
				String.valueOf(c.getY()), 
				gps[2],
				String.valueOf(cWgs.getX()), 
				String.valueOf(cWgs.getY()), 
			};  
			gpsConfigList.add(idInfo);
			
		}
		
		return gpsConfigList;
	}
	
	public static void writeGPSTrajectoriesToCSV(List<ArrayList<String[]>> listOfGPSpoints, String[] listOfGPStrajectoryFileNames, String directory){
		for (int i = 0; i < listOfGPStrajectoryFileNames.length-1; i++) {
			writeDataToCSV(directory + "/" + listOfGPStrajectoryFileNames[i] + ".csv", convertGPSpointsToArray(listOfGPSpoints.get(i)));
		}
	}
	
	
	
	
	
	
	
	/*
	 * Converting network or link objects
	 */
	
	
	
	public static ArrayList<String[]> convertLinkListToArray(List<Id<Link>> linkIdList, Network network){
		ArrayList<String[]> linkConfigList = new ArrayList<String[]>(); 
		linkConfigList.add(new String[]{"lid", "fn","fnX", "fnLon", "fnY", "fnLat", "tn", "tnX", "tnLon", "tnY", "tnLat", "Length", "FreeSpeed" });
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		
		for(Id<Link> linkID : linkIdList){
			Link link = network.getLinks().get(linkID);
			Coord fc = link.getFromNode().getCoord();
			Coord fcWgs = ct.transform(fc);
			Coord tc = link.getToNode().getCoord();
			Coord tcWgs = ct.transform(tc);
			
			String[] idInfo = new String[]{link.getId().toString(), 
					link.getFromNode().getId().toString(), 
					String.valueOf(fc.getX()), 
					String.valueOf(fcWgs.getX()), 
					String.valueOf(fc.getY()), 
					String.valueOf(fcWgs.getY()), 
					link.getToNode().getId().toString(), 
					String.valueOf(tc.getX()), 
					String.valueOf(tcWgs.getX()), 
					String.valueOf(tc.getY()),
					String.valueOf(tcWgs.getY()),
					String.valueOf(link.getLength()),
					String.valueOf(link.getFreespeed())};  
			linkConfigList.add(idInfo);
			}
			return linkConfigList;
		}
	
	public static void writeLinkListtoCSV(String fileName, List<Id<Link>> linkIdList, Network network){
		writeDataToCSV(fileName, convertLinkListToArray(linkIdList, network));
	}
	
	public static ArrayList<String[]> convertNetworkToArray(Network network){
		ArrayList<String[]> networkConfigList = new ArrayList<String[]>(); 
		networkConfigList.add(new String[]{"lid", "fn","fnX", "fnLon", "fnY", "fnLat", "tn", "tnX", "tnLon", "tnY", "tnLat", "Length", "FreeSpeed" });
		
		CoordinateTransformation ct = TransformationFactory.getCoordinateTransformation("WGS84_SA_Albers", "WGS84");
		
		for(Id<Link> linkId : network.getLinks().keySet()){
			
			Link link = network.getLinks().get(linkId);	
			Coord fc = link.getFromNode().getCoord();
			Coord fcWgs = ct.transform(fc);
			Coord tc = link.getToNode().getCoord();
			Coord tcWgs = ct.transform(tc);
			
			String[] idInfo = new String[]{linkId.toString(), 
					link.getFromNode().getId().toString(), 
					String.valueOf(fc.getX()), 
					String.valueOf(fcWgs.getX()), 
					String.valueOf(fc.getY()), 
					String.valueOf(fcWgs.getY()), 
					link.getToNode().getId().toString(), 
					String.valueOf(tc.getX()), 
					String.valueOf(tcWgs.getX()), 
					String.valueOf(tc.getY()),
					String.valueOf(tcWgs.getY()),
					String.valueOf(link.getLength()),
					String.valueOf(link.getFreespeed())};  
			networkConfigList.add(idInfo);
			
		}
		
		return networkConfigList;
	}
	
	public static void writeNetworktoCSV(String fileName, Network network){
		writeDataToCSV(fileName, convertNetworkToArray(network));
	}
	
	public static Network createNetworkFromLinks( Network network, List<Link> ListOfLinks){
		List<Id<Link>> ListOfLinkIds = new ArrayList<Id<Link>>();
		for (Link link:ListOfLinks){
			ListOfLinkIds.add(link.getId());
		}
		return createNetworkFromLinkIds(network, ListOfLinkIds);
	}
	
	public static Network createNetworkFromLinks(ArrayList<GpsCoord> gpsPointList, Network network) {
		List<Id<Link>> ListOfLinkIds = new ArrayList<Id<Link>>();
 		
		for (GpsCoord point: gpsPointList){
			for (CandidateLink candidatelink: point.getCandidateLinks()){ 
				ListOfLinkIds.add(candidatelink.getLinkId());
			}
		}
		
		return createNetworkFromLinkIds(network, ListOfLinkIds);
	}
	
	public static List<Link> createListOfLinksFromArray(ArrayList<String[]> arrayList, Network network) {
		List<Link> listofLinks = new ArrayList<Link>();
		for (String[] st:arrayList ) {
			listofLinks.add(network.getLinks().get(Id.createLinkId(st[0])));
		}
		return listofLinks;
	}
	
 	public static Network createNetworkFromLinkIds( Network network, List<Id<Link>> ListOfLinkIds){
 		Network	candidateNetwork = NetworkUtils.createNetwork();
 		NetworkFactory candidateNF = candidateNetwork.getFactory();
 		
 		for (Id<Link> linkID: ListOfLinkIds){ 
			Link candidateLink = network.getLinks().get(linkID);
			Id<Node> fromNodeId = Id.createNodeId(candidateLink.getFromNode().getId().toString());
			Id<Node> toNodeId = Id.createNodeId(candidateLink.getToNode().getId().toString());
			
			Node fromNode;
			Node toNode;
			Link link;
			
			// Add all the nodes if not already in the network
			if(!candidateNetwork.getNodes().containsKey(fromNodeId)){
				fromNode = candidateNF.createNode(fromNodeId, candidateLink.getFromNode().getCoord());
				candidateNetwork.addNode(fromNode);
			}
			else {
				fromNode = candidateNetwork.getNodes().get(fromNodeId);
			}
			if(!candidateNetwork.getNodes().containsKey(toNodeId)){
				toNode = candidateNF.createNode(toNodeId, candidateLink.getToNode().getCoord());
				candidateNetwork.addNode(toNode);
			} 
			else {
				toNode = candidateNetwork.getNodes().get(toNodeId);
			}

			// Add the link
			if (!candidateNetwork.getLinks().containsKey(linkID)) {
				link = candidateNF.createLink(linkID, fromNode, toNode);
				link.setFreespeed(network.getLinks().get(linkID).getFreespeed());
				link.setLength(network.getLinks().get(linkID).getLength());
				candidateNetwork.addLink(link);
			}
		}
		return candidateNetwork;
 	}
	
 	
 	//----------------------------------------------------------------------
 	
	
 	/**
 	 * 
 	 * @param network
 	 * @param fromNode
 	 * @param toNode
 	 * @param shortesRoute if true the you get the shortest path (can be lowest probability if the network is the 'graph'). Conversely, false gives the longest path (or highest probability if the network is a 'graph'). 
 	 * @return
 	 */
	public static Path getPath(Network network, Node fromNode, Node toNode, Boolean shortesRoute){
		DijkstraFactory dijkstraFactory = new DijkstraFactory();
		TravelDisutility travelDisutilityShortest = new TravelDisutility() {
			
			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return link.getLength();
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return link.getLength();
			}
		};
		
		TravelDisutility travelDisutilityLongest = new TravelDisutility() {
			
			@Override
			public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
				return 1.0/link.getLength();
			}
			
			@Override
			public double getLinkMinimumTravelDisutility(Link link) {
				return 1.0/link.getLength();
			}
		};
		
		TravelTime travelTime = new FreeSpeedTravelTime();
		LeastCostPathCalculator leastCostPathCalculator;
		if (shortesRoute){
			leastCostPathCalculator = dijkstraFactory.createPathCalculator(network, travelDisutilityShortest, travelTime );
		}
		else {
			leastCostPathCalculator = dijkstraFactory.createPathCalculator(network, travelDisutilityLongest, travelTime );	
		}
		
		Path path = leastCostPathCalculator.calcLeastCostPath(fromNode, toNode, Time.parseTime("08:00:00"), null, null);
		return path;
	}
	
	
	
	public static Coordinate convertFromCoordToCoordinate(Coord coord){
		/* Convert MATSim link to Vividsolutions LineString. */
		Coordinate coordinate = new Coordinate(coord.getX(), coord.getY());
		return coordinate;
	}

	public static List<Link> convertSetIntoList(Set<Id<Link>> keySet, Network network) {
		List<Link> returnList= new ArrayList<Link>();
		Object[] array = keySet.toArray();
		int x = array.length;
		for(int i = 0; i < x; i++){
			returnList.add(network.getLinks().get((array[i])));
		}
		return returnList;
	}
	
	public static Network parseNetwork (String networkLocation){
		Scenario sc = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(sc.getNetwork()).readFile(networkLocation);
		return sc.getNetwork();
	}
	public static List<String[]> parseToArrayfromCSV(String inputGPStrajectory) {
		return parseToArrayfromCSV(inputGPStrajectory, 0);
	}
	
	public static List<String[]> parseToArrayfromCSV(String inputGPStrajectory, int startingLine) {
		
		List<String[]> arrayOfCSVdata = new ArrayList<>();
		BufferedReader br = IOUtils.getBufferedReader(inputGPStrajectory);
		try {
			String line = null;
			while((line = br.readLine()) != null) {
				String[] sa = line.split(",");
				arrayOfCSVdata.add(sa);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read from " + inputGPStrajectory);
		} finally {
			try {
				br.close();
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot close " + inputGPStrajectory);
			}
		}

		/* Remove first lines. */
		for (int i = 0; i < startingLine; i++) {
			arrayOfCSVdata.remove(0);
		}
		return arrayOfCSVdata;
		
	}
	
	public static List<List<String[]>> parseToArrayFromCSVs(String gpsTrajectoriesFilename, String[] inputGPStrajectories) {
		List<List<String[]>> listOfGPSpoints  = new ArrayList<List<String[]>>();
		for ( int i = 0; i < inputGPStrajectories.length; i++) {
			if ( !inputGPStrajectories[i].startsWith(".")) {
				listOfGPSpoints.add(parseToArrayfromCSV(gpsTrajectoriesFilename + "/" + inputGPStrajectories[i]));
			}
		}
		return listOfGPSpoints;
	}

	
}