package org.matsim.pt.connectionScan;

import edu.kit.ifv.mobitopp.publictransport.connectionscan.TransitNetwork;
import org.matsim.core.config.Config;
import org.matsim.pt.connectionScan.conversion.transitNetworkConversion.MappingHandler;
import org.matsim.pt.connectionScan.conversion.transitNetworkConversion.NetworkConverter;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class ConnectionScanTransitRouterProvider implements Provider<TransitRouter> {

    private final TransitRouterNetworkTravelTimeAndDisutility travelDisutility;
    private final TransitRouterConfig transitRouterConfig;
    private final TransitNetwork preparedTransitNetwork;
    private final MappingHandler mappingHandler;

    @Inject
    public ConnectionScanTransitRouterProvider(final TransitSchedule schedule, final Config config) {

        this.transitRouterConfig = new TransitRouterConfig(
                config.planCalcScore(),
                config.plansCalcRoute(),
                config.transitRouter(),
                config.vspExperimental());
        PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(schedule);
        travelDisutility = new TransitRouterNetworkTravelTimeAndDisutility(transitRouterConfig, preparedTransitSchedule);

        NetworkConverter networkConverter = new NetworkConverter(schedule, transitRouterConfig, travelDisutility);
        preparedTransitNetwork = networkConverter.convert();
        mappingHandler = networkConverter.getMappingHandler();
    }

    public ConnectionScanTransitRouterProvider(final TransitSchedule schedule, final TransitRouterConfig transitRouterConfig) {

        this.transitRouterConfig = transitRouterConfig;

        PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(schedule);
        travelDisutility = new TransitRouterNetworkTravelTimeAndDisutility(transitRouterConfig, preparedTransitSchedule);

        NetworkConverter networkConverter = new NetworkConverter(schedule, transitRouterConfig, travelDisutility);
        preparedTransitNetwork = networkConverter.convert();
        mappingHandler = networkConverter.getMappingHandler();
    }

    @Override
    public TransitRouter get() {
        return new ConnectionScanRouter(transitRouterConfig, this.travelDisutility, preparedTransitNetwork, mappingHandler);
    }
}


