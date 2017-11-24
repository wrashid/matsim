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

package org.matsim.core.router;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.router.costcalculators.LinkToLinkTravelDisutilityFactory;
import org.matsim.core.router.util.LinkToLinkLeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.lanes.data.Lanes;

public class LinkToLinkRoutingV2
    implements Provider<RoutingModule>
{
    private final String mode;

    @Inject
    PopulationFactory populationFactory;

    @Inject
    LinkToLinkLeastCostPathCalculatorFactory l2lLeastCostPathCalcFactory;

    @Inject
    Map<String, LinkToLinkTravelDisutilityFactory> l2lTravelDisutilities;

    @Inject
    Network network;
    
    @Inject
    Lanes lanes;

    @Inject
    LinkToLinkTravelTime l2lTravelTimes;


    public LinkToLinkRoutingV2(String mode)
    {
        this.mode = mode;
    }


    @Override
    public RoutingModule get()
    {
    		return new LinkToLinkRoutingModuleV2(mode, populationFactory, network, lanes,
                l2lLeastCostPathCalcFactory, l2lTravelDisutilities.get(mode), l2lTravelTimes);
    }
}