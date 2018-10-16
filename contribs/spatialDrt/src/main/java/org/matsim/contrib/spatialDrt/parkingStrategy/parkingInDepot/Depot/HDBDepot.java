package org.matsim.contrib.spatialDrt.parkingStrategy.parkingInDepot.Depot;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.data.Vehicle;

import java.util.ArrayList;

public class HDBDepot implements Depot{

    private Id<Depot> id;
    private Link link;
    private double capacity;
    private ArrayList<Id<Vehicle>> vehicles = new ArrayList<>();

    public HDBDepot(Id<Depot> depotId, Link link, double capacity){
        this.id = depotId;
        this.link = link;
        this.capacity = capacity;
    }
    public Link getLink() {
        return link;
    }

    public double getCapacity() {
        return capacity;
    }

    public void addVehicle(Id<Vehicle> vid){
        if (vehicles.contains(vid)){
            return;
        }
        if (vehicles.size() == capacity){
            throw new RuntimeException("The depot " + link.getId() + " is full!!");
        }
        vehicles.add(vid);
    }

    public void removeVehicle(Id<Vehicle> vid){
        vehicles.remove(vid);
    }

    public double getNumOfVehicles() {
        return vehicles.size();
    }

    public Id<Depot> getId() {
        return id;
    }
    @Override
    public DepotType getDepotType() {
        return DepotType.HDB;
    }
}
