package org.matsim.contrib.spatialDrt.parkingStrategy;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.spatialDrt.parkingStrategy.parkingInDepot.ParkingInDepot;
import org.matsim.contrib.spatialDrt.parkingStrategy.parkingOntheRoad.ParkingOntheRoad;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.mobsim.framework.events.MobsimBeforeCleanupEvent;
import org.matsim.core.mobsim.framework.listeners.MobsimBeforeCleanupListener;

import java.util.HashMap;
import java.util.Map;

public class MixedParkingStrategy implements ParkingStrategy {
    @Inject
    ParkingInDepot parkingInDepot;
    @Inject
    ParkingOntheRoad parkingOntheRoad;
    public static final double dayT0 = 7 * 3600;
    public static final double dayT1 = 20 * 3600;
    Map<Id<Vehicle>, ParkingStrategy.Strategies> parkingStrategiesPerVehicle = new HashMap<Id<Vehicle>, ParkingStrategy.Strategies>();


    @Override
    public ParkingStrategy.ParkingLocation parking(Vehicle vehicle, double time) {
        if (vehicle.getCapacity() <= 10 && !isDaytime(time)){
            parkingStrategiesPerVehicle.put(vehicle.getId(), Strategies.ParkingOntheRoad);
            return parkingOntheRoad.parking(vehicle, time);
        }
        parkingStrategiesPerVehicle.put(vehicle.getId(), Strategies.ParkingInDepot);
        return parkingInDepot.parking(vehicle, time);
    }

    @Override
    public void departing(Vehicle vehicle, double time) {
        switch (parkingStrategiesPerVehicle.get(vehicle.getId())){
            case ParkingOntheRoad:
                parkingOntheRoad.departing(vehicle,time);
                parkingStrategiesPerVehicle.remove(vehicle.getId());
                break;
            case ParkingInDepot:
                parkingInDepot.departing(vehicle,time);
                parkingStrategiesPerVehicle.remove(vehicle.getId());
                break;
            default:
                throw new RuntimeException("No parking strategy found!");
        }
    }

    @Override
    public ParkingStrategy.Strategies getCurrentStrategy(Id<Vehicle> vehicleId) {
        return parkingStrategiesPerVehicle.get(vehicleId);
    }

    private boolean isDaytime(double time){
        if (time >= dayT0 && time < dayT1){
            return true;
        }
        return false;
    }

}
