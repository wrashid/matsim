/* *********************************************************************** *
 * project: org.matsim.*
 * RandomizingTimeDistanceTravelDisutilityFactory.java
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

import org.apache.log4j.Logger;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import java.util.Collections;
import java.util.Set;

public class RandomizingTimeDistanceTravelDisutilityFactory implements TravelDisutilityFactory {
	private static final Logger log = Logger.getLogger( RandomizingTimeDistanceTravelDisutilityFactory.class ) ;

	private static int wrnCnt = 0 ;
	private static int normalisationWrnCnt = 0;

	private final String mode;
	private double sigma = 0. ;
	private final PlanCalcScoreConfigGroup cnScoringGroup;

	public RandomizingTimeDistanceTravelDisutilityFactory(final String mode, PlanCalcScoreConfigGroup cnScoringGroup ) {
		this.mode = mode;
		this.cnScoringGroup = cnScoringGroup;
	}

	@Override
	public TravelDisutility createTravelDisutility(
			final TravelTime travelTime) {
		logWarningsIfNecessary( cnScoringGroup );

		RandomizingTimeDistanceTravelDisutilityConfig config =
				new RandomizingTimeDistanceTravelDisutilityConfig(
						cnScoringGroup,
						mode,
						sigma );
		return new RandomizingTimeDistanceTravelDisutility( travelTime , config );
	}

	private void logWarningsIfNecessary(final PlanCalcScoreConfigGroup cnScoringGroup) {
		if ( wrnCnt < 1 ) {
			wrnCnt++ ;
			if ( cnScoringGroup.getModes().get( mode ).getMonetaryDistanceRate() > 0. ) {
				log.warn("Monetary distance cost rate needs to be NEGATIVE to produce the normal " +
						"behavior; just found positive.  Continuing anyway.") ;
			}

			final Set<String> monoSubpopKeyset = Collections.singleton( null );
			if ( !cnScoringGroup.getScoringParametersPerSubpopulation().keySet().equals( monoSubpopKeyset ) ) {
				log.warn( "Scoring parameters are defined for different subpopulations." +
						" The routing disutility will only consider the ones of the default subpopulation.");
				log.warn( "This warning can safely be ignored if disutility of traveling only depends on travel time.");
			}
		}
	}

	public RandomizingTimeDistanceTravelDisutilityFactory setSigma(double val ) {
		this.sigma = val ;
		return this;
	}
}
