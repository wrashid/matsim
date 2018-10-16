package org.matsim.contrib.spatialDrt.bayInfrastructure;
import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.core.network.NetworkChangeEvent;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;
import org.omg.SendingContext.RunTime;

import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BayByNumber {
    private final TransitStopFacility transitStop;
    private final Id<Link> linkId;
    private final double capacity;


    private Queue<Id<Vehicle>> vehicles = new ConcurrentLinkedQueue<>();
    private Queue<Id<Vehicle>> dwellingVehicles = new ConcurrentLinkedQueue<>();

    public BayByNumber(TransitStopFacility transitStop){
        this.transitStop = transitStop;
        this.linkId = transitStop.getLinkId();
        if (transitStop.getAttributes().getAttribute("capacity") == null){
            this.capacity = Double.MAX_VALUE;
        }else{
            this.capacity = (double) transitStop.getAttributes().getAttribute("capacity");
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
        if (vid == null){
            throw new RuntimeException("transitStop: " + transitStop.getId().toString());
        }
        if (dwellingVehicles.size() > capacity){
            throw new RuntimeException("too many dwelling vehicles!");
        }
        if (dwellingVehicles.contains(vid)){
            return;
        }
        if (dwellingVehicles.size() == capacity){
            if (!vehicles.contains(vid)) {
                vehicles.add(vid);
            }
        }else{
            dwellingVehicles.add(vid);
        }
    }



    public void removeVehicle(Id<Vehicle> vid){
        if(!dwellingVehicles.contains(vid) && !vehicles.contains(vid)){
            String dwell = new String();
            String vehs = new String();
            for (Id<Vehicle> dvehid : dwellingVehicles){
                if (dvehid == null){
                    dwell = dwell + ";null";
                }
                dwell = dwell + ";" + dvehid.toString();
            }
            for (Id<Vehicle> vehid : vehicles){
                if (vehid == null){
                    vehs = vehs + ";null";
                }
                vehs = vehs + ";" + vehid.toString();
            }
            throw new RuntimeException("vid: " + vid.toString() + ", transitStop: " + transitStop.getId().toString() + ", dwellV: " + dwell + ", v: " + vehs);
        }
        vehicles.remove(vid);
        if (dwellingVehicles.remove(vid) && isFull()){
            Id<Vehicle> vehicleId = vehicles.peek();
            dwellingVehicles.add(vehicleId);
            vehicles.remove(vehicleId);
        }
    }


    public Queue<Id<Vehicle>> getVehicles() {
        return vehicles;
    }


    public boolean isFull() {
        return vehicles.size() > 0;
    }

}
