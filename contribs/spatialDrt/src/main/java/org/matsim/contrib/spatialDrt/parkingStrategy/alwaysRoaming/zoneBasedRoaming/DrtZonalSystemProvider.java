package org.matsim.contrib.spatialDrt.parkingStrategy.alwaysRoaming.zoneBasedRoaming;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import com.vividsolutions.jts.geom.Geometry;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.spatialDrt.parkingStrategy.alwaysRoaming.zoneBasedRoaming.DrtZonalSystem;

import java.util.Map;

public class DrtZonalSystemProvider implements Provider<DrtZonalSystem> {
    @Inject
    @Named("dvrp_routing")
    Network network;
    private double cellsize;

    public DrtZonalSystemProvider(double cellsize) {
        this.cellsize = cellsize;
    }

    public DrtZonalSystemProvider() {
    }

    public DrtZonalSystem get() {
        Map<String, Geometry> zones = DrtGridUtils.createGridFromNetwork(network, cellsize);
        DrtZonalSystem drtZonalSystem = new DrtZonalSystem(network, zones);
        return drtZonalSystem;
    }

}
