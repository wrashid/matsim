package org.matsim.contrib.spatialDrt.parkingStrategy.parkingInDepot.Depot;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.Vehicle;

import java.util.Map;

public interface DepotManager {



     void addDepot(Depot depot);

     Map<Id<Depot>, Depot> getDepots();
     Map<Id<Depot>, Depot> getDepots(Depot.DepotType depotType);

     boolean isVehicleInDepot(Vehicle vehicle);

     void registerVehicle(Id<Vehicle> vid, Id<Depot> did);

     Depot getDepotOfVehicle(Vehicle vehicle) ;

     void vehicleLeavingDepot(Vehicle vehicle) ;

     Map<Id<Depot>, Depot> getDepots(double capacity);
}
