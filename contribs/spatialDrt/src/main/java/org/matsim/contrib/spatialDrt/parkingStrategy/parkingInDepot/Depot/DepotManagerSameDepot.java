package org.matsim.contrib.spatialDrt.parkingStrategy.parkingInDepot.Depot;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.core.config.Config;

import java.util.Map;

public class DepotManagerSameDepot extends DepotManagerDifferentDepots {

    @Inject
    public DepotManagerSameDepot(Config config, @Named(DvrpRoutingNetworkProvider.DVRP_ROUTING) Network network) {
        super(config, network);
    }

    @Override
    public Map<Id<Depot>, Depot> getDepots(double capacity) {
        return getDepots(Depot.DepotType.DEPOT);
    }
}
