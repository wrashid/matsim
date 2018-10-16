package org.matsim.contrib.spatialDrt.bayInfrastructure;

import org.matsim.contrib.spatialDrt.run.AtodConfigGroup;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Bay {
    private final TransitStopFacility transitStop;
    private final Id<Link> linkId;
    private final double capacity;


    private Queue<Id<Vehicle>> vehicles = new ConcurrentLinkedQueue<>();
    private Queue<Id<Vehicle>> dwellingVehicles = new ConcurrentLinkedQueue<>();
    private double dwellLength = 0;

    public Bay(TransitStopFacility transitStop, double linkLength, double minBaySize){
        this.transitStop = transitStop;
        this.linkId = transitStop.getLinkId();
        if (transitStop.getAttributes().getAttribute("capacity") == null){
            this.capacity = Double.POSITIVE_INFINITY;
        }else if ((double) transitStop.getAttributes().getAttribute("capacity") == 0) {
            this.capacity = Double.max(linkLength, minBaySize);
        }else{
            this.capacity = (double) transitStop.getAttributes().getAttribute("capacity");
        }
    }

    public Bay(TransitStopFacility transitStop, double linkLength, AtodConfigGroup drtconfig){
        this.transitStop =transitStop;
        this.linkId = transitStop.getLinkId();
        switch (drtconfig.getDoor2DoorStop()){
            case infinity:
                this.capacity = Double.POSITIVE_INFINITY;
                break;
            case linkLength:
                this.capacity = Double.max(linkLength, drtconfig.getMinBaySize());
                break;
            default:
                throw new RuntimeException("No such door-to-door stop strategy!");
        }
    }

    public TransitStopFacility getTransitStop() {
        return transitStop;
    }

    public Id<Link> getLinkId() {
        return linkId;
    }

    public double getCapacity() {
        return capacity;
    }


    public void addVehicle(Id<Vehicle> vid){
        if (dwellLength > capacity){
            throw new RuntimeException("too many dwelling vehicles!");
        }
        if (dwellingVehicles.contains(vid) || vehicles.contains(vid)){
            return;
        }
        double vehicleLength = VehicleLength.getLength(vid);
        if (dwellLength +  vehicleLength >= capacity){
            if (!vehicles.contains(vid)) {
                vehicles.add(vid);
            }
        }else{
            dwellLength = dwellLength + vehicleLength;
            dwellingVehicles.add(vid);
        }
    }

    public void removeVehicle(Id<Vehicle> vid){
        if(!dwellingVehicles.contains(vid) && !vehicles.contains(vid)){
            String dwell = new String();
            String vehs = new String();
            for (Id<Vehicle> dvehid : dwellingVehicles){
                dwell = dwell + ";" + dvehid.toString();
            }
            for (Id<Vehicle> vehid : vehicles){
                vehs = vehs + ";" + vehid.toString();
            }
            throw new RuntimeException("vid: " + vid.toString() + ", transitStop: " + transitStop.getId().toString() + ", dwellV: " + dwell + ", v: " + vehs);
        }
        vehicles.remove(vid);
        if (dwellingVehicles.remove(vid)) {
            double vehicleLength = VehicleLength.getLength(vid);
            dwellLength = dwellLength - vehicleLength;
            Id<Vehicle> vehicleId = vehicles.peek();
            double vehLength = vehicleId==null?Double.POSITIVE_INFINITY:VehicleLength.getLength(vehicleId);
            while (dwellLength + vehLength <= capacity && isFull()) {
                dwellLength = dwellLength + vehLength;
                dwellingVehicles.add(vehicleId);
                vehicles.poll();
                vehicleId = vehicles.peek();
                vehLength = vehicleId==null?Double.POSITIVE_INFINITY:VehicleLength.getLength(vehicleId);
            }
        }
    }

    public Queue<Id<Vehicle>> getVehicles() {
        return vehicles;
    }

    public Queue<Id<Vehicle>> getDwellingVehicles() {
        return dwellingVehicles;
    }

    public boolean isFull() {
        return vehicles.size() > 0;
    }

    public double getDwellingLength() {
        return dwellLength;
    }
}
