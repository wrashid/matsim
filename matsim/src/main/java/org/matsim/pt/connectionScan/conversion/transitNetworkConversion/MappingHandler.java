package org.matsim.pt.connectionScan.conversion.transitNetworkConversion;

import edu.kit.ifv.mobitopp.publictransport.model.Stop;
import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.HashMap;
import java.util.Map;

public class MappingHandler {

    private Map<Integer, TransitStopFacility> stopId2TransitStopFacility = new HashMap<>();
    private Map<Id<TransitStopFacility>, Stop> matsimId2Stop = new HashMap<>();
    private Map<Integer, Id[]> connectionId2LineAndRouteId = new HashMap<>();

    private int conectionIdCounter = 0;
    private int journeyIdCounter = 0;

    /**
     * TODO
     * @param matsimStop
     * @return
     */
    int createAndMapId(TransitStopFacility matsimStop) {
        int newId = stopId2TransitStopFacility.size();
        stopId2TransitStopFacility.put(newId, matsimStop);
        return newId;
    }

    void addMatsimId2StopMapping(Id<TransitStopFacility> id, Stop stop) {
        this.matsimId2Stop.put(id, stop);
    }

    void addConnectionId2LineAndRouteId(int connectionId, Id[] lineAndRouteId) {
        this.connectionId2LineAndRouteId.put(connectionId, lineAndRouteId);
    }

    public Map<Integer, TransitStopFacility> getStopId2TransitStopFacility() {
        return stopId2TransitStopFacility;
    }

    public Map<Id<TransitStopFacility>, Stop> getMatsimId2Stop() {
        return matsimId2Stop;
    }

    public Map<Integer, Id[]> getConnectionId2LineAndRouteId() {
        return connectionId2LineAndRouteId;
    }

    int createNewConectionId() {
        return conectionIdCounter++;
    }

    int createNewJourneyId() {
        return journeyIdCounter++;
    }
}
