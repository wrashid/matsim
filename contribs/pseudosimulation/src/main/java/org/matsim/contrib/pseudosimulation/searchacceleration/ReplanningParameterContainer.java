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
import org.matsim.core.router.util.TravelTime;

import floetteroed.utilities.Units;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public interface ReplanningParameterContainer {

	public static Map<Id<Link>, Double> newUniformLinkWeights(final Network network) {
		final Map<Id<Link>, Double> weights = new LinkedHashMap<>();
		for (Link link : network.getLinks().values()) {
			weights.put(link.getId(), 1.0);
		}
		return weights;
	}

	public static Map<Id<Link>, Double> newOneOverCapacityLinkWeights(final Network network) {
		final Map<Id<Link>, Double> weights = new LinkedHashMap<>();
		for (Link link : network.getLinks().values()) {
			final double cap_veh_h = link.getFlowCapacityPerSec() * Units.VEH_H_PER_VEH_S;
			if (cap_veh_h <= 1e-6) {
				throw new RuntimeException("link " + link.getId() + " has capacity " + cap_veh_h + " veh/h");
			}
			weights.put(link.getId(), 1.0 / cap_veh_h);
		}
		return weights;
	}

	public double getMeanLambda(int iteration);

	public double getDelta(int iteration, Double deltaN2);
	
	public boolean isCongested(Object locObj, int timeBin, TravelTime travelTimes);
	
	public double getWeight(Object locObj, int timeBin, TravelTime travelTimes);
	
	// public boolean isCongested(Object locObj, int time_s);

}
