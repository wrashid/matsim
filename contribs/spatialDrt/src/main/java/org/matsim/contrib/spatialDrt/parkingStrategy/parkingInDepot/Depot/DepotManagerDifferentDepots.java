package org.matsim.contrib.spatialDrt.parkingStrategy.parkingInDepot.Depot;


import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.contrib.spatialDrt.run.AtodConfigGroup;
import org.matsim.core.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DepotManagerDifferentDepots implements DepotManager{

    Map<Id<Depot>, Depot> depots = new HashMap<>();
    Map<Id<Vehicle>,Id<Depot>> vehicleLists = new HashMap<>(); //  vehicle id, depot id

    @Inject
    public DepotManagerDifferentDepots(Config config, @Named(DvrpRoutingNetworkProvider.DVRP_ROUTING) Network network){
        AtodConfigGroup drtConfig = AtodConfigGroup.get(config);
        new DepotReader(this,network).parse(drtConfig.getDepotFileUrl(config.getContext()));
    }

    public void addDepot(Depot depot) {
        depots.put(depot.getId(), depot);
    }

    public Map<Id<Depot>, Depot> getDepots() {
        return depots;
    }
    public Map<Id<Depot>, Depot> getDepots(Depot.DepotType depotType) {
        return depots.entrySet().stream().filter(depot -> depot.getValue().getDepotType() == depotType).collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
    }

    public boolean isVehicleInDepot(Vehicle vehicle) {
        return vehicleLists.containsKey(vehicle.getId());
    }

    public void registerVehicle(Id<Vehicle> vid, Id<Depot> did) {
        vehicleLists.put(vid,did);
    }

    public Depot getDepotOfVehicle(Vehicle vehicle) {
        if (vehicleLists.containsKey(vehicle.getId())){
            return depots.get(vehicleLists.get(vehicle.getId()));
        }
        return null;
    }

    public void vehicleLeavingDepot(Vehicle vehicle) {
        Depot currentDepot = getDepotOfVehicle(vehicle);
        if (currentDepot == null){
            return;
        }
        currentDepot.removeVehicle(vehicle.getId());
        vehicleLists.remove(vehicle.getId());
    }

    public Map<Id<Depot>, Depot> getDepots(double capacity) {
        if (capacity < 10){
            return getDepots(Depot.DepotType.HDB);
        }else{
            return getDepots(Depot.DepotType.DEPOT);
        }
    }
}
