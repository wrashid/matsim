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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ConstantReplanningParameters implements ReplanningParameterContainer<Id<Link>> {

	private final double meanLambda;

	private final double delta;

	private final double flowCapacityFactor;

	private final double timeBinSize_s;

	private Map<Id<Link>, Double> linkWeights;

	private final Network network;

	public ConstantReplanningParameters(final double meanLambda, final double delta, final double flowCapacityFactor,
			final double timeBinSize_s, final Map<Id<Link>, Double> linkWeights, final Network network) {
		this.meanLambda = meanLambda;
		this.delta = delta;
		this.flowCapacityFactor = flowCapacityFactor;
		this.timeBinSize_s = timeBinSize_s;
		this.linkWeights = linkWeights;
		this.network = network;
	}

	@Override
	public double getMeanLambda(final int iteration) {
		return this.meanLambda;
	}

	@Override
	public double getDelta(final int iteration) {
		return this.delta;
	}

	@Override
	public Double getWeight(final Id<Link> linkId, final double cnt_veh_timeBin) {
		final Link link = this.network.getLinks().get(linkId);
		final double threshold_veh = this.flowCapacityFactor * this.timeBinSize_s * link.getFlowCapacityPerSec();
		if (cnt_veh_timeBin < threshold_veh) {
			return 0.0;
		} else {
			return this.linkWeights.get(linkId);
		}
	}
}
