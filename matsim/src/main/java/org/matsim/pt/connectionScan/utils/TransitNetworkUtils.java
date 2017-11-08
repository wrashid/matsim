package org.matsim.pt.connectionScan.utils;


import edu.kit.ifv.mobitopp.publictransport.connectionscan.TransitNetwork;
import edu.kit.ifv.mobitopp.publictransport.model.Stop;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;

import java.util.ArrayList;
import java.util.List;

public class TransitNetworkUtils {

    public static List<Stop> getNearestStops(List<Stop> stops, Facility<?> facility, double withinDistance) {
        List<Stop> nearestStops = new ArrayList<>();

        for (Stop currentStop : stops) {
            Coord stopCoord = new Coord(currentStop.coordinate().getX(), currentStop.coordinate().getY());
            double currentDistance = CoordUtils.calcEuclideanDistance(facility.getCoord(), stopCoord);
            if (currentDistance <= withinDistance) {
                nearestStops.add(currentStop);
            }
        }

        return nearestStops;
    }

    public static Stop getNearestStop(List<Stop> stops, Facility<?> facility, double withinDistance) {
        Stop nearestStop = null;
        double nearestDistance = withinDistance;

        for (Stop currentStop : stops) {
            Coord stopCoord = new Coord(currentStop.coordinate().getX(), currentStop.coordinate().getY());
            double currentDistance = CoordUtils.calcEuclideanDistance(facility.getCoord(), stopCoord);
            if (currentDistance < nearestDistance) {
                nearestStop = currentStop;
                nearestDistance = currentDistance;
            }
        }

        return nearestStop;
    }
}
