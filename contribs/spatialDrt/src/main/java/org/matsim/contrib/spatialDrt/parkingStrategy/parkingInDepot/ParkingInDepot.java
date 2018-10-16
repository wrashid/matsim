package org.matsim.contrib.spatialDrt.parkingStrategy.parkingInDepot;


import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.spatialDrt.parkingStrategy.ParkingStrategy;
import org.matsim.contrib.spatialDrt.parkingStrategy.parkingInDepot.Depot.Depot;
import org.matsim.contrib.spatialDrt.parkingStrategy.parkingInDepot.Depot.DepotManager;
import org.matsim.contrib.util.distance.DistanceUtils;

public class ParkingInDepot implements ParkingStrategy {
    DepotManager depotManager;

    @Inject
    public ParkingInDepot(DepotManager depotManager){
        this.depotManager = depotManager;
    }


    @Override
    public ParkingStrategy.ParkingLocation parking(Vehicle vehicle, double time) {
        Depot bestDepot = findDepots(vehicle);
        if (bestDepot == null){
            return null;
        }
        return new ParkingLocation(vehicle.getId(), bestDepot.getLink());
    }

    private Depot findDepots(Vehicle vehicle) {
        DrtStayTask currentTask = (DrtStayTask)vehicle.getSchedule().getCurrentTask();
        Link currentLink = currentTask.getLink();
        if (depotManager.isVehicleInDepot(vehicle)) {
            return null;// stay where it is
        }

        Depot bestDepot = null;
        double bestDistance = Double.MAX_VALUE;
        for (Depot d : depotManager.getDepots(vehicle.getCapacity()).values()) {
            if (d.getCapacity() > d.getNumOfVehicles()){
                double currentDistance = DistanceUtils.calculateSquaredDistance(currentLink.getCoord(), d.getLink().getCoord());
                if (currentDistance < bestDistance) {
                    bestDistance = currentDistance;
                    bestDepot = d;
                }
            }
        }
        if (bestDepot == null){
            throw new RuntimeException("All depots are full!!");
        }
        bestDepot.addVehicle(vehicle.getId());
        depotManager.registerVehicle(vehicle.getId(),bestDepot.getId());
        return bestDepot;
    }

    @Override
    public void departing(Vehicle vehicle, double time) {
        depotManager.vehicleLeavingDepot(vehicle);
    }

    @Override
    public ParkingStrategy.Strategies getCurrentStrategy(Id<Vehicle> vehicleId) {
        return Strategies.ParkingInDepot;
    }

}
