package org.matsim.pt.connectionScan.conversion.transitNetworkConversion;

import edu.kit.ifv.mobitopp.publictransport.connectionscan.TransitNetwork;
import edu.kit.ifv.mobitopp.publictransport.model.Connections;
import edu.kit.ifv.mobitopp.publictransport.model.Stop;
import edu.kit.ifv.mobitopp.time.Time;
import org.apache.log4j.Logger;
import org.matsim.pt.connectionScan.model.Day;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitTravelDisutility;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.util.ArrayList;
import java.util.List;

public class NetworkConverter {
    private static final Logger log = Logger.getLogger(NetworkConverter.class);

    //matsim
    private TransitSchedule transitSchedule;

    //transformation
    private MappingHandler idAndMappingHandler;

    //connection-scan
    private List<Stop> stops = new ArrayList<>();
    private Connections connections = new Connections();
    private Time day;
    private TransitRouterConfig config;
    private TransitTravelDisutility costFunction;


    public NetworkConverter(TransitSchedule transitSchedule, TransitRouterConfig config, TransitTravelDisutility costFunction) {

        this.transitSchedule = transitSchedule;
        this.idAndMappingHandler = new MappingHandler();
        this.config = config;
        this.costFunction = costFunction;
        createDay();
    }

    public TransitNetwork convert() {
        log.info("Start converting TransitNetwork");

        StopConverter stopConverter = new StopConverter(transitSchedule.getFacilities(), idAndMappingHandler,
                config.getBeelineWalkConnectionDistance(), costFunction, config);
//        stopConverter.convert();

        ConnectionConverter connectionConverter = new ConnectionConverter(stopConverter,
                transitSchedule.getTransitLines(),
                idAndMappingHandler, getDay());
        connectionConverter.convert();
        this.stops = stopConverter.getConnectionScanStops();
        this.connections = connectionConverter.getConnections();

//        LoopFinder.hasLoop(this.connections);

        TransitNetwork transitNetwork = TransitNetwork.createOf(stops, connections);
        log.info("Finished converting TransitNetwork");
        return transitNetwork;
    }

    private void createDay() { this.day = Day.getDay();}

    public Time getDay() {
        return day;
    }

    public MappingHandler getMappingHandler() {
        return idAndMappingHandler;
    }
}
