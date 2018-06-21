package org.matsim.pt.router;

import com.google.inject.Singleton;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.DefaultStopPaths;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.PublicTransportRoute;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.StopPaths;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.TransitNetwork;
import edu.kit.ifv.mobitopp.publictransport.model.*;
import org.apache.log4j.Logger;
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

@Singleton
public class ConnectionScanRouter extends AbstractTransitRouter implements TransitRouter {
    private static final Logger log = Logger.getLogger(ConnectionScanRouter.class);

    private edu.kit.ifv.mobitopp.publictransport.connectionscan.ConnectionScan connectionScan;
    private MappingHandler mappingHandler;
    private TransitPassengerRouteConverter transitPassengerRouteConverter;
    private TransitRouterConfig trConfig;

    public ConnectionScanRouter(TransitRouterConfig config, TransitTravelDisutility travelDisutility,
                                TransitNetwork transitNetwork, MappingHandler mappingHandler) {
        super(config, travelDisutility);
        init(config, transitNetwork, mappingHandler);
    }

    private void init(TransitRouterConfig config, TransitNetwork transitNetwork, MappingHandler mappingHandler) {
        trConfig = config;
        this.connectionScan = new edu.kit.ifv.mobitopp.publictransport.connectionscan.ConnectionScan(transitNetwork);
        this.transitPassengerRouteConverter = new TransitPassengerRouteConverter(mappingHandler);
        this.mappingHandler = mappingHandler;
    }


    @Override
    public List<Leg> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime, Person person) {

        departureTime = normalize(departureTime);
//        StopAndInitialData from = findNextStop(fromFacility);
//        StopAndInitialData to = findNextStop(toFacility);
        List<StopAndInitialData> from = findNextStops(fromFacility);
        List<StopAndInitialData> to = findNextStops(toFacility);

//        if (from == null || to == null) {
        if (from.size() == 0 || to.size() == 0) {
            return this.createDirectWalkLegList(null, fromFacility.getCoord(), toFacility.getCoord());
        }

        for (StopAndInitialData currentStop : from) {
            double initialAccessTime = getWalkTime(person, fromFacility.getCoord(), CoordinateUtils.convert2Coord(currentStop.getStop().coordinate()));
            currentStop.setInitialTime(initialAccessTime);
        }
        for (StopAndInitialData currentStop : to) {
            double initialEgressTime = getWalkTime(person, CoordinateUtils.convert2Coord(currentStop.getStop().coordinate()), toFacility.getCoord());
            currentStop.setInitialTime(initialEgressTime);
        }

//        double ptDeparture = normalize(departureTime + initialAccessTime);
//        Time departure = convertDeparture(ptDeparture);

        Time departure = convertDeparture(departureTime);

        PublicTransportRoute route = calcRouteFrom(convertStops(from), convertStops(to), departure);

        if (route == null) {
            return this.createDirectWalkLegList(null, fromFacility.getCoord(), toFacility.getCoord());
        }
        if (route.connections().size() == 1 && route.connections().get(0).journey() instanceof FootJourney) {
            // then the two stops must be neighbours and its allways better to take the direct walk
            return this.createDirectWalkLegList(person, fromFacility.getCoord(), toFacility.getCoord());
        }
//        TransitPassengerRoute transitPassengerRoute = transitPassengerRouteConverter.createTransitPassengerRoute(ptDeparture, route.connections(),
//                from.getInitialTime(), to.getInitialTime());

//        double initialAccessTime = route.connections().get(0).
//        double initialEgressTime =
//        double ptDeparture = normalize(departureTime + initialAccessTime);

        double accessArrivalTime = -1;
        for (StopAndInitialData currentStop : from) {
            if (currentStop.getStop().equals(route.start())) {
                accessArrivalTime = departureTime + currentStop.getInitialTime();
            }
        }

        TransitPassengerRoute transitPassengerRoute = transitPassengerRouteConverter.createTransitPassengerRoute(
                accessArrivalTime, route.connections(),
                0,0);

        List<Leg> legList = convertPassengerRouteToLegList(departureTime, transitPassengerRoute, fromFacility.getCoord(), toFacility.getCoord(), person);

        double pathTime = 0;
        for (Leg leg : legList)
            pathTime += leg.getTravelTime();
        double directWalkTime = getWalkTime(person, fromFacility.getCoord(), toFacility.getCoord());

        if (directWalkTime < pathTime) {
            return this.createDirectWalkLegList(person, fromFacility.getCoord(), toFacility.getCoord());
        } else {
            return legList;
        }
    }

    @Deprecated //extension radius is not working correctly, but do we even need this method?
    private StopAndInitialData findNextStop(Facility<?> facility) {
        return TransitNetworkUtils.getNearestStop(new ArrayList(mappingHandler.getMatsimId2Stop().values()), facility, trConfig.getSearchRadius());
    }

    private List<StopAndInitialData> findNextStops(Facility<?> facility) {

        List<StopAndInitialData> nearestStops = new ArrayList<>();
        int i = 0;
        while(nearestStops.size() == 0) {
            nearestStops = TransitNetworkUtils.getNearestStops(
                    new ArrayList(mappingHandler.getMatsimId2Stop().values()), facility,
                    trConfig.getSearchRadius() + (trConfig.getExtensionRadius() * i++));
        }
        return nearestStops;
    }

    private Time convertDeparture(double departure) {
        Time day = new Time(LocalDateTime.of(2017, 3, 14, 0, 0)); //TODO make constant
        return day.add(TransitNetworkUtils.convertTime(departure));
    }

    private double normalize(double relativeTime) {
        while (relativeTime >= 86400) {
            relativeTime -= 86400;
        }
        return relativeTime;
    }

    private StopPaths convertStops(List<StopAndInitialData> stops) {
        List<StopPath> convertedStops = new ArrayList<>();
        for (StopAndInitialData currentStop : stops) {
            convertedStops.add(new StopPath(currentStop.getStop(), TransitNetworkUtils.convertTime(currentStop.getInitialTime())));
        }
        return DefaultStopPaths.from(convertedStops);
    }

    private PublicTransportRoute calcRouteFrom(Stop from, Stop to, Time departure) {
        Optional<PublicTransportRoute> potentialRoute = connectionScan.findRoute(from, to, departure);
        return potentialRoute.orElse(null);
    }

    private PublicTransportRoute calcRouteFrom(StopPaths from, StopPaths to, Time departure) {
        Optional<PublicTransportRoute> potentialRoute = connectionScan.findRoute(from, to, departure);
        return potentialRoute.orElse(null);
    }
}
