package org.matsim.pt.connectionScan.conversion.legConversion;

import edu.kit.ifv.mobitopp.publictransport.model.Connection;
import edu.kit.ifv.mobitopp.publictransport.model.TransportSystem;
import edu.kit.ifv.mobitopp.time.RelativeTime;
import edu.kit.ifv.mobitopp.time.Time;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.connectionScan.conversion.transitNetworkConversion.MappingHandler;
import org.matsim.pt.connectionScan.utils.TransitNetworkUtils;
import org.matsim.pt.router.RouteSegment;
import org.matsim.pt.router.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

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

    public TransitPassengerRoute createTransitPassengerRoute(double lastArrivalTime, List<Connection> connections) {

        double cost = 0;
//        double lastArrivalTime = timeInSecondsFromMidnight(departure);

        List<RouteSegment> routeSegments = new ArrayList<>();

//        RouteSegment storedRouteSegment = null;

        List<List<Connection>> legs = new ArrayList<>();

        //find cohesive connections (that are to be made to legs)

        boolean isFootLeg = connections.get(0).id() == -1;
        int nextLegStartElement = 0;
        for (int i = 0; i <= connections.size(); i++) {

            //join same Journey connection
            //first foot journey belongs to accessWalk
            //last foot journey belongs to egressWalk

            if (isFootLeg) while (i < connections.size() && connections.get(i).id() == -1) i++;
            else while (i < connections.size() && connections.get(i).id() != -1) i++;
            List<Connection> currentLeg = new ArrayList<>();
            for (int e = nextLegStartElement; e <= i-1; e++) {
                currentLeg.add(connections.get(e));
            }
            nextLegStartElement = i;
            legs.add(currentLeg);
            isFootLeg = !isFootLeg;
        }

        //TODO experimental: delete first and last footJourney
        if (legs.get(0).get(0).id() == -1) {
            lastArrivalTime = TransitNetworkUtils.convertTime(legs.get(0).get(legs.get(0).size()-1).arrival());
            legs.remove(0);
        }
        if (legs.get(legs.size()-1).get(0).id() == -1) {
            legs.remove(legs.size()-1);
        }

        //convert legs
        for (List<Connection> currentLeg : legs) {

            Connection startConnection = currentLeg.get(0);
            Connection endConnection = currentLeg.get(currentLeg.size()-1);
            TransitStopFacility fromFacility = mappingHandler.getStopId2TransitStopFacility().get(
                    startConnection.start().id());
            TransitStopFacility toFacility = mappingHandler.getStopId2TransitStopFacility().get(
                    endConnection.end().id());
            Time departure = startConnection.departure();
            Time arrival = endConnection.arrival();
            double travelTime = TransitNetworkUtils.convertTime(arrival.differenceTo(departure));

            double departureOffset = TransitNetworkUtils.convertTime(departure);
            //TODO make prettier
            if (lastArrivalTime != -1)
                departureOffset -= lastArrivalTime;
            travelTime += departureOffset;
            lastArrivalTime = TransitNetworkUtils.convertTime(arrival);

            Id[] lineAndRouteId = new Id[] {null, null};
            for (Connection connection : currentLeg) {

                if (connection.id() != -1)
                    lineAndRouteId = mappingHandler.getConnectionId2LineAndRouteId().get(connection.id());
            }

            routeSegments.add(new RouteSegment(fromFacility, toFacility,
                    travelTime, lineAndRouteId[0], lineAndRouteId[1]));
            cost += travelTime;
        }

//        for (int i = 0; i < connections.size(); i++) {
//
//            //join same Journey connection
//            //first foot journey belongs to first routeSegment
//            //last foot journey belongs to last routeSegment
//
//            Connection connection = connections.get(i);
//            TransitStopFacility fromFacility = mappingHandler.getStopId2TransitStopFacility().get(connection.start().id());
//            TransitStopFacility toFacility = mappingHandler.getStopId2TransitStopFacility().get(connection.end().id());
//            RelativeTime duration = connection.duration();
//            double travelTime = TransitNetworkUtils.convertTime(duration);
//
//            double departureOffset = TransitNetworkUtils.convertTime(connection.departure()) - lastArrivalTime;
//            travelTime += departureOffset;
//            lastArrivalTime = TransitNetworkUtils.convertTime(connection.arrival());
//
//
//
//
//            //TODO improve readability
//            if (connection.id() == -1) {
//                if (storedRouteSegment != null) {
//                    if (storedRouteSegment.getRouteTaken() == null) {
//                        // then join these two route segments
//
//                        storedRouteSegment = new RouteSegment(storedRouteSegment.getFromStop(), toFacility,
//                                travelTime + storedRouteSegment.getTravelTime(),
//                                null,null);
//                    } else {
//                        routeSegments.add(storedRouteSegment);
//                        storedRouteSegment = new RouteSegment(fromFacility, toFacility,
//                                travelTime, null, null);
//                    }
//                } else {
//                    storedRouteSegment = new RouteSegment(fromFacility, toFacility,
//                            travelTime, null, null);
//                }
//            } else {
//
//                Id[] lineAndRouteId = mappingHandler.getConnectionId2LineAndRouteId().get(connection.id());
//
//                if (storedRouteSegment != null) {
//                    if (storedRouteSegment.getRouteTaken() != null &&
//                            storedRouteSegment.getRouteTaken().equals(lineAndRouteId[1])) {
//                        // then join these two route segments
//
//                        storedRouteSegment = new RouteSegment(storedRouteSegment.getFromStop(), toFacility,
//                                travelTime + storedRouteSegment.getTravelTime(),
//                                lineAndRouteId[0],
//                                lineAndRouteId[1]);
//                    } else {
//                        routeSegments.add(storedRouteSegment);
//                        storedRouteSegment = new RouteSegment(fromFacility, toFacility,
//                                travelTime,
//                                lineAndRouteId[0],
//                                lineAndRouteId[1]);
//                    }
//                } else {
//                    storedRouteSegment = new RouteSegment(fromFacility, toFacility,
//                            travelTime,
//                            lineAndRouteId[0],
//                            lineAndRouteId[1]);
//                }
//            }
//
//            cost += travelTime;
//        }
//
//        if (storedRouteSegment != null)
//            routeSegments.add(storedRouteSegment);

        if (routeSegments.size()==0) return null;
        else return new TransitPassengerRoute(cost, routeSegments);
    }

    private double getFirstDepartureOf(List<Connection> connections) {
        Time firstDeparture = connections.get(0).departure();

        for (Connection currentConnection : connections) {
            if (currentConnection.departure().isBefore(firstDeparture))
                firstDeparture = currentConnection.departure();
        }

        return TransitNetworkUtils.convertTime(firstDeparture);
    }

    private double getTravalTimeOf(List<Connection> connections) {
        RelativeTime duration = RelativeTime.of(0, ChronoUnit.MILLIS);

        for (Connection currentConnection : connections) {
            duration.plus(currentConnection.duration());
        }
        //TODO check if correct
        return duration.seconds();
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
