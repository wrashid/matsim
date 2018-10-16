package org.matsim.contrib.spatialDrt.vehicle;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;


public class DynVehicleType extends VehicleTypeImpl {
    public static String DYNTYPE = "dynType";
    private double accessTime = 1.5;
    private double egressTime = 1.5;
    private double batteryCapacity;


    public DynVehicleType(){
        super(Id.create(DYNTYPE, VehicleType.class));
    }

    @Override
    public double getAccessTime() {
        return this.accessTime;
    }

    @Override
    public double getEgressTime() {
        return this.egressTime;
    }

    public double getBatteryCapacity() {
        return batteryCapacity;
    }

    public void setBatteryCapacity(double batteryCapacity) {
        this.batteryCapacity = batteryCapacity;
    }
}
