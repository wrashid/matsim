/* *********************************************************************** *
 * project: org.matsim.*
 * ArrayRoutingNetworkFactory.java
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

package org.matsim.core.router.util;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.lanes.Lane;
import org.matsim.lanes.Lanes;
import org.matsim.lanes.LanesToLinkAssignment;

/*
 * TODO:
 * - adapt RoutingNetworkFactory to support "createRoutingNetwork(final Network network, final Lanes lanes)"
 */

public class LinkToLinkRoutingNetworkFactory {

	private final static Logger log = Logger.getLogger(LinkToLinkRoutingNetworkFactory.class);
	
	private int nodeArrayIndexCounter;
	private int linkArrayIndexCounter;
	
	// Taking turn restrictions into account and split nodes that are linked to restricted links.
	public LinkToLinkArrayRoutingNetwork createRoutingNetwork(final Network network, final Lanes lanes) {
		
		this.nodeArrayIndexCounter = 0;
		this.linkArrayIndexCounter = 0;
		
		LinkToLinkArrayRoutingNetwork routingNetwork = new LinkToLinkArrayRoutingNetwork(network);
		
		// collect node information
		Map<Id<Node>, NodeWrapper> wrappers = new LinkedHashMap<>();
		for (Node node : network.getNodes().values()) {
			NodeWrapper wrapper = new NodeWrapper();
			wrapper.node = node;
			wrapper.toLinks.addAll(node.getOutLinks().values());
			
			// create BitSet for default node allowing all out-links
			BitSet defaultBitSet = new BitSet(wrapper.toLinks.size());
			for (int i = 0; i < wrapper.toLinks.size(); i++) defaultBitSet.set(i, true);
			wrapper.links.put(defaultBitSet, new ArrayList<>());
			
			for (Link inLink : node.getInLinks().values()) {
				BitSet bitSet = new BitSet(wrapper.toLinks.size());
				
				LanesToLinkAssignment assignment = lanes.getLanesToLinkAssignments().get(inLink.getId());
				if (assignment != null) {
					Set<Id<Link>> set = new TreeSet<>();	// ensure a deterministic order!
					for (Lane lane : assignment.getLanes().values()) {
						if (lane.getToLinkIds() != null) set.addAll(lane.getToLinkIds());
					}
					
					for (int i = 0; i < wrapper.toLinks.size(); i++) {
						Link link = wrapper.toLinks.get(i);
						if (set.contains(link.getId())) bitSet.set(i, true);
						else bitSet.set(i, false);
					}
				} 
				// Default: No turn restrictions, i.e. all entries in the BitSet have to be true.
				else {
					for (int i = 0; i < wrapper.toLinks.size(); i++) bitSet.set(i, true);
				}
				
				// add BitSet to wrapper
				List<Link> list = wrapper.links.get(bitSet);
				if (list == null) {
					list = new ArrayList<>();
					wrapper.links.put(bitSet, list);
				}
				list.add(inLink);
			}
			
			wrappers.put(node.getId(), wrapper);
		}
		
		// create nodes
		Map<Id<Link>, RoutingNetworkNode> toNodes = new HashMap<>();	// due to a link's turn restrictions we can determine its to-nodes
		for (NodeWrapper wrapper : wrappers.values()) {
			int i = 0;
			for (Entry<BitSet, List<Link>> entry : wrapper.links.entrySet()) {
				BitSet bitSet = entry.getKey();
				List<Link> inLinks = entry.getValue();
				int numOutLinks = bitSet.cardinality();
				
				Node node;
				if (numOutLinks == wrapper.toLinks.size()) node = wrapper.node;	// default node
				else node = network.getFactory().createNode(Id.createNodeId(wrapper.node.getId().toString() + "_turnRestrictionsDuplicate_" + i++), wrapper.node.getCoord());
				
				RoutingNetworkNode routingNode = createRoutingNetworkNode(node, numOutLinks);
				routingNetwork.addNode(routingNode);
				wrapper.outLinks.put(routingNode, bitSet);
				
				// all in-links have the same set of allowed to-nodes which is stored in this node's out-links-array
				for (Link inLink : inLinks) toNodes.put(inLink.getId(), routingNode);					
			}
		}
		log.info("Original network contains " + network.getNodes().size() + 
				" nodes, routing network taking turn restrictions into accounts contains " + this.nodeArrayIndexCounter + " nodes, i.e. " +
				(this.nodeArrayIndexCounter - network.getNodes().size()) + " additional ones.");
		
		// Create regular links in the same order as in the network.
		// The TravelTimeCalculator might use their indices to access its internal data structures.
		Map<Id<Link>, LinkToLinkArrayRoutingNetworkLink> regularRoutingLinks = new HashMap<>();
		for (Link link : network.getLinks().values()) {
			RoutingNetworkNode fromNode = routingNetwork.getNodes().get(link.getFromNode().getId());
			RoutingNetworkNode toNode = routingNetwork.getNodes().get(link.getToNode().getId());
			LinkToLinkArrayRoutingNetworkLink dijkstraLink = createRoutingNetworkLink(link, fromNode, toNode);
			regularRoutingLinks.put(dijkstraLink.getId(), dijkstraLink);
		}
		
		// create links
		Set<RoutingNetworkLink> usedLinks = new HashSet<>();
		Map<Id<Link>, Map<Id<Node>, LinkToLinkRoutingNetworkLink>> routingLinks = new HashMap<>();
		for (Link link : network.getLinks().values()) {
			
			LinkToLinkArrayRoutingNetworkLink regularRoutingLink = regularRoutingLinks.get(link.getId());
						
			// We have to create one link for each copy of a node that allows using the link
			Map<Id<Node>, LinkToLinkRoutingNetworkLink> map = new HashMap<>();
			routingLinks.put(link.getId(), map);
			NodeWrapper wrapper = wrappers.get(link.getFromNode().getId());
			int index = wrapper.toLinks.indexOf(link);
			for (Entry<RoutingNetworkNode, BitSet> entry : wrapper.outLinks.entrySet()) {
				BitSet bitSet = entry.getValue();
				
				// if the node allows using the link, create a routing-link using the node as from-node
				if (bitSet.get(index)) {
					// use the currently handled node as from-node
					RoutingNetworkNode fromNode = entry.getKey();
					
					// use to-node that respects the turn restrictions
					RoutingNetworkNode toNode = toNodes.get(link.getId());
					
					// use regularRoutingLinks if possible, otherwise create a clone with the same array index (see comment about TravelTimeCalculator)
					LinkToLinkArrayRoutingNetworkLink routingLink;
					if (regularRoutingLink.getFromNode() == fromNode && regularRoutingLink.getToNode() == toNode) routingLink = regularRoutingLink;
					else routingLink = cloneRoutingNetworkLink(regularRoutingLink, fromNode, toNode);

					usedLinks.add(routingLink);
					
//					RoutingNetworkLink routingLink = createRoutingNetworkLink(link, fromNode, toNode);
					map.put(fromNode.getId(), routingLink);
					
					/*
					 * In case the link has turn restrictions, its to-node is not a regular node. Then, we need
					 * to add the link to the networks links map to allow the router to replace an unrestricted
					 * to-node with the (correct) restricted one.
					 * 
					 * In case the link starts at a restricted node, we also have to add it to the map allowing
					 * the router to find out from which duplicated node(s) the link can be accessed.
					 * 
					 * Note: this is quite ugly since we store only a single version of the link in the map.
					 * However, for our purpose it is okay since we need only the to-node of the link which is
					 * the same for all versions.
					 */
//					if (!network.getNodes().containsKey(fromNode.getId()) || !network.getNodes().containsKey(toNode.getId())) {
//						routingNetwork.addLink(routingLink);				
//					}
					routingNetwork.addLink(routingLink);
				}
				
				/*
				 * Tell each duplicated link from which nodes it and its duplicates can be reached. The routing
				 * algorithm needs this to check whether it can be terminated.
				 * Criteria: can the to-link be reached from the currently handled node? In case the link was 
				 * duplicated, there are multiple nodes that lead to the searched link.
				 * Note: if there is only a single node leading to the link (even if is a duplicated node!),
				 * we can skip this step! 
				 */
				if (map.size() > 1) {
					RoutingNetworkNode[] nodes = new RoutingNetworkNode[map.size()];
					int i = 0;
					for (LinkToLinkRoutingNetworkLink routingLink : map.values()) {
						nodes[i++] = routingLink.getFromNode();
						routingLink.setFromNodes(nodes);
					}
				}
			}
			
//			// THIS DOES NOT WORK!!!
//			// use regular from-node - it should only be used to build the path after the routing process
//			RoutingNetworkNode fromNode = routingNetwork.getNodes().get(link.getFromNode().getId());
//
//			// use to-node that respects the turn restrictions
//			RoutingNetworkNode toNode = toNodes.remove(link.getId());
//			
//			RoutingNetworkLink routingLink = createRoutingNetworkLink(link, fromNode, toNode);
//			routingLinks.put(routingLink.getId(), routingLink);
//			
//			/*
//			 * In case the link has turn restrictions, its to-node is not a regular node. Then, we need
//			 * to add the link to the networks links map to allow the router to replace an unrestricted
//			 * to-node with the (correct) restricted one.
//			 */
//			if (!network.getNodes().containsKey(toNode.getId())) {
//				routingNetwork.addLink(routingLink);				
//			}
		}
		
		// set links
		for (NodeWrapper wrapper : wrappers.values()) {
			
			for (Entry<RoutingNetworkNode, BitSet> entry : wrapper.outLinks.entrySet()) {
				RoutingNetworkNode node = entry.getKey();
				BitSet bitSet = entry.getValue();
				
				int index = 0;
				LinkToLinkRoutingNetworkLink[] array = new LinkToLinkRoutingNetworkLink[bitSet.cardinality()];
				for (int i = 0; i < bitSet.length(); i++) {
					if (bitSet.get(i)) {
						Link outLink = wrapper.toLinks.get(i);
						Map<Id<Node>, LinkToLinkRoutingNetworkLink> map = routingLinks.get(outLink.getId());
						LinkToLinkRoutingNetworkLink routingLink = map.get(node.getId());
						array[index++] = routingLink;
					}
				}
				node.setOutLinksArray(array);
			}
		}
		log.info("Original network contains " + network.getLinks().size() + 
				" links, routing network taking turn restrictions into accounts contains " + usedLinks.size() + " links, i.e. " +
				(usedLinks.size() - network.getLinks().size()) + " additional ones.");
		
		return routingNetwork;
	}
	
	/*
	 * Wrapper class to collect links connected to a node and their turn restrictions (respectively their valid to-links).
	 * The links map stores the valid to-links for all in-links. The BitSet (key) refers to the toLinks array. If
	 * a bit is set, the corresponding to-Links can be reached from the links in the List (values).
	 * I.e. we have to create one node for each entry in the map. For nodes without turn restrictions, only a single
	 * node will be created.  
	 */
	private static class NodeWrapper {
		Node node;	// the underlying network node
		List<Link> toLinks = new ArrayList<>();	// the to-links of the underlying node
		Map<BitSet, List<Link>> links = new LinkedHashMap<>();	// links and their to-links mapping
		Map<RoutingNetworkNode, BitSet> outLinks = new LinkedHashMap<>();	// BitSet that defines which out-links are valid for a node 
	}
	
	public ArrayRoutingNetworkNode createRoutingNetworkNode(Node node, int numOutLinks) {
		return new ArrayRoutingNetworkNode(node, numOutLinks, this.nodeArrayIndexCounter++);
	}

	public LinkToLinkArrayRoutingNetworkLink createRoutingNetworkLink(Link link, RoutingNetworkNode fromNode, RoutingNetworkNode toNode) {
		return new LinkToLinkArrayRoutingNetworkLink(link, fromNode, toNode, this.linkArrayIndexCounter++);
	}

	private LinkToLinkArrayRoutingNetworkLink cloneRoutingNetworkLink(LinkToLinkArrayRoutingNetworkLink link, RoutingNetworkNode fromNode, RoutingNetworkNode toNode) {
		return new LinkToLinkArrayRoutingNetworkLink(link.getLink(), fromNode, toNode, link.getArrayIndex());
	}
}