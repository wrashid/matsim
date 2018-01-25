package org.matsim.pt.connectionScan;

import com.google.inject.Singleton;
import org.matsim.core.config.Config;
import org.matsim.pt.router.*;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import javax.inject.Inject;
import javax.inject.Provider;

@Singleton
public class ConnectionScanTransitRouterProvider implements Provider<TransitRouter> {


    private final TransitSchedule schedule;
    private final TransitRouterNetworkTravelTimeAndDisutility travelDisutility;
    private final TransitRouterConfig transitRouterConfig;

    @Inject
    ConnectionScanTransitRouterProvider(final TransitSchedule schedule, final Config config) {

        this.schedule = schedule;

        this.transitRouterConfig = new TransitRouterConfig(
                config.planCalcScore(),
                config.plansCalcRoute(),
                config.transitRouter(),
                config.vspExperimental());
        PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(this.schedule);
        travelDisutility = new TransitRouterNetworkTravelTimeAndDisutility(transitRouterConfig, preparedTransitSchedule);
    }

    @Override
    public TransitRouter get() {
        return new ConnectionScanRouter(transitRouterConfig, this.schedule);
    }
}


