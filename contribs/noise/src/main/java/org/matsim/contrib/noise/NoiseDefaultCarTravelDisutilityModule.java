/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.noise;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.contrib.noise.routing.NoiseTollTimeDistanceTravelDisutilityFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;

/**
* @author ikaddoura
*/

public class NoiseDefaultCarTravelDisutilityModule extends AbstractModule {
	private static final Logger log = Logger.getLogger(NoiseDefaultCarTravelDisutilityModule.class);

	@Override
	public void install() {
		
		NoiseConfigGroup noiseParameters = ConfigUtils.addOrGetModule(this.getConfig(), NoiseConfigGroup.class);
				
		if (noiseParameters.isInternalizeNoiseDamages()) {
			
			log.info("Replacing the default travel disutility for the transport mode 'car' by a travel distuility which accounts for noise tolls.");
			
			if (noiseParameters.isComputeAvgNoiseCostPerLinkAndTime() == false) {
				log.warn("The travel disutility which accounts for noise tolls requires the computation of average noise cost per link and time bin."
						+ "Setting the value 'computeAvgNoiseCostPerLinkAndTime' to 'true'...");
				noiseParameters.setComputeAvgNoiseCostPerLinkAndTime(true);
			}
			
			bindCarTravelDisutilityFactory()
					.toProvider( new DisutilityProvider( TransportMode.car ) )
					.asEagerSingleton();
		}		
	}

    private static class DisutilityProvider implements Provider<TravelDisutilityFactory> {
    	private final String mode;
    	@Inject
        public Scenario scenario;

        private DisutilityProvider(String mode) {
            this.mode = mode;
        }

        @Override
		public TravelDisutilityFactory get() {
			return new NoiseTollTimeDistanceTravelDisutilityFactory(
					new RandomizingTimeDistanceTravelDisutilityFactory(
							scenario.getPopulation().getPersonAttributes(),
							mode,
							scenario.getConfig()),
					scenario.getConfig().planCalcScore() );
		}
    }
}

