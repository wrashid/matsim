package org.matsim.pt.connectionScan.plausibilityCheck;

import edu.kit.ifv.mobitopp.publictransport.model.Connection;
import edu.kit.ifv.mobitopp.publictransport.model.Connections;
import edu.kit.ifv.mobitopp.publictransport.model.Journey;
import edu.kit.ifv.mobitopp.publictransport.model.Stop;

import java.util.*;

public class LoopFinder {

    public static boolean hasLoop(Connections connections) {

        Map<Integer, Connections> journeys = new HashMap<>();
        for (Connection con : connections.asCollection()) {
            if (!journeys.keySet().contains(con.journey().id())) {
                journeys.put(con.journey().id(), new Connections());
            }
            journeys.get(con.journey().id()).add(con);
        }

        for (Connections cons : journeys.values()) {
            if (hasLoopOnJourney(cons)) return true;
        }

        return false;
    }

    private static boolean hasLoopOnJourney(Connections cons) {

        List<Connection> connections = new ArrayList(cons.asCollection());

        for (int i = 0; i < connections.size(); i++) {
            Connection con1 = connections.get(i);
            Stop start1 = con1.start();
            Stop end1 = con1.end();

            for (int e = i+1; e < connections.size(); e++) {
                Connection con2 = connections.get(e);
                Stop end2 = con2.end();

                if (start1.equals(end1) || (start1.equals(end2) && !con1.departure().isBefore(con2.arrival()))) {
                    return true;
                }
            }
        }

        return false;
    }


}
