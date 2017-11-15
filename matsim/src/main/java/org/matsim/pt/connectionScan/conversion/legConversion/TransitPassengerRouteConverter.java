package org.matsim.pt.connectionScan.conversion.legConversion;

import edu.kit.ifv.mobitopp.publictransport.connectionscan.PublicTransportRoute;
import edu.kit.ifv.mobitopp.publictransport.model.Connection;
import edu.kit.ifv.mobitopp.publictransport.model.RelativeTime;
import edu.kit.ifv.mobitopp.publictransport.model.Time;
import edu.kit.ifv.mobitopp.publictransport.model.TransportSystem;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.pt.connectionScan.conversion.transitNetworkConversion.MappingHandler;
import org.matsim.pt.router.RouteSegment;
import org.matsim.pt.router.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TransitPassengerRouteConverter {

    private MappingHandler mappingHandler;

    public TransitPassengerRouteConverter(MappingHandler mappingHandler) {
        this.mappingHandler = mappingHandler;
    }

    public List<Leg> fromPublicTransportRoute(PublicTransportRoute route) {
//        List<Leg> legs = new ArrayList<>();
//
//        Map<TransportSystem, List<Connection>> transportSystem2Connections =
//                sortConnectionsByLegMode(route.connections());
//
//        PopulationFactory factory = PopulationUtils.getFactory();
//        for (Map.Entry<TransportSystem, List<Connection>> currentTransportSystem2Connections : transportSystem2Connections.entrySet()) {
//
//            legs.add(createLeg(currentTransportSystem2Connections, factory));
//        }
//
//        return convertPassengerRouteToLegList(departureTime, transitPassengerRoute, fromFacility.getCoord(), toFacility.getCoord(), person);;
        return null;
    }

    public TransitPassengerRoute createTransitPassengerRoute(double departureTime, List<Connection> connections) {

        double cost = 0;

        List<RouteSegment> routeSegments = new ArrayList<>();



        for (int i = 0; i < connections.size(); i++) {
            Connection connection = connections.get(i);
            TransitStopFacility fromFacility = mappingHandler.getStopId2TransitStopFacility().get(connection.start().id());
            TransitStopFacility toFacility = mappingHandler.getStopId2TransitStopFacility().get(connection.end().id());
            double travelTime = connection.duration().seconds();

            //TODO extract for performance?
            if (i == 0) {
                double departureOffset = timeInSecondsFromMidnight(connection.departure()) - departureTime;
                travelTime += departureOffset;
            }
            Id[] lineAndRouteId = mappingHandler.getConnectionId2LineAndRouteId().get(connection.id());

            routeSegments.add(new RouteSegment(fromFacility, toFacility,
                    travelTime,
                    lineAndRouteId[0],
                    lineAndRouteId[1]));
            cost += travelTime;
        }

        if (routeSegments.size()==0) return null;
        else return new TransitPassengerRoute(cost, routeSegments);
    }

    private double getFirstDepartureOf(List<Connection> connections) {
        Time firstDeparture = connections.get(0).departure();

        for (Connection currentConnection : connections) {
            if (currentConnection.departure().isBefore(firstDeparture))
                firstDeparture = currentConnection.departure();
        }

        return timeInSecondsFromMidnight(firstDeparture);
    }

    private double getTravalTimeOf(List<Connection> connections) {
        RelativeTime duration = RelativeTime.of(0, ChronoUnit.MILLIS);

        for (Connection currentConnection : connections) {
            duration.plus(currentConnection.duration());
        }
        //TODO check if correct
        return duration.seconds();
    }

    private double timeInSecondsFromMidnight(Time time) {
        LocalDateTime localDateTime = time.time();
        double result = localDateTime.getHour()*60*60 + localDateTime.getMinute()*60 + localDateTime.getSecond();
        return result;
    }

    private Map<TransportSystem, List<Connection>> sortConnectionsByLegMode(List<Connection> connections) {
        Map<TransportSystem, List<Connection>> transportSystem2Connections = new HashMap<>();

        for (Connection currentConnection : connections) {
            TransportSystem system = currentConnection.journey().transportSystem();
            addTransportSystem2Connection(transportSystem2Connections, system, currentConnection);
        }

        return transportSystem2Connections;
    }

    private void addTransportSystem2Connection(
                            Map<TransportSystem, List<Connection>> transportSystem2Connections,
                            TransportSystem system, Connection connection) {

        if (!transportSystem2Connections.keySet().contains(system))
            transportSystem2Connections.put(system, new ArrayList<>());

        transportSystem2Connections.get(system).add(connection);
    }

}
