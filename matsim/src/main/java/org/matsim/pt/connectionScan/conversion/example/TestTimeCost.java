package org.matsim.pt.connectionScan.conversion.example;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.CustomDataManager;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

public class TestTimeCost implements TravelTime, TransitTravelDisutility {

    private final Map<Id<Link>, Double> travelTimes = new HashMap<>();
    private final Map<Id<Link>, Double> travelCosts = new HashMap<>();

    public void setData(final Id<Link> id, final double travelTime, final double travelCost) {
        this.travelTimes.put(id, travelTime);
        this.travelCosts.put(id, travelCost);
    }

    @Override
    public double getLinkTravelTime(final Link link, final double time, Person person, Vehicle vehicle) {
        return this.travelTimes.get(link.getId());
    }

    @Override
    public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager) {
        return this.travelCosts.get(link.getId());
    }

    @Override
    public double getWalkTravelTime(Person person, Coord coord, Coord toCoord) {
        return 0;
    }

    @Override
    public double getWalkTravelDisutility(Person person, Coord coord, Coord toCoord) {
        return 0;
    }

}
