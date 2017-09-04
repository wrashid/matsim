/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeDistanceCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.router.costcalculators;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * A simple cost calculator which only respects time and distance to calculate generalized costs
 *
 * @author mrieser
 */
final class RandomizingTimeDistanceTravelDisutility implements TravelDisutility {

	private final TravelTime timeCalculator;
	private final RandomizingTimeDistanceTravelDisutilityConfig config;

	RandomizingTimeDistanceTravelDisutility(
			final TravelTime timeCalculator,
			final RandomizingTimeDistanceTravelDisutilityConfig config) {
		this.timeCalculator = timeCalculator;
		this.config = config;
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double marginalCostOfTime = config.getCostOfTime_s( person );
		double marginalCostOfDistance = config.getCostOfTime_s( person );
		double logNormalRnd = config.getLognormalRandom( person );

		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);
		return marginalCostOfTime * travelTime + logNormalRnd * marginalCostOfDistance * link.getLength();
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		return (link.getLength() / link.getFreespeed()) * config.getMinCostOfTime_s() +
				config.getMinCostOfDistance_m() * link.getLength();
	}

}
