package org.matsim.pt.connectionScan.conversion.transitNetworkConversion;

import edu.kit.ifv.mobitopp.publictransport.model.*;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class ConnectionConverter {

    private Map<Id<TransitLine>, TransitLine> transitLines;
    private Connections connections = new Connections();
    private MappingHandler idAndMappingHandler;

    //Journey attributes
    private Time day;
    //TODO
    private final int CAPACITY = 2000000000;

    ConnectionConverter(Map<Id<TransitLine>, TransitLine> transitLines,
                        MappingHandler idAndMappingHandler, Time day) {

        this.transitLines = transitLines;
        this.idAndMappingHandler = idAndMappingHandler;
        this.day = day;
    }

    void convert() {

        for (TransitLine transitLine : transitLines.values()) {
            for (TransitRoute transitRoute : transitLine.getRoutes().values()) {
                Connections newConnections = convertConnections(transitRoute, transitLine.getId());
                this.connections.addAll(newConnections);
            }
        }
    }

    private Connections convertConnections(TransitRoute transitRoute, Id<TransitLine> lineId) {

        List<StopWithMatsimOffsets> stops = new ArrayList<>();
        for (TransitRouteStop transitRouteStop : transitRoute.getStops()) {

            Stop currentStop = idAndMappingHandler.getMatsimId2Stop().get(transitRouteStop.getStopFacility().getId());
            stops.add(StopWithMatsimOffsets.from(currentStop, transitRouteStop.getArrivalOffset(),
                    transitRouteStop.getDepartureOffset()));
        }
        Connections newConnections = new Connections();

        for (Departure departure : transitRoute.getDepartures().values()) {

            String transportMode = transitRoute.getTransportMode();
            Journey journey = new DefaultModifiableJourney(
                    idAndMappingHandler.createNewJourneyId(), this.day, new TransportSystem(transportMode), this.CAPACITY);

            for (int i = 1; i < transitRoute.getStops().size(); i++) {
                Connection connection = convertConnection(
                        stops.get(i-1),
                        stops.get(i),
                        convertDeparture(departure),
                        journey);
                newConnections.add(connection);
                idAndMappingHandler.addConnectionId2LineAndRouteId(connection.id(), new Id[] {lineId, transitRoute.getId()});
            }
        }

        return newConnections;
    }

    private Connection convertConnection(StopWithMatsimOffsets start, StopWithMatsimOffsets end,
                                         Time departure, Journey journey) {
        int id = idAndMappingHandler.createNewConectionId();
        Stop startStop = start.getStop();
        Stop endStop = end.getStop();
        Time connectionDeparture = departure.add(RelativeTime.of((long)start.getDepartureOffset(), ChronoUnit.SECONDS));
        Time connectionArrival = departure.add(RelativeTime.of((long)end.getArrivalOffset(), ChronoUnit.SECONDS));
        RoutePoints route = RoutePoints.from(startStop, endStop);
        return Connection.from(id, startStop, endStop, connectionDeparture, connectionArrival, journey, route);
    }

    private Time convertDeparture(Departure departure) {
        return day.add(RelativeTime.of((long)departure.getDepartureTime(), ChronoUnit.SECONDS));
    }

    public Connections getConnections() {
        return connections;
    }
}
