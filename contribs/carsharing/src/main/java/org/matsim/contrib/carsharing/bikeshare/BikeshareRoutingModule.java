package org.matsim.contrib.carsharing.bikeshare;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.carsharing.router.CarsharingRoute;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.EmptyStageActivityTypes;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.facilities.Facility;

public class BikeshareRoutingModule implements RoutingModule {
	
	public BikeshareRoutingModule() {		
		
	}
	@Override
	public List<? extends PlanElement> calcRoute(Facility fromFacility,
			Facility toFacility, double departureTime, Person person) {
		
		final List<PlanElement> trip = new ArrayList<PlanElement>();
		final Leg accessLeg = PopulationUtils.createLeg("access_walk_bike");				
		final Leg leg1 = PopulationUtils.createLeg("bikeshare");
		final Leg egressLeg = PopulationUtils.createLeg("egress_walk_bike");
		CarsharingRoute route1 = new CarsharingRoute(fromFacility.getLinkId(), toFacility.getLinkId());
		accessLeg.setRoute(route1);
		egressLeg.setRoute(route1);
		leg1.setRoute(route1);
		trip.add(accessLeg);
		trip.add( leg1 );	
		trip.add(egressLeg);				
		return trip;
	}

	@Override
	public StageActivityTypes getStageActivityTypes() {
		// TODO Auto-generated method stub
		
		return EmptyStageActivityTypes.INSTANCE;
	}
	
	
}
