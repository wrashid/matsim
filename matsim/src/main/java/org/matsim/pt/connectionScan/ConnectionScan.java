package org.matsim.pt.connectionScan;

import edu.kit.ifv.mobitopp.publictransport.connectionscan.PublicTransportRoute;
import edu.kit.ifv.mobitopp.publictransport.model.RelativeTime;
import edu.kit.ifv.mobitopp.publictransport.model.Stop;
import edu.kit.ifv.mobitopp.publictransport.model.Time;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;
import org.matsim.pt.connectionScan.conversion.legConversion.TransitPassengerRouteConverter;
import org.matsim.pt.connectionScan.conversion.transitNetworkConversion.MappingHandler;
import org.matsim.pt.connectionScan.conversion.transitNetworkConversion.NetworkConverter;
import org.matsim.pt.connectionScan.utils.TransitNetworkUtils;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author gthunig on 17.10.2017.
 */
public class ConnectionScan extends AbstractTransitRouter implements TransitRouter {
    private static final Logger log = Logger.getLogger(ConnectionScan.class);

    private edu.kit.ifv.mobitopp.publictransport.connectionscan.ConnectionScan connectionScan;
    private MappingHandler mappingHandler;
    private TransitPassengerRouteConverter transitPassengerRouteConverter;

    public ConnectionScan(TransitRouterConfig config, TransitSchedule transitSchedule) {
        super(config, transitSchedule);
        init(transitSchedule);
    }

    @Deprecated
    public ConnectionScan(TransitRouterConfig config, TransitTravelDisutility travelDisutility,
                          TransitSchedule transitSchedule) {
        super(config, travelDisutility);
        init(transitSchedule);
    }

    private void init(TransitSchedule transitSchedule) {
        NetworkConverter networkConverter = new NetworkConverter(transitSchedule);
        this.connectionScan = new edu.kit.ifv.mobitopp.publictransport.connectionscan.ConnectionScan(networkConverter.convert());
        this.mappingHandler = networkConverter.getMappingHandler();
        this.transitPassengerRouteConverter = new TransitPassengerRouteConverter(mappingHandler);
    }


    @Override
    public List<Leg> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime, Person person) {

        Stop from = findNextStop(fromFacility);
        Stop to = findNextStop(toFacility);
        Time departure = convertDeparture(departureTime);

        PublicTransportRoute route = calcRouteFrom(from, to, departure);

        TransitPassengerRoute transitPassengerRoute = transitPassengerRouteConverter.createTransitPassengerRoute(departureTime, route.connections());
        return convertPassengerRouteToLegList(departureTime, transitPassengerRoute, fromFacility.getCoord(), toFacility.getCoord(), person);
    }

    private Stop findNextStop(Facility<?> facility) {
        return TransitNetworkUtils.getNearestStop(new ArrayList(mappingHandler.getMatsimId2Stop().values()), facility, 1000);
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
