/* *********************************************************************** *
 * project: org.matsim.*
 * ArrayRoutingNetwork.java
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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

//public class LinkToLinkArrayRoutingNetwork extends LinkToLinkAbstractRoutingNetwork {
public class LinkToLinkArrayRoutingNetwork extends ArrayRoutingNetwork implements LinkToLinkRoutingNetwork {

	/*package*/ final Map<Id<Link>, LinkToLinkRoutingNetworkLink> links = new HashMap<>();
	
	public LinkToLinkArrayRoutingNetwork(final Network network) {
		super(network);
	}
	
	/*package*/ void addLink(LinkToLinkRoutingNetworkLink ll) {
		this.links.put(ll.getId(), ll);
	}

	@Override
	public Map<Id<Link>, LinkToLinkRoutingNetworkLink> getLinks() {
		return this.links;
	}

	@Override
	public Link removeLink(Id<Link> linkId) {
		return this.links.remove(linkId);
	}
}