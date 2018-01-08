package org.matsim.pt.connectionScan.conversion.transitNetworkConversion;

import edu.kit.ifv.mobitopp.publictransport.model.DefaultStation;
import edu.kit.ifv.mobitopp.publictransport.model.RelativeTime;
import edu.kit.ifv.mobitopp.publictransport.model.Station;
import edu.kit.ifv.mobitopp.publictransport.model.Stop;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.pt.connectionScan.utils.CoordinateUtils;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.awt.geom.Point2D;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

/**
 * TODO
 */
class StopConverter {

    private Map<Id<TransitStopFacility>, TransitStopFacility> matsimStops;
    private List<Stop> connectionScanStops = new ArrayList<>();
    private MappingHandler mappingHandler;
    private final double maxBeelineWalkConnectionDistance;
    private TransitTravelDisutility costFunction;
    private QuadTree<Stop> qtStops = null;

    /**
     * TODO
     * @param mappingHandler
     * @param costFunction
     */
    StopConverter(Map<Id<TransitStopFacility>, TransitStopFacility> matsimStops, MappingHandler mappingHandler,
                  double maxBeelineWalkConnectionDistance, TransitTravelDisutility costFunction) {

        this.matsimStops = matsimStops;
        this.mappingHandler = mappingHandler;
        this.maxBeelineWalkConnectionDistance = maxBeelineWalkConnectionDistance;
        this.costFunction = costFunction;
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
        addNeighbours();
    }

    public void buildQuadTree() {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        for (Stop stop : this.connectionScanStops) {
            Coord c = CoordinateUtils.convert2Coord(stop.coordinate());
            if (c.getX() < minX) {
                minX = c.getX();
            }
            if (c.getY() < minY) {
                minY = c.getY();
            }
            if (c.getX() > maxX) {
                maxX = c.getX();
            }
            if (c.getY() > maxY) {
                maxY = c.getY();
            }
        }

        QuadTree<Stop> quadTree = new QuadTree<>(minX, minY, maxX, maxY);
        for (Stop stop : this.connectionScanStops) {
            Coord c = CoordinateUtils.convert2Coord(stop.coordinate());
            quadTree.put(c.getX(), c.getY(), stop);
        }
        this.qtStops = quadTree;
    }

    private void addNeighbours() {

        buildQuadTree();
        for (Stop stop : connectionScanStops) {
            for (Stop nearStop : getNearestNodes(stop.coordinate(), maxBeelineWalkConnectionDistance)) {
                if ((stop != nearStop)) {
                    if (stop.station() != nearStop.station()) {
                        stop.addNeighbour(nearStop, calcWalkTime(stop, nearStop));
                    }
                }
            }
        }
    }

    private RelativeTime calcWalkTime(Stop stop1, Stop stop2) {
        double walkTime = costFunction.getWalkTravelTime(null,
                CoordinateUtils.convert2Coord(stop1.coordinate()),
                CoordinateUtils.convert2Coord(stop2.coordinate()));
        return RelativeTime.of((long)walkTime, ChronoUnit.SECONDS);
    }

    public Collection<Stop> getNearestNodes(final Point2D coord, final double distance) {
        return this.qtStops.getDisk(coord.getX(), coord.getY(), distance);
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
        int id = mappingHandler.createAndMapId(matsimStop);
        String name = matsimStop.getName();
        Point2D location = CoordinateUtils.convert2Point2D(matsimStop.getCoord());
        RelativeTime changeTime = RelativeTime.ZERO;
        Station station = convert2Station(id);
        Stop stop = new Stop(id, name, location, changeTime, station, 0);
        mappingHandler.addMatsimId2StopMapping(matsimStop.getId(), stop);
        return stop;
    }

    /**
     * Converts a matsim/TransitStopFacility into a connection-scan/Station.
     *
     * @param id the matsim/TransitStopFacility ID that contains the required matsim/Id
     * @return the converted connection-scan Station
     */
    private Station convert2Station(int id) {
        return new DefaultStation(id, emptyList());
    }

    public List<Stop> getConnectionScanStops() {
        return connectionScanStops;
    }
}
