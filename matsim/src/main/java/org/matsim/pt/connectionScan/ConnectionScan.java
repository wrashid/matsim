package org.matsim.pt.connectionScan;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;
import org.matsim.pt.router.AbstractTransitRouter;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitTravelDisutility;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.RouteSearch;

import java.util.List;

/**
 * @author gthunig on 17.10.2017.
 */
public class ConnectionScan extends AbstractTransitRouter implements TransitRouter {

    public ConnectionScan(TransitRouterConfig config, TransitTravelDisutility travelDisutility) {
        super(config, travelDisutility);
        RouteSearch routeSearch = null;
    }

    @Override
    public List<Leg> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime, Person person) {
        return null;
    }
}
