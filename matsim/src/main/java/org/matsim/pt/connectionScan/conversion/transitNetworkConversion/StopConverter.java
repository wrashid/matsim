package org.matsim.pt.connectionScan.conversion.transitNetworkConversion;

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

/**
 * TODO
 */
class StopConverter {

    private Map<Id<TransitStopFacility>, TransitStopFacility> matsimStops;
    private List<Stop> connectionScanStops = new ArrayList<>();
    private MappingHandler idAndMappingHandler;

    /**
     * TODO
     * @param idAndMappingHandler
     */
    StopConverter(Map<Id<TransitStopFacility>, TransitStopFacility> matsimStops, MappingHandler idAndMappingHandler) {

        this.matsimStops = matsimStops;
        this.idAndMappingHandler = idAndMappingHandler;
    }

    /**
     * Converts the given Map of matsim/TransitStopFacility into a List of connection-scan/Stop.
     * Thereby the method creates a idMap which points from newly for the connection-scan created ids
     *  to the old matsim-ids.
     */
    void convert() {

        for (TransitStopFacility matsimStop : matsimStops.values()) {
            convertAndAddStop(matsimStop);
        }
    }


    private void convertAndAddStop(TransitStopFacility matsimStop) {
        Stop stop = convertStop(matsimStop);
        this.connectionScanStops.add(stop);
    }

    /**
     * Converts a matsim/TransitStopFacility into a connection-scan/Stop.
     *
     * @param matsimStop the matsim/TransitStopFacility that is to be converted
     * @return the converted connection-scan stop
     */
    private Stop convertStop(TransitStopFacility matsimStop) {
        int id = idAndMappingHandler.createAndMapId(matsimStop);
        String name = matsimStop.getName();
        Point2D location = convert2Point2D(matsimStop.getCoord());
        RelativeTime changeTime = RelativeTime.ZERO;
        Station station = convert2Station(id);
        Stop stop = new Stop(id, name, location, changeTime, station, 0);
        idAndMappingHandler.addMatsimId2StopMapping(matsimStop.getId(), stop);
        return stop;
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
     * Converts a matsim/TransitStopFacility into a connection-scan/Station.
     *
     * @param matsimStop the matsim/TransitStopFacility that contains the required matsim/Id
     * @return the converted connection-scan Station
     */
    private Station convert2Station(int id) {
        return new DefaultStation(id, emptyList());
    }

    public List<Stop> getConnectionScanStops() {
        return connectionScanStops;
    }
}
