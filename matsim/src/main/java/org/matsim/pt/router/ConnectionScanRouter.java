package org.matsim.pt.router;

import edu.kit.ifv.mobitopp.publictransport.connectionscan.PublicTransportRoute;
import edu.kit.ifv.mobitopp.publictransport.model.FootJourney;
import edu.kit.ifv.mobitopp.publictransport.model.RelativeTime;
import edu.kit.ifv.mobitopp.publictransport.model.Stop;
import edu.kit.ifv.mobitopp.publictransport.model.Time;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.facilities.Facility;
import org.matsim.pt.connectionScan.conversion.legConversion.TransitPassengerRouteConverter;
import org.matsim.pt.connectionScan.conversion.transitNetworkConversion.MappingHandler;
import org.matsim.pt.connectionScan.conversion.transitNetworkConversion.NetworkConverter;
import org.matsim.pt.connectionScan.model.StopAndInitialData;
import org.matsim.pt.connectionScan.utils.CoordinateUtils;
import org.matsim.pt.connectionScan.utils.TransitNetworkUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author gthunig on 17.10.2017.
 */
public class ConnectionScanRouter extends AbstractTransitRouter implements TransitRouter {
    private static final Logger log = Logger.getLogger(ConnectionScanRouter.class);

    private edu.kit.ifv.mobitopp.publictransport.connectionscan.ConnectionScan connectionScan;
    private MappingHandler mappingHandler;
    private TransitPassengerRouteConverter transitPassengerRouteConverter;

    public ConnectionScanRouter(TransitRouterConfig config, TransitTravelDisutility travelDisutility,
                                TransitSchedule transitSchedule) {
        super(config, travelDisutility);
        init(config, transitSchedule);
    }

    private void init(TransitRouterConfig config, TransitSchedule transitSchedule) {
        NetworkConverter networkConverter = new NetworkConverter(transitSchedule, config, getTravelDisutility());
        this.connectionScan = new edu.kit.ifv.mobitopp.publictransport.connectionscan.ConnectionScan(networkConverter.convert());
        this.mappingHandler = networkConverter.getMappingHandler();
        this.transitPassengerRouteConverter = new TransitPassengerRouteConverter(mappingHandler);
    }


    @Override
    public List<Leg> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime, Person person) {

        StopAndInitialData from = findNextStop(fromFacility);
        StopAndInitialData to = findNextStop(toFacility);

        if (from == null || to == null) {
            return this.createDirectWalkLegList(null, fromFacility.getCoord(), toFacility.getCoord());
        }
        double initialAccessTime = getWalkTime(person, fromFacility.getCoord(), CoordinateUtils.convert2Coord(from.getStop().coordinate()));
        from.setInitialTime(initialAccessTime);
        double initialEgressTime = getWalkTime(person, CoordinateUtils.convert2Coord(to.getStop().coordinate()), toFacility.getCoord());
        to.setInitialTime(initialEgressTime);

        double ptDeparture = normalize(departureTime + initialAccessTime);
        Time departure = convertDeparture(ptDeparture);

        PublicTransportRoute route = calcRouteFrom(from.getStop(), to.getStop(), departure);

        if (route == null) {
            return this.createDirectWalkLegList(null, fromFacility.getCoord(), toFacility.getCoord());
        }
        if (route.connections().size() == 1 && route.connections().get(0).journey() instanceof FootJourney) {
            // then the two stops must be neighbours and its allways better to take the direct walk
            return this.createDirectWalkLegList(person, fromFacility.getCoord(), toFacility.getCoord());
        }
        TransitPassengerRoute transitPassengerRoute = transitPassengerRouteConverter.createTransitPassengerRoute(ptDeparture, route.connections(),
                from.getInitialTime(), to.getInitialTime());

        double pathTime = transitPassengerRoute.getTravelCost();
        double directWalkTime = getWalkTime(person, fromFacility.getCoord(), toFacility.getCoord());

        if (directWalkTime < pathTime) {
            return this.createDirectWalkLegList(person, fromFacility.getCoord(), toFacility.getCoord());
        }

        return convertPassengerRouteToLegList(departureTime, transitPassengerRoute, fromFacility.getCoord(), toFacility.getCoord(), person);
    }

    private StopAndInitialData findNextStop(Facility<?> facility) {
        return TransitNetworkUtils.getNearestStop(new ArrayList(mappingHandler.getMatsimId2Stop().values()), facility, 1000);
    }

    private Time convertDeparture(double departure) {
        Time day = new Time(LocalDateTime.of(2017, 3, 14, 0, 0));
        return day.add(RelativeTime.of((long) departure, ChronoUnit.SECONDS));
    }

    private double normalize(double relativeTime) {
        while (relativeTime >= 86400) {
            relativeTime -= 86400;
        }
        return relativeTime;
    }

    private PublicTransportRoute calcRouteFrom(Stop from, Stop to, Time departure) {
        Optional<PublicTransportRoute> potentialRoute = connectionScan.findRoute(from, to, departure);
        return potentialRoute.orElse(null);
    }
}
