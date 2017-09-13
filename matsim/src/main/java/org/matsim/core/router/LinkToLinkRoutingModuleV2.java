/* *********************************************************************** *
 * project: org.matsim.*
 * InvertedNetworkLegRouter
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
package org.matsim.core.router;

import java.util.Arrays;
import java.util.List;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.costcalculators.LinkToLinkTravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.LinkToLinkLeastCostPathCalculator;
import org.matsim.core.router.util.LinkToLinkLeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LinkToLinkTravelDisutility;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.facilities.Facility;
import org.matsim.lanes.data.Lanes;

public class LinkToLinkRoutingModuleV2 implements RoutingModule {
	
	private final Network network;
    private final LinkToLinkLeastCostPathCalculator leastCostPathCalculator;
    private final PopulationFactory populationFactory;
    private final String mode;

    public LinkToLinkRoutingModuleV2(final String mode, final PopulationFactory populationFactory, final Network network, 
    		final Lanes lanes, final LinkToLinkLeastCostPathCalculatorFactory leastCostPathCalculatorFactory,
            final LinkToLinkTravelDisutilityFactory travelCostCalculatorFactory, final LinkToLinkTravelTime travelTime) {
    	this.network = network;
    	this.populationFactory = populationFactory;
    	this.mode = mode;
    	
        final LinkToLinkTravelDisutility travelCost = travelCostCalculatorFactory.createLinkToLinkTravelDisutility(travelTime);
        this.leastCostPathCalculator = leastCostPathCalculatorFactory.createPathCalculator(network, lanes, travelCost, travelTime);
    }

    @Override
    public List<? extends PlanElement> calcRoute(final Facility<?> fromFacility, final Facility<?> toFacility, final double departureTime, final Person person) {	      

    	Leg newLeg = this.populationFactory.createLeg( this.mode );
		
		Gbl.assertNotNull(fromFacility);
		Gbl.assertNotNull(toFacility);

		Link fromLink = this.network.getLinks().get(fromFacility.getLinkId());
		Link toLink = this.network.getLinks().get(toFacility.getLinkId());

		if (toLink != fromLink) {
			Path path = this.leastCostPathCalculator.calcLeastCostPath(fromLink, toLink, departureTime, person, null);
			if (path == null) throw new RuntimeException("No route found from link " + fromLink.getId() + " to link " + toLink.getId() + ".");
			NetworkRoute route = this.populationFactory.getRouteFactories().createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
			route.setTravelTime(path.travelTime);
			route.setTravelCost(path.travelCost);
			route.setDistance(RouteUtils.calcDistance(route, 1.0, 1.0, this.network));
			newLeg.setRoute(route);
			newLeg.setTravelTime(path.travelTime);
		} else {
			// create an empty route == staying on place if toLink == endLink
			// note that we still do a route: someone may drive from one location to another on the link. kai, dec'15
			NetworkRoute route = this.populationFactory.getRouteFactories().createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			route.setTravelTime(0);
			route.setDistance(0.0);
			newLeg.setRoute(route);
			newLeg.setTravelTime(0);
		}
		newLeg.setDepartureTime(departureTime);

		return Arrays.asList(newLeg);
    }

	@Override
	public StageActivityTypes getStageActivityTypes() {
		return EmptyStageActivityTypes.INSTANCE;
	}
    
	@Override
	public String toString() {
		return "[LinkToLinkRoutingModule: mode="+this.mode+"]";
	}
}