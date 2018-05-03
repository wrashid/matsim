/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class LinkWeightContainer {

	private Map<Id<Link>, Double> data;

	public static LinkWeightContainer newUniformLinkWeights(final Network network) {
		final Map<Id<Link>, Double> weights = new LinkedHashMap<>();
		for (Link link : network.getLinks().values()) {
			weights.put(link.getId(), 1.0);
		}
		return new LinkWeightContainer(weights);
	}

	public static LinkWeightContainer newOneOverCapacityLinkWeights(final Network network) {
		final Map<Id<Link>, Double> weights = new LinkedHashMap<>();
		for (Link link : network.getLinks().values()) {
			if (link.getCapacity() <= 1e-6) {
				throw new RuntimeException("link " + link.getId() + " has capacity " + link.getCapacity());
			}
			weights.put(link.getId(), 1.0 / link.getCapacity());
		}
		return new LinkWeightContainer(weights);
	}

	public LinkWeightContainer(Map<Id<Link>, Double> data) {
		this.data = data;
	}

	public Double getWeight(final Object linkId) {
		return this.data.get(linkId);
	}

}
