package org.matsim.pt.connectionScan;

import edu.kit.ifv.mobitopp.publictransport.connectionscan.PublicTransportRoute;
import edu.kit.ifv.mobitopp.publictransport.model.RelativeTime;
import edu.kit.ifv.mobitopp.publictransport.model.Stop;
import edu.kit.ifv.mobitopp.publictransport.model.Time;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;
import org.matsim.pt.connectionScan.conversion.legConversion.LegListConverter;
import org.matsim.pt.connectionScan.conversion.transitNetworkConversion.NetworkConverter;
import org.matsim.pt.connectionScan.utils.TransitNetworkUtils;
import org.matsim.pt.router.AbstractTransitRouter;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author gthunig on 17.10.2017.
 */
public class ConnectionScan extends AbstractTransitRouter implements TransitRouter {
    private static final Logger log = Logger.getLogger(ConnectionScan.class);

    private edu.kit.ifv.mobitopp.publictransport.connectionscan.ConnectionScan connectionScan;
    private Map<Id<TransitStopFacility>, Stop> transitStopFacilityId2Stop;
    private Map<Integer, TransitStopFacility> stopId2TransitStopFacility;

    public ConnectionScan(TransitRouterConfig config, TransitTravelDisutility travelDisutility,
                          TransitSchedule transitSchedule) {
        super(config, travelDisutility);
        init(transitSchedule);
    }

    private void init(TransitSchedule transitSchedule) {
        NetworkConverter networkConverter = new NetworkConverter(transitSchedule);
        this.connectionScan = new edu.kit.ifv.mobitopp.publictransport.connectionscan.ConnectionScan(networkConverter.convert());
        this.transitStopFacilityId2Stop = networkConverter.getTransitStopFacilityId2StopMap();
        this.stopId2TransitStopFacility = networkConverter.getStopId2TransitStopFacilityMap();
    }


    @Override
    public List<Leg> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime, Person person) {

        Stop from = findNextStop(fromFacility);
        Stop to = findNextStop(toFacility);
        Time departure = convertDeparture(departureTime);

        PublicTransportRoute route = calcRouteFrom(from, to, departure);

        return LegListConverter.fromPublicTransportRoute(route);
    }

    private Stop findNextStop(Facility<?> facility) {
        return TransitNetworkUtils.getNearestStop(new ArrayList(transitStopFacilityId2Stop.values()), facility, 1000);
    }

    private Time convertDeparture(double departure) {
        Time day = new Time(LocalDateTime.of(2017, 3, 14, 0, 0));
        return day.add(RelativeTime.of((long)departure, ChronoUnit.SECONDS));
    }

    private PublicTransportRoute calcRouteFrom(Stop from, Stop to, Time departure) {
        Optional<PublicTransportRoute> potentialRoute = connectionScan.findRoute(from, to, departure);
        return potentialRoute.orElse(null);
    }
}
