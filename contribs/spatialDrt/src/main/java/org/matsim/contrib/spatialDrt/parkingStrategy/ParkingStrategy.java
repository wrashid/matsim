package org.matsim.contrib.spatialDrt.parkingStrategy;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;

public interface ParkingStrategy {
    public enum Strategies{
        AlwaysRoaming,
        NoParkingStrategy,
        ParkingInDepot,
        ParkingOntheRoad,
        MixedParking;
    }

    public class ParkingLocation {
        public final Id<Vehicle> vid;
        public final Link link;

        public ParkingLocation(Id<Vehicle> vid, Link link) {
            this.vid = vid;
            this.link = link;
        }
    }

    /**
     * This method is called at each re-balancing step (interval defined in config).
     */

    ParkingLocation parking(Vehicle vehicle, double time);

    void departing(Vehicle vehicle, double time);

    Strategies getCurrentStrategy(Id<Vehicle> vehicleId);

}