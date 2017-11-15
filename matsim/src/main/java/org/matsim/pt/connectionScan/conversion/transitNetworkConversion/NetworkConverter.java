package org.matsim.pt.connectionScan.conversion.transitNetworkConversion;

import edu.kit.ifv.mobitopp.publictransport.connectionscan.TransitNetwork;
import edu.kit.ifv.mobitopp.publictransport.model.*;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NetworkConverter {
    private static final Logger log = Logger.getLogger(NetworkConverter.class);

    //matsim
    private TransitSchedule transitSchedule;

    //transformation
    private MappingHandler idAndMappingHandler;
    //TODO
    private CoordinateTransformation coordinateTransformation;

    //connection-scan
    private List<Stop> stops = new ArrayList<>();
    private Connections connections = new Connections();
    private Time day;


    public NetworkConverter(TransitSchedule transitSchedule) {

        this.transitSchedule = transitSchedule;
        this.idAndMappingHandler = new MappingHandler();
        createDay();
    }

    public TransitNetwork convert() {
        log.info("Start converting TransitNetwork");

        StopConverter stopConverter = new StopConverter(transitSchedule.getFacilities(), idAndMappingHandler);
        stopConverter.convert();
        this.stops = stopConverter.getConnectionScanStops();

        ConnectionConverter connectionConverter = new ConnectionConverter(transitSchedule.getTransitLines(),
                idAndMappingHandler, getDay());
        connectionConverter.convert();
        this.connections = connectionConverter.getConnections();

        TransitNetwork transitNetwork = TransitNetwork.createOf(stops, connections);
        log.info("Finished converting TransitNetwork");
        return transitNetwork;
    }

    private void createDay() { this.day = new Time(LocalDateTime.of(2017, 3, 14, 0, 0));}

    public Time getDay() {
        return day;
    }

    public MappingHandler getMappingHandler() {
        return idAndMappingHandler;
    }
}
