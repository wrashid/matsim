package org.matsim.pt.connectionScan.utils;


import edu.kit.ifv.mobitopp.publictransport.model.RelativeTime;
import edu.kit.ifv.mobitopp.publictransport.model.Stop;
import edu.kit.ifv.mobitopp.publictransport.model.StopPath;
import edu.kit.ifv.mobitopp.publictransport.model.Time;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.Facility;
import org.matsim.pt.connectionScan.model.StopAndInitialData;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static java.time.temporal.ChronoUnit.MINUTES;
import static java.time.temporal.ChronoUnit.NANOS;

public class TransitNetworkUtils {

    public static List<StopAndInitialData> getNearestStops(List<Stop> stops, Facility<?> facility, double withinDistance) {
        List<StopAndInitialData> nearestStops = new ArrayList<>();

        for (Stop currentStop : stops) {
            Coord stopCoord = new Coord(currentStop.coordinate().getX(), currentStop.coordinate().getY());
            double currentDistance = CoordUtils.calcEuclideanDistance(facility.getCoord(), stopCoord);
            if (currentDistance <= withinDistance) {
                StopAndInitialData nearStop = new StopAndInitialData(currentStop);
                nearStop.setInitialDistance(currentDistance);
                nearestStops.add(nearStop);
            }
        }

        return nearestStops;
    }

    public static StopAndInitialData getNearestStop(List<Stop> stops, Facility<?> facility, double withinDistance) {
        StopAndInitialData nearestStop = null;
        double nearestDistance = withinDistance;

        for (Stop currentStop : stops) {
            Coord stopCoord = new Coord(currentStop.coordinate().getX(), currentStop.coordinate().getY());
            double currentDistance = CoordUtils.calcEuclideanDistance(facility.getCoord(), stopCoord);
            if (currentDistance < nearestDistance) {
                nearestStop = new StopAndInitialData(currentStop);
                nearestStop.setInitialDistance(currentDistance);
                nearestDistance = currentDistance;
            }
        }

        return nearestStop;
    }

    public static RelativeTime convertTime(double time) {
        return RelativeTime.of((long) (time * Math.pow(10, 9)), ChronoUnit.NANOS);
    }

    public static double convertTime(RelativeTime relativeTime) {
        return relativeTime.toDuration().toNanos()* Math.pow(10, -9);
    }

    public static double convertTime(Time time) {
        return time.time().toLocalTime().toNanoOfDay() * Math.pow(10, -9);
    }
}
