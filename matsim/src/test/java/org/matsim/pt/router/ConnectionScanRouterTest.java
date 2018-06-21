package org.matsim.pt.router;

import edu.kit.ifv.mobitopp.publictransport.connectionscan.TransitNetwork;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.runners.Parameterized;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.connectionScan.ConnectionScanTransitRouterProvider;
import org.matsim.pt.connectionScan.conversion.transitNetworkConversion.NetworkConverter;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Junit Test for the ConnectionScan algorithm.
 *
 * @author gthunig
 */
public class ConnectionScanRouterTest extends TransitRouterImplTest {

    private static final Logger log = Logger.getLogger(ConnectionScanRouterTest.class);

    @Parameterized.Parameters(name = "{index}: TransitRouter == {0}")
    public static Collection<Object> createRouterTypes() {
        Object[] router = new Object[]{
//                "standard",
                "connectionScan"
                // expect 4 tests to fail.
                // 2 because of lineSwitchUtility (connection scan does not support that)
                // 1 because of travel-time-differences in nanoSeconds-range (CS does jet not support that either)
                // 1 because in CS you cannot enter a vehicle the same second you start at a stop
                // further issues that don't lead to failed tests here:
                //
        };
        return Arrays.asList(router);
    }

    public ConnectionScanRouterTest(String routerType) {
        super(routerType);
        log.warn("using router=" + routerType);
    }


    protected TransitRouter createTransitRouter(TransitSchedule schedule, TransitRouterConfig trConfig, String routerType) {
        TransitRouter router = null;
        switch (routerType) {
            case "standard":
                router = new TransitRouterImpl(trConfig, schedule);
                break;
            case "connectionScan":
                ConnectionScanTransitRouterProvider provider = new ConnectionScanTransitRouterProvider(schedule, trConfig);
                router = provider.get();
                break;
        }
        return router;
    }

}
