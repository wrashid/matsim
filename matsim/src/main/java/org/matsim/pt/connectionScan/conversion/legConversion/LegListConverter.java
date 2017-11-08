package org.matsim.pt.connectionScan.conversion.legConversion;

import edu.kit.ifv.mobitopp.publictransport.connectionscan.PublicTransportRoute;
import edu.kit.ifv.mobitopp.publictransport.model.Connection;
import edu.kit.ifv.mobitopp.publictransport.model.RelativeTime;
import edu.kit.ifv.mobitopp.publictransport.model.Time;
import edu.kit.ifv.mobitopp.publictransport.model.TransportSystem;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.PopulationUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LegListConverter {

    public static List<Leg> fromPublicTransportRoute(PublicTransportRoute route) {
        List<Leg> legs = new ArrayList<>();

        Map<TransportSystem, List<Connection>> transportSystem2Connections =
                sortConnectionsByLegMode(route.connections());

        PopulationFactory factory = PopulationUtils.getFactory();
        for (Map.Entry<TransportSystem, List<Connection>> currentTransportSystem2Connections : transportSystem2Connections.entrySet()) {

            legs.add(createLeg(currentTransportSystem2Connections, factory));
        }

        return legs;
    }

    private static Leg createLeg(Map.Entry<TransportSystem, List<Connection>> entry, PopulationFactory factory) {
        Leg currentLeg = factory.createLeg(entry.getKey().code());
        currentLeg.setDepartureTime(getFirstDepartureOf(entry.getValue()));
        currentLeg.setTravelTime(getTravalTimeOf(entry.getValue()));

        return currentLeg;
    }

    private static double getFirstDepartureOf(List<Connection> connections) {
        Time firstDeparture = connections.get(0).departure();

        for (Connection currentConnection : connections) {
            if (currentConnection.departure().isBefore(firstDeparture))
                firstDeparture = currentConnection.departure();
        }

        return timeInSecondsFromZero(firstDeparture);
    }

    private static double getTravalTimeOf(List<Connection> connections) {
        RelativeTime duration = RelativeTime.of(0, ChronoUnit.MILLIS);

        for (Connection currentConnection : connections) {
            duration.plus(currentConnection.duration());
        }
        //TODO
        return duration.seconds();
    }

    private static double timeInSecondsFromZero(Time time) {
        LocalDateTime localDateTime = time.time();
        double result = localDateTime.getHour()*60*60 + localDateTime.getMinute()*60 + localDateTime.getSecond();
        return result;
    }

    private static Map<TransportSystem, List<Connection>> sortConnectionsByLegMode(List<Connection> connections) {
        Map<TransportSystem, List<Connection>> transportSystem2Connections = new HashMap<>();

        for (Connection currentConnection : connections) {
            TransportSystem system = currentConnection.journey().transportSystem();
            addTransportSystem2Connection(transportSystem2Connections, system, currentConnection);
        }

        return transportSystem2Connections;
    }

    private static void addTransportSystem2Connection(
                            Map<TransportSystem, List<Connection>> transportSystem2Connections,
                            TransportSystem system, Connection connection) {

        if (!transportSystem2Connections.keySet().contains(system))
            transportSystem2Connections.put(system, new ArrayList<>());

        transportSystem2Connections.get(system).add(connection);
    }

}
