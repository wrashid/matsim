package org.matsim.pt.connectionScan.conversion.example;

import edu.kit.ifv.mobitopp.publictransport.connectionscan.ConnectionScan;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.PublicTransportRoute;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.RouteSearch;
import edu.kit.ifv.mobitopp.publictransport.connectionscan.TransitNetwork;
import edu.kit.ifv.mobitopp.publictransport.model.RelativeTime;
import edu.kit.ifv.mobitopp.publictransport.model.Stop;
import edu.kit.ifv.mobitopp.publictransport.model.Time;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.connectionScan.conversion.transitNetworkConversion.NetworkConverter;
import org.matsim.pt.router.FakeFacility;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleReader;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static com.github.npathai.hamcrestopt.OptionalMatchers.isPresent;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

public class ConversionPlayaround {

    public static void main(String[] args) {
        m2();
    }

    private static void m2() {
        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        TransitScheduleReader reader = new TransitScheduleReader(scenario);
        reader.readFile("C:\\Users\\gthunig\\Desktop\\Vsp\\ConnectionScan\\sampleSchedule.xml");

        TransitSchedule transitSchedule = scenario.getTransitSchedule();
        TestTimeCost tc = new TestTimeCost();
        TransitRouterConfig trc = new TransitRouterConfig(scenario.getConfig().planCalcScore(),
                scenario.getConfig().plansCalcRoute(), scenario.getConfig().transitRouter(),
                scenario.getConfig().vspExperimental());

        org.matsim.pt.connectionScan.ConnectionScan connectionScan =
                new org.matsim.pt.connectionScan.ConnectionScan(trc, tc, transitSchedule);

        FakeFacility fromFacility = new FakeFacility(new Coord(13.411267, 52.521512));
//        FakeFacility toFacility = new FakeFacility(new Coord(13.368924, 52.525847));
        FakeFacility toFacility = new FakeFacility(new Coord(13.308924, 52.521847));
        double departureTime = 4*60*60;

        List<Leg> result = connectionScan.calcRoute(fromFacility, toFacility, departureTime, null);

        assertNotNull(result);
        assert true;

//        Optional<PublicTransportRoute> potentialRoute = connectionScan.findRoute(fromAlex, toHbf, departure);
//        PublicTransportRoute route = potentialRoute.get();
//        TransitPassengerRouteConverter.fromPublicTransportRoute()
    }

    private static void m1() {
//        Config config = ConfigUtils.createConfig();
//        Scenario scenario = ScenarioUtils.createScenario(config);
//        TransitScheduleReader reader = new TransitScheduleReader(scenario);
//        reader.readFile("C:\\Users\\gthunig\\Desktop\\Vsp\\ConnectionScan\\sampleSchedule.xml");
//        TransitSchedule transitSchedule = scenario.getTransitSchedule();
//
//        NetworkConverter networkConverter = new NetworkConverter(transitSchedule);
//        TransitNetwork transitNetwork = networkConverter.convert();
//
//        Stop fromAlex = networkConverter.getTransitStopFacilityId2StopMap().
//                get(Id.create("000008011155", TransitStopFacility.class));
//        Stop toHbf = networkConverter.getTransitStopFacilityId2StopMap().
//                get(Id.create("000008011160", TransitStopFacility.class));
//        Time departure = networkConverter.getDay().add(RelativeTime.of((long)14400, ChronoUnit.SECONDS));
//
//        RouteSearch connectionScan = new ConnectionScan(transitNetwork);
//
//        Optional<PublicTransportRoute> potentialRoute = connectionScan.findRoute(fromAlex, toHbf, departure);
//
//        assertThat(potentialRoute, isPresent());
//        PublicTransportRoute route = potentialRoute.get();
////        assertThat(route.arrival(), is(equalTo(threeOClock)));
////        assertThat(route.start(), is(equalTo(fromAmsterdam)));
////        assertThat(route.end(), is(equalTo(toBerlin)));
////        assertThat(route.connections(), contains(amsterdamToDortmund, dortmundToBerlin));
    }

}
