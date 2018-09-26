package org.matsim.pt.router;

import com.google.inject.Singleton;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.DefaultStopPaths;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.PublicTransportRoute;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.StopPaths;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.TransitNetwork;
import edu.kit.ifv.mobitopp.publictransport.model.Connection;
import edu.kit.ifv.mobitopp.publictransport.model.FootJourney;
import edu.kit.ifv.mobitopp.publictransport.model.Stop;
import edu.kit.ifv.mobitopp.publictransport.model.StopPath;
import edu.kit.ifv.mobitopp.time.Time;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.connectionScan.conversion.legConversion.TransitPassengerRouteConverter;
import org.matsim.pt.connectionScan.conversion.transitNetworkConversion.MappingHandler;
import org.matsim.pt.connectionScan.model.Day;
import org.matsim.pt.connectionScan.model.StopAndInitialData;
import org.matsim.pt.connectionScan.utils.CalculationTimeMonitor;
import org.matsim.pt.connectionScan.utils.CoordinateUtils;
import org.matsim.pt.connectionScan.utils.TransitNetworkUtils;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.lang.System.nanoTime;

/**
 * @author gthunig on 17.10.2017.
 */

public class ConnectionScanRouter extends AbstractTransitRouter implements TransitRouter {
    private static final Logger log = Logger.getLogger(ConnectionScanRouter.class);

    private edu.kit.ifv.mobitopp.publictransport.connectionscan.ConnectionScan connectionScan;
    private MappingHandler mappingHandler;
    private TransitPassengerRouteConverter transitPassengerRouteConverter;
    private TransitRouterConfig trConfig;
    private CalculationTimeMonitor monitor;

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
        monitor = new CalculationTimeMonitor();
    }

    @Override
    public List<Leg> calcRoute(Facility<?> fromFacility, Facility<?> toFacility, double departureTime, Person person) {

        boolean monitorTime = false;
        if (monitorTime)
            monitor.routingStarted();

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
            double initialAccessTime = Math.ceil(getWalkTime(person, fromFacility.getCoord(), CoordinateUtils.convert2Coord(currentStop.getStop().coordinate())));
            currentStop.setInitialTime(initialAccessTime);
        }
        for (StopAndInitialData currentStop : to) {
            double initialEgressTime = Math.ceil(getWalkTime(person, CoordinateUtils.convert2Coord(currentStop.getStop().coordinate()), toFacility.getCoord()));
            currentStop.setInitialTime(initialEgressTime);
        }

//        double ptDeparture = normalize(departureTime + initialAccessTime);
//        Time departure = convertDeparture(ptDeparture);

        Time departure = convertDeparture(departureTime);

        StopPaths fromStops = convertStops(from);
        StopPaths toStops = convertStops(to);

        if (monitorTime)
        monitor.csaStarted();
        PublicTransportRoute route = calcRouteFrom(fromStops, toStops, departure);
        if (monitorTime)
        monitor.csaFinished();

        if (route == null) {
            return this.createDirectWalkLegList(null, fromFacility.getCoord(), toFacility.getCoord());
        }
        if (route.connections().size() == 1 && route.connections().get(0).journey() instanceof FootJourney) {
            // then the two stops must be neighbours and its allways better to take the direct walk
            return this.createDirectWalkLegList(person, fromFacility.getCoord(), toFacility.getCoord());
        }

        double accessTime = -1;
        for (StopAndInitialData currentStop : from) {
            if (currentStop.getStop().equals(route.start())) {
                accessTime = currentStop.getInitialTime();
            }
        }
        double egressTime = -1;
        for (StopAndInitialData currentStop : to) {
            if (currentStop.getStop().equals(route.end())) {
                egressTime = currentStop.getInitialTime();
            }
        }

        TransitPassengerRoute transitPassengerRoute = transitPassengerRouteConverter.createTransitPassengerRoute(
                accessTime+departureTime, route.connections());

        TransitStopFacility startStop = mappingHandler.getStopId2TransitStopFacility().get(route.connections().get(0).start().id());
        TransitStopFacility endStop = mappingHandler.getStopId2TransitStopFacility().get(route.connections().get(route.connections().size()-1).end().id());
        Leg accessLeg = createAccessLeg(--accessTime, fromFacility.getCoord(), startStop);
        Leg egressLeg = createEgressLeg(--egressTime, endStop, toFacility.getCoord());
        List<Leg> legList = convertPublicTransportRouteToLegList(transitPassengerRoute);
        legList.add(0, accessLeg);
        legList.add(egressLeg);

//        TransitPassengerRoute transitPassengerRoute = transitPassengerRouteConverter.createTransitPassengerRoute(ptDeparture, route.connections(),
//                from.getInitialTime(), to.getInitialTime());

//        double initialAccessTime = route.connections().get(0).
//        double initialEgressTime =
//        double ptDeparture = normalize(departureTime + initialAccessTime);
//
//      slightly wrong and wasted performance
//        TransitPassengerRoute transitPassengerRoute = transitPassengerRouteConverter.createTransitPassengerRoute(
//                accessArrivalTime, route.connections(),
//                0,0);
//
//        List<Leg> legList = convertPassengerRouteToLegList(departureTime, transitPassengerRoute, fromFacility.getCoord(), toFacility.getCoord(), person);


        double pathTime = 0;
        for (Leg leg : legList)
            pathTime += leg.getTravelTime();
        double directWalkTime = getWalkTime(person, fromFacility.getCoord(), toFacility.getCoord());

        if (monitorTime) {
            monitor.routingFinished();
            monitor.printOutput();
        }

        if (directWalkTime < pathTime) {
            return this.createDirectWalkLegList(person, fromFacility.getCoord(), toFacility.getCoord());
        } else {
            return legList;
        }
    }

    private Leg createAccessLeg(double walkTime, Coord fromCoord, TransitStopFacility toStop) {

        Leg leg = createWalkLeg(walkTime);
        Route walkRoute = RouteUtils.createGenericRouteImpl(null, toStop.getLinkId());
        walkRoute.setTravelTime(leg.getTravelTime());
        walkRoute.setDistance(trConfig.getBeelineDistanceFactor() * NetworkUtils.getEuclideanDistance(fromCoord, toStop.getCoord()));
        leg.setRoute(walkRoute);
        return leg;
    }

    private Leg createEgressLeg(double walkTime, TransitStopFacility endStop, Coord toCoord) {

        Leg leg = createWalkLeg(walkTime);
        Route walkRoute = RouteUtils.createGenericRouteImpl(endStop.getLinkId(), null);
        walkRoute.setTravelTime(leg.getTravelTime());
        walkRoute.setDistance(trConfig.getBeelineDistanceFactor() * NetworkUtils.getEuclideanDistance(endStop.getCoord(), toCoord));
        leg.setRoute(walkRoute);
        return leg;
    }

    private Leg createWalkLeg(double walkTime) {

        Leg leg = PopulationUtils.createLeg(TransportMode.transit_walk);
        leg.setTravelTime(walkTime);
        return leg;
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
        Time day = Day.getDay(); //TODO
        return day.plus(TransitNetworkUtils.convertTime(departure));
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

    private List<Leg> convertPublicTransportRouteToLegList(TransitPassengerRoute route) {

        List<Leg> legList = new ArrayList<>();

        // route segments are in pt-walk-pt sequence
        for (RouteSegment routeSegement : route.getRoute()) {
            if (routeSegement.getRouteTaken() == null) {// transfer
                if (!routeSegement.fromStop.equals(routeSegement.toStop)) { // same to/from stop => no transfer. Amit Feb'18
                    legList.add(createTransferTransitWalkLeg(routeSegement));
                }
            } else {
                // pt leg
                legList.add(createTransitLeg(routeSegement));
            }
        }

        return legList;
    }

    private Leg createTransferTransitWalkLeg(RouteSegment routeSegement) {

        Leg leg = createWalkLeg(routeSegement.travelTime);
        Route walkRoute = RouteUtils.createGenericRouteImpl(routeSegement.getFromStop().getLinkId(), routeSegement.getToStop().getLinkId());
        walkRoute.setTravelTime(routeSegement.travelTime);
        walkRoute.setDistance(trConfig.getBeelineDistanceFactor() * NetworkUtils.getEuclideanDistance(routeSegement.fromStop.getCoord(), routeSegement.toStop.getCoord()));
        leg.setRoute(walkRoute);

        return leg;
    }

    private Leg createTransitLeg(RouteSegment routeSegment) {
        Leg leg = PopulationUtils.createLeg(TransportMode.pt);

        TransitStopFacility accessStop = routeSegment.getFromStop();
        TransitStopFacility egressStop = routeSegment.getToStop();

        ExperimentalTransitRoute ptRoute = new ExperimentalTransitRoute(accessStop, egressStop, routeSegment.getLineTaken(), routeSegment.getRouteTaken());
        ptRoute.setTravelTime(routeSegment.travelTime);
        leg.setRoute(ptRoute);

        leg.setTravelTime(routeSegment.getTravelTime());
        return leg;
    }
}
