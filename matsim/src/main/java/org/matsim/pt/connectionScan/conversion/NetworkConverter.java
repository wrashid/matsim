package org.matsim.pt.connectionScan.conversion;

import edu.kit.ifv.mobitopp.publictransport.connectionscan.TransitNetwork;
import edu.kit.ifv.mobitopp.publictransport.model.*;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NetworkConverter {

    //matsim
    private TransitSchedule transitSchedule;

    //transformation
    private IdAndMappingHandler idAndMappingHandler;
    //TODO
    private CoordinateTransformation coordinateTransformation;

    //connection-scan
    private List<Stop> stops = new ArrayList<>();
    private Connections connections = new Connections();
    private Time day;


    public NetworkConverter(TransitSchedule transitSchedule) {

        this.transitSchedule = transitSchedule;
        this.idAndMappingHandler = new IdAndMappingHandler();
        createDay();
    }

    public TransitNetwork convert() {

        StopConverter stopConverter = new StopConverter(idAndMappingHandler);
        ConnectionConverter connectionConverter = new ConnectionConverter(transitSchedule.getTransitLines(),
                stopConverter, idAndMappingHandler, getDay());
        connectionConverter.convert();
        this.stops = stopConverter.getConnectionScanStops();
        this.connections = connectionConverter.getConnections();

        return TransitNetwork.createOf(stops, connections);
    }

    private void createDay() { this.day = new Time(LocalDateTime.of(2017, 3, 14, 0, 0));}

    public Time getDay() {
        return day;
    }

    public Map<Id<TransitStopFacility>, Stop> getTransitStopFacilityId2StopMap() {
        return idAndMappingHandler.getMatsimId2Stop();
    }

    public Map<Integer, TransitStopFacility> getStopId2TransitStopFacilityMap() {
        return idAndMappingHandler.getStopId2TransitStopFacility();
    }
}
