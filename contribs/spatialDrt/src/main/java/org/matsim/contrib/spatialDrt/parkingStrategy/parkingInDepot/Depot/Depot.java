package org.matsim.contrib.spatialDrt.parkingStrategy.parkingInDepot.Depot;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;

public interface Depot {
    public static enum DepotType{
        DEPOT,
        HDB;
    }
    public Link getLink();

    public double getCapacity();

    public void addVehicle(Id<Vehicle> vehicleId);


    public void removeVehicle(Id<Vehicle> vehicleId);

    public double getNumOfVehicles();

    public Id<Depot> getId();

    public DepotType getDepotType();
}
