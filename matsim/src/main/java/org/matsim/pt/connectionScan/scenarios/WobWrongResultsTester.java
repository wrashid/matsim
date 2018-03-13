package org.matsim.pt.connectionScan.scenarios;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.connectionScan.ConnectionScanTransitRouterProvider;
import org.matsim.pt.router.FakeFacility;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.router.TransitRouterConfig;
import org.matsim.pt.router.TransitRouterImpl;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

import java.io.File;
import java.util.List;

public class WobWrongResultsTester {

    public static void main(String[] args) {

        File file = new File("");
        System.out.println(file.getAbsolutePath());

        String config = "../../..\\shared-svn\\projects\\ptrouting\\niedersachsen_sample_scenario/config.xml";

        Scenario scenario = ScenarioUtils.loadScenario(ConfigUtils.loadConfig(config));

        WobWrongResultsTester wobTester = new WobWrongResultsTester(
                scenario.getTransitSchedule(), new TransitRouterConfig(scenario.getConfig()));
        wobTester.testCase1();
    }

    private TransitRouter router;

    private WobWrongResultsTester(TransitSchedule schedule, TransitRouterConfig trConfig) {

        this.router = new TransitRouterImpl(trConfig, schedule);

//        ConnectionScanTransitRouterProvider provider = new ConnectionScanTransitRouterProvider(schedule, trConfig);
//        this.router = provider.get();
    }

    private void testCase1() {

        Coord fromCoord = new Coord(603986.1825117596, 5791003.143343626);
        Coord toCoord = new Coord(604072.3705846588, 5791726.736841967);
        double departureTime = 8*3600 + 15*60 + 39;
        List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), departureTime, null);
        assert legs.size() == 3;
    }

}
