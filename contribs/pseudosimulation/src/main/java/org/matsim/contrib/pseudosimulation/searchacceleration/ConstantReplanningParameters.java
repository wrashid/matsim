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
import org.matsim.core.router.util.TravelTime;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class ConstantReplanningParameters implements ReplanningParameterContainer {

	private final AccelerationConfigGroup accelerationConfig;

	private Map<Id<Link>, Double> linkWeights;

	private final Network network;

	public ConstantReplanningParameters(final AccelerationConfigGroup accelerationConfig, final Network network) {
		this.accelerationConfig = accelerationConfig;
		this.linkWeights = accelerationConfig.newLinkWeights(network);
		this.network = network;
	}

	@Override
	public double getMeanLambda(final int iteration) {
		return this.accelerationConfig.getMeanReplanningRate();
	}

	@Override
	public double getDelta(final int iteration, final Double deltaN2) {
		if (this.accelerationConfig.getRegularizationType() == AccelerationConfigGroup.RegularizationType.absolute) {
			return this.accelerationConfig.getRegularizationWeight();
		} else if (this.accelerationConfig
				.getRegularizationType() == AccelerationConfigGroup.RegularizationType.relative) {
			return this.accelerationConfig.getRegularizationWeight() * this.getMeanLambda(iteration) * deltaN2;
		} else {
			throw new RuntimeException(
					"Unhandled regularizationType: " + this.accelerationConfig.getRegularizationType());
		}
	}

	@Override
	public boolean isCongested(final Object linkId, final int bin, final TravelTime travelTimes) {
		if (!(linkId instanceof Id<?>)) {
			throw new RuntimeException("linkId is of type " + linkId.getClass().getSimpleName());
		}
		final Link link = this.network.getLinks().get(linkId);
		return this.accelerationConfig.isCongested(link, bin, travelTimes);
	}

	private double congestionFactor(final Object linkId, final int bin, final TravelTime travelTimes) {
		if (!(linkId instanceof Id<?>)) {
			throw new RuntimeException("linkId is of type " + linkId.getClass().getSimpleName());
		}
		final Link link = this.network.getLinks().get(linkId);
		return this.accelerationConfig.congestionFactor(link, bin, travelTimes);
	}

	@Override
	public double getWeight(final Object linkId, final int bin, final TravelTime travelTimes) {
		if (!(linkId instanceof Id<?>)) {
			throw new RuntimeException("linkId is of type " + linkId.getClass().getSimpleName());
		}
		if (this.isCongested(linkId, bin, travelTimes)) {
			if (this.accelerationConfig.getCongestionProportionalWeighting()) {
				return this.linkWeights.get(linkId) * this.congestionFactor(linkId, bin, travelTimes);
			} else {
				return this.linkWeights.get(linkId);
			}
		} else {
			return 0.0;
		}
	}

	// @Override
	// public boolean isCongested(final Object linkId, int time_s) {
	// return false;
	// }

}
