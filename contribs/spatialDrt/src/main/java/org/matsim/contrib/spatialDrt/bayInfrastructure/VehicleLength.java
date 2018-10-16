package org.matsim.contrib.spatialDrt.bayInfrastructure;

import com.google.inject.Inject;


import org.matsim.api.core.v01.Id;


import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.interfaces.MobsimVehicle;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

public class VehicleLength {
    static Map<Id<Vehicle>, MobsimVehicle> lengthByVehicle;

    @Inject
    public VehicleLength(QSim qSim){
        lengthByVehicle = qSim.getVehicles();
    }


    public static double getLength(Id<Vehicle> vid){
        return lengthByVehicle.get(vid).getVehicle().getType().getLength();
    }
}
