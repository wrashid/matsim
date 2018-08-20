package org.matsim.contrib.carsharing.bikeshare;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.mobsim.framework.MobsimAgent;
import org.matsim.core.mobsim.framework.PlanAgent;
import org.matsim.core.mobsim.qsim.agents.WithinDayAgentUtils;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Inject;


public class BikeshareDepartureHandler implements DepartureHandler {

	@Inject
	BikeshareFleet bikeFleet;

	@Inject
	Network network;

	@Override
	public boolean handleDeparture(double now, MobsimAgent agent, Id<Link> linkId) {
		if (agent instanceof PlanAgent) {
			if (agent.getMode().startsWith("access_walk_bike")) {
				// Plan plan = WithinDayAgentUtils.getModifiablePlan( agent ) ;
				Link link = network.getLinks().get(linkId);
				bikeFleet.addRental(link.getCoord(), now);

				Plan plan = WithinDayAgentUtils.getModifiablePlan(agent);
				final Integer planElementsIndex = WithinDayAgentUtils.getCurrentPlanElementIndex(agent);
				final Leg accessLeg = (Leg) plan.getPlanElements().get(planElementsIndex);
				final Leg leg = (Leg) plan.getPlanElements().get(planElementsIndex + 1);
				final Leg egressLeg = (Leg) plan.getPlanElements().get(planElementsIndex + 2);
				Id<Vehicle> bikeId = bikeFleet.getAndRemoveClosest(link.getCoord(), agent.getId());
				//todo: if there is no bike in 500m replace it with walk
				if (bikeId == null) {
					agent.setStateToAbort(now);
					return true;
				}
				Coord bikeCoord = this.bikeFleet.getBikeCoordMap().get(bikeId);

				if (CoordUtils.calcEuclideanDistance(link.getCoord(), bikeCoord) > 500) {
				//	agent.setStateToAbort(now);
					//return the bike
					this.bikeFleet.addBike(this.bikeFleet.getBikeCoordMap().get(bikeId), bikeId);
					accessLeg.setMode("walk");
					accessLeg.setTravelTime(CoordUtils.calcEuclideanDistance(link.getCoord(), network.getLinks().get(leg.getRoute().getEndLinkId()).getCoord()) * 1.05 / 1.38);
					plan.getPlanElements().remove(planElementsIndex + 1);
					plan.getPlanElements().remove(planElementsIndex + 2);
					return true;
				}
					
				double accessTime = CoordUtils.calcEuclideanDistance(link.getCoord(), bikeCoord) * 1.05 / 1.38;
				
				accessLeg.setTravelTime(accessTime);
				accessLeg.getRoute().setTravelTime(accessTime);
				accessLeg.getRoute().setEndLinkId(NetworkUtils.getNearestLinkExactly(network, bikeCoord).getId());
				double travelTime = CoordUtils.calcEuclideanDistance(bikeCoord, network.getLinks().get(leg.getRoute().getEndLinkId()).getCoord()) * 1.4 / 3.88;
				leg.setTravelTime(travelTime);
				leg.getRoute().setTravelTime(travelTime);
				leg.getRoute().setStartLinkId(NetworkUtils.getNearestLinkExactly(network, bikeCoord).getId());
				egressLeg.setTravelTime(0.0);
				egressLeg.getRoute().setTravelTime(0.0);
				
			}

		}

		return false;
	}

}
