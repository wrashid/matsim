package org.matsim.pt.connectionScan.conversion;

import edu.kit.ifv.mobitopp.publictransport.model.DefaultStation;
import edu.kit.ifv.mobitopp.publictransport.model.RelativeTime;
import edu.kit.ifv.mobitopp.publictransport.model.Station;
import edu.kit.ifv.mobitopp.publictransport.model.Stop;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

class StopConverter {

    private Map<Id<TransitStopFacility>, TransitStopFacility> matsimStops;
    private List<Stop> connectionScanStops = new ArrayList<>();

    StopConverter(Map<Id<TransitStopFacility>, TransitStopFacility> matsimStops) {

        this.matsimStops = matsimStops;
    }

    List<Stop> convert() {

        for (TransitStopFacility matsimStop : matsimStops.values()) {
            Stop connectionScanStop = convertStop(matsimStop);
            connectionScanStops.add(connectionScanStop);
        }

        return connectionScanStops;
    }

    /**
     * Converts a matsim/TransitStopFacility into a Stop from mobitopp/connection-scan.
     *
     * @param matsimStop the matsim/TransitStopFacility that is to be converted
     * @return the converted connection-scan stop
     */
    private Stop convertStop(TransitStopFacility matsimStop) {
        int id = Integer.getInteger(matsimStop.getId().toString());
        String name = matsimStop.getName();
        Point2D location = convert2Point2D(matsimStop.getCoord());
        RelativeTime changeTime = RelativeTime.ZERO;
        Station station = convert2Station(matsimStop);
        return new Stop(id, name, location, changeTime, station, 0);
    }

    /**
     * Converts a matsim/Coord into a Point2D.
     *
     * @param matsimCoord the matsim/TransitStopFacility that contains the required matsim/Coord
     * @return the converted Point2D
     */
    private Point2D convert2Point2D(Coord matsimCoord) {
        return new Point2D.Double(matsimCoord.getX(), matsimCoord.getY());
    }

    /**
     * Converts a matsim/TransitStopFacility into a Station from mobitopp/connection-scan.
     *
     * @param matsimStop the matsim/TransitStopFacility that contains the required matsim/Id
     * @return the converted connection-scan Station
     */
    private Station convert2Station(TransitStopFacility matsimStop) {
        return new DefaultStation(Integer.getInteger(matsimStop.getId().toString()), emptyList());
    }
}
