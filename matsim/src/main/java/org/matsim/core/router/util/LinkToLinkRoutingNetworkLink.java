/* *********************************************************************** *
 * project: org.matsim.*
 * RoutingNetworkLink.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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


public interface LinkToLinkRoutingNetworkLink extends RoutingNetworkLink {

	/*
	 * In case the network includes turn restrictions, some links are duplicated to represent the restrictions.
	 * The routing algorithm needs to know from which nodes a link can be reached.
	 * In case the link is NOT duplicated, this returns null!
	 */
	public RoutingNetworkNode[] getFromNodes();
	
	public void setFromNodes(RoutingNetworkNode[] fromNodes);
}