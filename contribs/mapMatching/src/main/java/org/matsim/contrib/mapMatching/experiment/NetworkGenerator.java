/* *********************************************************************** *
 * project: org.matsim.contrib.mapMatching.experiment*
 * NetworkGenerator.java
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

package org.matsim.contrib.mapMatching.experiment;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;


/**
 * Class to generate random grid networks.
 *
 * @author jwjoubert, jbvosloo
 */
public class NetworkGenerator {
	final private static Logger LOG = Logger.getLogger(NetworkGenerator.class);
	
	/**
	 * Generates a regular grid {@link Network} of any given size. Currently all edges are bidirectional.
	 * @param nodesInX number of nodes in the x-dimension (longitude);
	 * @param nodesInY number of nodes in the y-dimension (latitude);
	 * @param linklength link length, or distance between neighbouring nodes in either x- or y-direction.
	 * @return
	 */
	public static Network generateGrid(int nodesInX, int nodesInY, double linklength){
		LOG.info("Generating " + nodesInX + "x" + nodesInY + " grid network.");
		Network network = NetworkUtils.createNetwork();
		NetworkFactory networkFactory = network.getFactory();
		List<Node> previousColumnNode = new ArrayList<Node>();
		List<Node> currentColumnNode = new ArrayList<Node>();
		int x = 0;
		int y;
		
		for(int i = 0; i < nodesInX; i++){
			y = 0;
			for(int j = 0; j < nodesInY; j++){
				Id<Node> nodeId = Id.createNodeId(String.valueOf(i) + "-" +  String.valueOf(j));
				Node node = networkFactory.createNode(nodeId,CoordUtils.createCoord(x, y));
				network.addNode(node);
				if (!currentColumnNode.isEmpty()){
					Node nodeBelow = currentColumnNode.get(currentColumnNode.size()-1);
					addlLink(network, nodeBelow, node, linklength, true);
				}
				if (!previousColumnNode.isEmpty()){
					Node nodeToTheLeft = previousColumnNode.get(j);
					addlLink(network, nodeToTheLeft, node, linklength, true);
				}
				currentColumnNode.add(node);
				y += linklength;
			}
			previousColumnNode.clear();
			previousColumnNode.addAll(currentColumnNode);
			currentColumnNode.clear();
			x += linklength;
		}
		return network;
	}
	
	/**
	 * Not sure why this is a separate method, but it just adds the link, as well as the (optional) link in
	 * the opposite direction. 
	 * @param fromNode
	 * @param toNode
	 * @param networkFactory
	 * @param length
	 * @param network
	 * @param birectional
	 */
	private static void addlLink(Network network, Node fromNode, Node toNode, double length, boolean birectional){
		Id<Link> linkId = Id.createLinkId(fromNode.getId().toString() + "_" + toNode.getId().toString());
		Link link = network.getFactory().createLink(linkId, fromNode, toNode);						
		link.setLength(length);
		network.addLink(link);

		/* Add the reverse link for bidirectional edges. */
		if (birectional){
			linkId = Id.createLinkId(toNode.getId().toString() + "_" + fromNode.getId().toString());
			link = network.getFactory().createLink(linkId, toNode, fromNode);						
			link.setLength(length);
			network.addLink(link);
		}
	}
	
	
}
