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

package org.matsim.contrib.mapMatching.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.mapMatching.evaluation.AccuracyCalculator;
import org.matsim.contrib.mapMatching.trace.GpsCoord;
import org.matsim.contrib.mapMatching.utils.MapMatchingUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.utils.objectattributes.AttributeConverter;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;


public class GraphCreator {
	
	private Network	graph;
	private Node startNode;
	private Node endNode;
	private NetworkFactory networkFactory;
	private ObjectAttributes graphNodeAttributes;
	private ObjectAttributes graphLinkAttributes;
	private Network network;
	
	private LinkedHashMap<String, PathData> pathDataMap = new LinkedHashMap<String, PathData>();
	
	public GraphCreator() {
		graph = NetworkUtils.createNetwork();
		networkFactory = graph.getFactory();
		graphNodeAttributes = new ObjectAttributes();
		graphLinkAttributes = new ObjectAttributes();
	}
	
	/**
	 * 
	 * @author JBVosloo
	 * An inner class used to store all the data of a link in the chosen route
	 */
	class PathData {
		private Id<Link> linkID;
		private ArrayList<Double> possibleSpeeds = new ArrayList<Double>() ;
		
		protected PathData(Id<Link> linkID, double speed) {
			this.linkID = linkID;
			addSpeed(speed);
		}
		
		protected PathData(Id<Link> linkID) {
			this.linkID = linkID;
		}
		
		protected void addSpeed(Double speed) {
				possibleSpeeds.add(speed);
		}
		
		protected Id<Link> getLinkID() {
			return linkID;
		}

		protected double getSpeed() {
			double averageSpeed = 0;
			int entries = 0;
			for (Double d:possibleSpeeds) {
				averageSpeed += d;
				entries ++;
			}
			return averageSpeed/entries;
		}
	}
	
	// These variables are used for adding a found route to a LinekdHashMap with the speeds and link ids
	
	private Id<Link> lastLinkAdded = null;
	private String lastUniqueIDused = "";
	private int counter = 0;
	
	
	// Method for adding pathDataClass to the LinkedHashMap, if this is part of a series of the same link them add to the same entry as previous otherwise create a new one
	private void addLinkData (Id<Link> linkID, Double speed) {
		if (linkID == lastLinkAdded) {
			if (speed != null) {
				pathDataMap.get(lastUniqueIDused).addSpeed(speed);;
			}
		}
		else {
			lastUniqueIDused = linkID.toString() + "_" + counter;
			counter ++;
			lastLinkAdded = linkID;
			if (speed == null) {
				pathDataMap.put(lastUniqueIDused, new PathData(linkID));
			}
			else {
				pathDataMap.put(lastUniqueIDused, new PathData(linkID, speed));
			}
		}
		
	}
	
	
	//Main Methods-------------------------------------------------------------
	
	
	@SuppressWarnings("unchecked")
	public List<Link> findMostProbableRoute(ArrayList<GpsCoord> gpsPointList, Network network, String directory){
		createGraph(gpsPointList, network, directory);
		Path path = MapMatchingUtils.getPath(graph, startNode, endNode, false); // Graph path.
		ArrayList<Id<Link>> ListOfLinkIds = new ArrayList<Id<Link>>(); 
		ArrayList<String[]> ListOfRouteLinksWithData = new ArrayList<String[]>();

		

		
		for (Link graphLink: path.links){
			// Cycle through the links of the graph and first add the from link, 
			// then add the links stored as the shortest path between the links (If there are any)
			// Then store the end link
			//Skip the first and last nodes that were created manually in the graph as they have no original links
			if (graphLink.getFromNode().getId().toString() != "Start" ) {
				Id<Link> originalLinkID = getOriginalLink(graphLink.getFromNode()).getId();
				addLinkData(originalLinkID, null);
				if (graphLink.getToNode().getId().toString() != "End") {
					addLinkData(originalLinkID,
						(double)graphLinkAttributes.getAttribute(graphLink.getId().toString(), GraphLinkAttributeTypes.Speed.toString()));
				}
			}
			//Check if there is a path of original links between the graph nodes
			if (graphLink.getFromNode().getId().toString() != "Start" && graphLink.getToNode().getId().toString() != "End") {
				for (Link originalLink:(ArrayList<Link>)graphLinkAttributes.getAttribute(graphLink.getId().toString(), GraphLinkAttributeTypes.ShortestPathLinks.toString()) ){
					addLinkData(originalLink.getId(), 
							(double)graphLinkAttributes.getAttribute(graphLink.getId().toString(), GraphLinkAttributeTypes.Speed.toString()));
				}
			}
			// get the end original link , skip if it is the 'end' node as it is not associated with an original link
			if (graphLink.getToNode().getId().toString() != "End" ) {
				Id<Link> originalLinkID = getOriginalLink(graphLink.getToNode()).getId();
				addLinkData(originalLinkID, null);
				if (graphLink.getFromNode().getId().toString() != "Start") {
					addLinkData(originalLinkID, 
						(double)graphLinkAttributes.getAttribute(graphLink.getId().toString(), GraphLinkAttributeTypes.Speed.toString()));
				}
			}
		}
		
		
		/*
		 * Store a list of all the links traveled as well as the calculated speed on these links
		 * All saved a list of the links to be passed back
		 */
		List<Link> mostProbableRoute = new ArrayList<Link>();
		for (PathData pd:pathDataMap.values()) {
			mostProbableRoute.add(network.getLinks().get(pd.getLinkID()));
			ListOfLinkIds.add(pd.getLinkID());
			ListOfRouteLinksWithData.add(new String[]{
					pd.getLinkID().toString(),
					String.valueOf(network.getLinks().get(pd.getLinkID()).getLength()),
					String.valueOf(pd.getSpeed()),
					String.valueOf(network.getLinks().get(pd.getLinkID()).getFreespeed()),
					null,
					null
			});
		}
		
		
		
		/*
		 * Write out all the results if a directory is provided
		 */
		Network mostProbableRouteNetwork = MapMatchingUtils.createNetworkFromLinkIds(network, ListOfLinkIds);
		if (directory != null) {
			new NetworkWriter(mostProbableRouteNetwork).write(directory + "/Chosen path.xml");
			MapMatchingUtils.writeLinkListtoCSV(directory + "/Chosen_Path.csv",ListOfLinkIds, network);
			AccuracyCalculator.savedSpeednalysis(ListOfRouteLinksWithData, directory); //TODO this now creates a dependency between these tow packages.... not ideal - but I can just combine the packages in one
		}
		System.out.println("The probability of the path is" + (1-path.travelCost/path.links.size()));
		System.out.println("The travelcost is" + (path.travelCost));
		System.out.println("The number of links" + (path.links.size()));
		
		return mostProbableRoute;
	}
	
	
	/*
	 * Method to create graph
	 */
	
	private void createGraph(ArrayList<GpsCoord> gpsPointList, Network network, String directory) {
		graph = NetworkUtils.createNetwork();
		networkFactory = graph.getFactory();
		graphNodeAttributes = new ObjectAttributes();
		graphLinkAttributes = new ObjectAttributes();
		/*
		 * Steps for creating a graph of all the possible links
		 * 1) For GPS point in the trajectory do the following
		 * 	1.1) Add all the possible links for this point as nodes to a graph
		 * 		Nodes will be placed sequentially in a structured format in this graph in order to create a view of the graph
		 * 	1.2) Connect all of these nodes to the previous nodes with links - skip if this is the first point in the list
		 *	
		 * 2) Output this new graph network to output data - for review purposes
		 */
		this.network = network;
		
		double x = 0;
		double y = 0;
		ArrayList<Node> currentNewNodeIDs = new ArrayList<Node>(); 
		ArrayList<Node> previousNewNodeIDs = new ArrayList<Node>(); 
		GpsCoord previousGPSpoint = null;
		
		//Create a starting Node - End Node is created after all the other graph nodes
		Id<Node> StartNodeId = Id.createNodeId("Start");
		startNode = networkFactory.createNode(StartNodeId,CoordUtils.createCoord(x, 10));
		graph.addNode(startNode);

		for (GpsCoord point: gpsPointList){
			x = x + 10;
			y = 0;
			
			// 1.1) Create a node for every possible link that this point is connected to
			for (CandidateLink candidateLink: point.getCandidateLinks()){ 
				Id<Link> originalLinkID = candidateLink.getLinkId();
				y = y + 10;
				
				// TODO Need to double check my logic if there are links that are candidate links for more than one point
				// Will also happen if there are multiple points next to single stretch of road
				int i = 0;
				Id<Node> nodeId;
				do {
					nodeId = Id.createNodeId(originalLinkID.toString() + "-" + i);
					i++;
				} while(graph.getNodes().containsKey(nodeId));
				
				graphNodeAttributes.putAttribute(nodeId.toString(), GraphNodeAttributeTypes.OriginalLinkID.toString(), originalLinkID); // need to save the original link ID for this node in order to get the original link for calculations
				graphNodeAttributes.putAttribute(nodeId.toString(), GraphNodeAttributeTypes.DistanceToLink.toString(), candidateLink.getDistanceFromGPSpoint()); 
				graphNodeAttributes.putAttribute(nodeId.toString(), GraphNodeAttributeTypes.ObservationProbability.toString(), candidateLink.getObservationProbability());
				
				Node node= networkFactory.createNode(nodeId,CoordUtils.createCoord(x, y));
				graph.addNode(node);
				currentNewNodeIDs.add(node);
				
				// 1.2) If there are previous nodes connect them to these nodes else create a blank node for start
				if (!previousNewNodeIDs.isEmpty()) {
					for (Node pNode:previousNewNodeIDs) {
						Id<Link> linkId = Id.createLinkId(pNode.getId().toString() + "_" + node.getId().toString() );
						Link graphLink = networkFactory.createLink(linkId, pNode, node);						
						graphLink.setLength(calculateWeight(pNode, node, previousGPSpoint, point, network, graphLink));
						graph.addLink(graphLink);
					}
				}
				else {
					Id<Link> linkId = Id.createLinkId(StartNodeId.toString() + "_" + node.getId().toString() );
					Link graphLink = networkFactory.createLink(linkId, startNode, node);
					graphLink.setLength(candidateLink.getObservationProbability());
					graph.addLink(graphLink);
				}
			}
			previousNewNodeIDs.clear();
			previousNewNodeIDs.addAll(currentNewNodeIDs);
			currentNewNodeIDs.clear();
			previousGPSpoint = point;
		}
		//Create an end node
		Id<Node> EndNodeId = Id.createNodeId("End");
		endNode= networkFactory.createNode(EndNodeId,CoordUtils.createCoord(x + 10, 10));
		graph.addNode(endNode);
		
		// Link the end node to all the last nodes
		for (Node pNode:previousNewNodeIDs) {
			Id<Link> linkId = Id.createLinkId(pNode.getId().toString() + "_" + EndNodeId.toString() );
			Link graphLink = networkFactory.createLink(linkId, pNode, endNode);
			graphLink.setLength(1);
			graph.addLink(graphLink);
		}
		//mapMatching.MainClass.LOG.info(graph.getLinks().size() ); // TODO
		
		// 2)
		if (directory != null) {
			new NetworkWriter(graph).write(directory + "/Graph Network.xml");
			MapMatchingUtils.writeNetworktoCSV(directory + "/GraphNetwork_OutputData.csv",graph);
		}
		
	}
	
	//Ancillary methods------------------------------------------------------------
	
	
	private  double calculateWeight(Node previousNode, Node currentNode, GpsCoord previousGPSpoint, GpsCoord gpsPoint, Network network, Link link ){
		double euclideanDistanceBetweenGPSpoints = NetworkUtils.getEuclideanDistance(previousGPSpoint.getCoord(), gpsPoint.getCoord());
		
		//Need to test out which works best
		//1) This gives the same answer for all the combinations
		//double shortestDistanceBetweenNodes = getShortestPathDistance(network, getClosestOriginalNode(previousNode, previousGPSpoint), getClosestOriginalNode(currentNode, gpsPoint));
		
		//2) This results in some distances being 0 as the start and the end is the same
		//double shortestDistanceBetweenNodes = getShortestPathDistance(network, getOriginalFromNode(previousNode), getOriginalToNode(currentNode));
		
		//3) The other option is to use from nodes and then just subtract the distance of the gps point from the from node
		//double shortestDistanceBetweenNodes = getShortestPathDistance(network, getOriginalFromNode(previousNode), getOriginalFromNode(currentNode));
			//There will always be a situation where these two are the same.... the from and to or to and from
		
		// but I must always use the To node for the starting node so as to ensure that that specific link was used to travel first - Is using from node he can just do a u turn
		
		//4) Use To node for starting point, calculate the additional distance to be removed
		double shortestDistanceBetweenNodes = 0;
		double gpsPointDistanceFromFromNode = 0;
		double gpsPointDistanceFromToNode = 0;
		List<Link> listOfLinks = new ArrayList<Link>();
		boolean usePathMethod = true;
		
		Node fromNode = getOriginalToNode(previousNode);
		listOfLinks.add(getOriginalLink(previousNode));
		gpsPointDistanceFromFromNode = getProjectedDistanceOnLinkFromCoordToNode(previousGPSpoint, fromNode, getOriginalLink(previousNode));
		
		// Check if the graph nodes represent the same candidate link (i.e. the GPS point is referencing the same segment)
		// Then the distance between the nodes is only the distance on the segment between them and no shortest path calc is required
		// a shortest path calc will lead to u-turns and "driving-around-the-block" behaviour
		// But if the projected points of the gps points are not in the correct direction then the path method must be used
			// TODO in the articles they dont use the projected distance to a point on an estended line -
				// They use the projected point on the line - this will not penalise points further off even more - but that is what the observation probability is for is it not?
				// This can be investigated as a potential improvement together with using the temporal probability 
				// as a function of the calcuated speed vs the speed of the network
		if (getOriginalLink(previousNode).equals(getOriginalLink(currentNode))) {
			usePathMethod = false;
			Link originalLink = getOriginalLink(currentNode);
			gpsPointDistanceFromToNode = getProjectedDistanceOnLinkFromCoordToNode(gpsPoint, fromNode, originalLink) ;
			if (gpsPointDistanceFromToNode < gpsPointDistanceFromFromNode) {
				shortestDistanceBetweenNodes = NetworkUtils.getEuclideanDistance(getProjectedPointonLink(gpsPoint, originalLink),getProjectedPointonLink(previousGPSpoint, originalLink));
				// listOfLinks.add(getOriginalLink(previousNode)); removed as it is already added as the first link
				graphLinkAttributes.putAttribute(link.getId().toString(), GraphLinkAttributeTypes.ShortestPathLinks.toString(), listOfLinks);
			}
			else {
				usePathMethod = true;
			}
		}

		if (usePathMethod) {
			Node toNode = null;
			toNode	= getOriginalFromNode(currentNode);
			gpsPointDistanceFromToNode = getProjectedDistanceOnLinkFromCoordToNode(gpsPoint, toNode,  getOriginalLink(currentNode));
			listOfLinks.add(getOriginalLink(currentNode));
			
			/**
			 * This section above can lead to less incorrect segments ??
			 */
			
			// Must always move from ToNode to ToNode to ensure that the applicable paths are actually travelled
			// only if they are the same point can we use the from node and then add the projected distance back
//			if (fromNode.equals(getOriginalToNode(currentNode))){
//				toNode	= getOriginalFromNode(currentNode);
//				// When using the from node we must add the distance if the GPA node'projection is within the link and subtract if outside
//				// Only subtract it if it is a point on the link - 
//				// if it is outside the link then you dont need to add anything as the entire link distance will be used
//				gpsPointDistanceFromToNode = getProjectedDistanceOnLinkFromCoordToNode(gpsPoint, toNode,  getOriginalLink(currentNode));
//				if ( gpsPointDistanceFromToNode == getOriginalLink(currentNode).getLength()) {
//					gpsPointDistanceFromToNode = 0;
//				}
//				else {
//					gpsPointDistanceFromToNode = -gpsPointDistanceFromToNode;
//				}
//			}
//			else {
//				toNode	= getOriginalToNode(currentNode);
//				gpsPointDistanceFromToNode = -getProjectedDistanceOnLinkFromCoordToNode(gpsPoint, toNode, getOriginalLink(currentNode));
//			}
//			
			
			// This was used originally - seemed to give a more logical answer
//			if (fromNode.equals(getOriginalFromNode(currentNode))){
//				toNode	= getOriginalToNode(currentNode);
//				gpsPointDistanceFromToNode = getProjectedDistanceOnLinkFromCoordToNode(gpsPoint, toNode, getOriginalFromNode(currentNode), currentNode);
//			}
//			else {
//				toNode	= getOriginalFromNode(currentNode);
//				// When using the from node we must add the distance if the GPA node'projection is within the link and subtract if outside
//				gpsPointDistanceFromToNode = getProjectedDistanceOnLinkFromCoordToNode(gpsPoint, toNode, getOriginalToNode(currentNode), currentNode);
//			}
			
			// Using only the ToNode proved to be very inaccurate
//			toNode	= getOriginalFromNode(currentNode);
//			gpsPointDistanceFromToNode = getProjectedDistanceOnLinkFromCoordToNode(gpsPoint, toNode, getOriginalLink(currentNode));
//		
			//Save the shortest path that will get from the previous node to current node
			Path path = MapMatchingUtils.getPath(network, fromNode, toNode , true);
			graphLinkAttributes.putAttribute(link.getId().toString(), GraphLinkAttributeTypes.ShortestPathLinks.toString(), path.links);
			
			shortestDistanceBetweenNodes = path.travelCost;
			shortestDistanceBetweenNodes += gpsPointDistanceFromFromNode + gpsPointDistanceFromToNode;
			listOfLinks.addAll(path.links);
			if (shortestDistanceBetweenNodes == 0){
				System.out.println("Distance is 0");
			}
		}
		
		double observationProbability = (Double)graphNodeAttributes.getAttribute(currentNode.getId().toString(), GraphNodeAttributeTypes.ObservationProbability.toString());
		double transmissionProbaility = Math.min(euclideanDistanceBetweenGPSpoints/shortestDistanceBetweenNodes,1);
		double temporalProbability = temporalProbability(listOfLinks, shortestDistanceBetweenNodes, previousGPSpoint.getTime(), gpsPoint.getTime());
		
		//Save the individual attributes of ST matching that make up the probability
		graphLinkAttributes.putAttribute(link.getId().toString(), GraphLinkAttributeTypes.TransmissionProbability.toString(), transmissionProbaility);
		graphLinkAttributes.putAttribute(link.getId().toString(), GraphLinkAttributeTypes.ObservationProbability.toString(), observationProbability);
		graphLinkAttributes.putAttribute(link.getId().toString(), GraphLinkAttributeTypes.TemporalProbability.toString(), temporalProbability);
		graphLinkAttributes.putAttribute(link.getId().toString(), GraphLinkAttributeTypes.Speed.toString(), shortestDistanceBetweenNodes/(gpsPoint.getTime()-previousGPSpoint.getTime()));
		
		if (Double.isNaN(observationProbability)){
			System.out.println("Test1");
		}
		if (Double.isNaN(transmissionProbaility)){
			System.out.println("Test2");
		}
		if (Double.isNaN(temporalProbability)){
			System.out.println("Test3");
		}
		//
		if (observationProbability<0){
			System.out.println("Test11");
		}
		if (transmissionProbaility<0){
			System.out.println("Test22");
		}
		if (temporalProbability<0){
			System.out.println("Test33");
		}
		if (shortestDistanceBetweenNodes == 0) {
			return 0;
		}
		return observationProbability*transmissionProbaility*temporalProbability ;
		//return observationProbability*transmissionProbaility;
		//return transmissionProbaility;
		//return temporalProbability;
		//return observationProbability;
	}
	
	//@SuppressWarnings("unused")
	private  double temporalProbability(List<Link> path, double shortestDistanceBetweenNodes, double startTime, double endTime){
		if (shortestDistanceBetweenNodes == 0 ) {
			return 0;
		}
		double speed = shortestDistanceBetweenNodes/(endTime - startTime);
		//double speed = path.travelCost/(endTime - startTime);
		//System.out.println(path.travelCost - shortestDistanceBetweenNodes);
		double a = 0;
		for (Link link:path){
			a += link.getFreespeed()*speed; 
		}
		double b = 0;
		for (Link link:path){
			b += Math.pow(link.getFreespeed(),2);
		}
		double c = 0;
		for (Link link:path){
			c += Math.pow(speed,2);
		}

		return a/(Math.sqrt(b)*Math.sqrt(c));
	}
	
	//
	@SuppressWarnings("unchecked")
	private Link getOriginalLink(Node graphNode){
		Id<Link> linkId = (Id<Link>)graphNodeAttributes.getAttribute(graphNode.getId().toString(), GraphNodeAttributeTypes.OriginalLinkID.toString());
		return network.getLinks().get(linkId);
	}
	
	@SuppressWarnings("unchecked")
	private double getDistanceToOriginalLink(Node node){
		return  (Double)graphNodeAttributes.getAttribute(node.getId().toString(), GraphNodeAttributeTypes.DistanceToLink.toString());
		
	}
	
	private Node getOriginalFromNode(Node node){
		return getOriginalLink(node).getFromNode();
	}
	
	private Node getOriginalToNode(Node node){
		return getOriginalLink(node).getToNode();
	}
	
	private Node getClosestOriginalNode(Node node, GpsCoord gpsPoint){
		return NetworkUtils.getCloserNodeOnLink(gpsPoint.getCoord(), getOriginalLink(node));
	}
	
	
	//Todo This needs to be moved to when the candidate link is created
	// How to retrieve the coord is to ask the gps poin for the coord and pass it the link
	// This should save time
	private Coord getProjectedPointonLink(GpsCoord gpsPoint, Link link){
		Coordinate ca = new Coordinate(link.getFromNode().getCoord().getX(), link.getFromNode().getCoord().getY());
		Coordinate cb = new Coordinate(link.getToNode().getCoord().getX(), link.getToNode().getCoord().getY());
		
		LineSegment seg = new LineSegment(ca, cb);
		
		Coordinate cu = new Coordinate(gpsPoint.getCoord().getX(), gpsPoint.getCoord().getY());
		Coordinate cp = seg.project(cu);
		//Coordinate cp = seg.closestPoint(cu);
		
		Coord coord = CoordUtils.createCoord(cp.x, cp.y);
		return coord;	
	}
	
	private double getProjectedDistanceOnLinkFromCoordToNode(GpsCoord gpsPoint, Node originalNodeToMeasureFrom, Link link){
		Coord coord = getProjectedPointonLink(gpsPoint, link);
		return NetworkUtils.getEuclideanDistance(coord, originalNodeToMeasureFrom.getCoord());
	}
	
//	static private double getProjectedDistanceOnLinkFromCoordToNode(GPSpoint gpsPoint, Node originalNodeToProjectTo, Node originalNodeOther, Node graphNode ){
//		
//		//Need to create a vivid solutions coordinate in order to compute the angles to the different node
//		Coordinate tail = new Coordinate(originalNodeToProjectTo.getCoord().getX(), originalNodeToProjectTo.getCoord().getY());
//		Coordinate tip1 = new Coordinate(originalNodeOther.getCoord().getX(), originalNodeOther.getCoord().getY());
//		Coordinate tip2 = new Coordinate(gpsPoint.getxCoord(), gpsPoint.getyCoord());
//		
//		double hypotenuse =  NetworkUtils.getEuclidianDistance(originalNodeToProjectTo.getCoord(), gpsPoint.getCoord());
//		double alpha = Angle.angleBetween(tip1, tail, tip2);
//		if (alpha > Math.PI / 2) { // if this angle is bigger than 90' then we need to take the other angle as this means projection is outside the link
//			alpha = Math.PI - alpha;
//		}
//		return hypotenuse * Math.cos(alpha);
//	}
//	 static private boolean isPointOutsideLink(GPSpoint gpsPoint, Node originalNodeToProjectTo, Node originalNodeOther, Node graphNode ){
//			//Need to create a vivid solutions coordinate in order to compute the angles to the different node
//			Coordinate tail = new Coordinate(originalNodeToProjectTo.getCoord().getX(), originalNodeToProjectTo.getCoord().getY());
//			Coordinate tip1 = new Coordinate(originalNodeOther.getCoord().getX(), originalNodeOther.getCoord().getY());
//			Coordinate tip2 = new Coordinate(gpsPoint.getxCoord(), gpsPoint.getyCoord());
//			
//			double hypotenuse =  NetworkUtils.getEuclidianDistance(originalNodeToProjectTo.getCoord(), gpsPoint.getCoord());
//			double alpha = Angle.angleBetween(tip1, tail, tip2);
//			if (alpha > Math.PI / 2) { // if this angle is bigger than 90' then we need to take the other angle as this means projection is outside the link
//				return true;
//			}
//			return false;
//	 }
			
 	public void writeOutAttributes(String directory){
		ObjectAttributesXmlWriter oaLink = new ObjectAttributesXmlWriter(graphLinkAttributes);
		AttributeConverter<Id<Link>> idConverter = new AttributeConverter<Id<Link>>() {

			@Override
			public Id<Link> convert(String value) {
				return Id.createLinkId(value);
			}

			@Override
			public String convertToString(Object o) {
				return o.toString();
			}
		};
		oaLink.putAttributeConverter(Id.class, idConverter );
		oaLink.writeFile(directory + "/graphLinkAttributes.xml");
		
		ObjectAttributesXmlWriter oaNode = new ObjectAttributesXmlWriter(graphNodeAttributes);
		oaNode.putAttributeConverter(Id.class, idConverter);
		oaNode.writeFile(directory + "/graphNodeAttributes.xml");
	}
	
}


