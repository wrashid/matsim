package org.matsim.pt.router;

import edu.kit.ifv.mobitopp.publictransport.connectionscan.TransitNetwork;
import junit.framework.TestCase;
import org.junit.Assert;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.connectionScan.conversion.transitNetworkConversion.NetworkConverter;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;

import java.util.List;

/**
 * Junit Test for the TransitLeastCostPathTree.
 *
 * @author gabriel.thunig on 23.05.2016.
 */
public class ConnectionScanRouterTest extends TestCase {

    private TransitNetwork network;
    private TransitRouterNetworkTravelTimeAndDisutility travelDisutility;
    private ConnectionScanRouter connectionScanRouter;
    private TransitRouterImpl transitRouterImpl;
    private Fixture f;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        instantiateFixtureNetworkAndTravelDisutility();
        instantiateRouters();
    }

    /**
     * Instantiates a new TransitLeastCostPathTree object with a sample transitSchedule and default configuration.
     */
    private void instantiateFixtureNetworkAndTravelDisutility() {
        f = new Fixture();
        f.init();
        TransitRouterConfig config = new TransitRouterConfig(f.scenario.getConfig().planCalcScore(),
                f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
                f.scenario.getConfig().vspExperimental());

        PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(f.schedule);
        travelDisutility = new TransitRouterNetworkTravelTimeAndDisutility(config, preparedTransitSchedule);

        NetworkConverter networkConverter = new NetworkConverter(f.schedule, config, travelDisutility);
        network = networkConverter.convert();

    }

    private void instantiateRouters() {

        connectionScanRouter = new ConnectionScanRouter(travelDisutility.config, f.schedule);
        transitRouterImpl = new TransitRouterImpl(travelDisutility.config, f.schedule);
    }

    /**
     * Check whether the SetUp-instantiation is instantiating a network and a travelDisutility.
     */
    public void testNetworkAndTravelDisutilityInstantiated() {

        Assert.assertNotNull(network);
        Assert.assertNotNull(travelDisutility);
    }

    public void testFixtureAndRouterInstantiated() {

        Assert.assertNotNull(f);
        Assert.assertNotNull(connectionScanRouter);
        Assert.assertNotNull(transitRouterImpl);
    }

    public void testSingleConnection() {

        FakeFacility from = new FakeFacility(new Coord((double) 8000, (double) 5002));
        FakeFacility to = new FakeFacility(new Coord((double) 12000, (double) 5002));
        double departure = 60*60*5 + 60*10;

        List<Leg> legs = connectionScanRouter.calcRoute(from, to, departure, null);

        Assert.assertNotNull("No Route calculated.", legs);
        assertEquals(3, legs.size());
        assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
        Assert.assertEquals("Wrong travel time", 480, legs.get(1).getTravelTime(), MatsimTestCase.EPSILON);
        assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
        ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
        assertEquals(Id.create("2", TransitStopFacility.class), ptRoute.getAccessStopId());
        assertEquals(Id.create("4", TransitStopFacility.class), ptRoute.getEgressStopId());
        assertEquals(f.blueLine.getId(), ptRoute.getLineId());
        assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());

    }

    public void testSingleLine() {

        FakeFacility from = new FakeFacility(new Coord((double) 4000, (double) 5002));
        FakeFacility to = new FakeFacility(new Coord((double) 16000, (double) 5002));
        double departure = 60*60*5 + 60*5;

        List<Leg> legs = connectionScanRouter.calcRoute(from, to, departure, null);

        Assert.assertNotNull("No Route calculated.", legs);
        assertEquals(3, legs.size());
        assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
        Assert.assertEquals("Wrong travel time", 1440, legs.get(1).getTravelTime(), MatsimTestCase.EPSILON);
        assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
        ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
        assertEquals(Id.create("0", TransitStopFacility.class), ptRoute.getAccessStopId());
        assertEquals(Id.create("6", TransitStopFacility.class), ptRoute.getEgressStopId());
        assertEquals(f.blueLine.getId(), ptRoute.getLineId());
        assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());

    }

    public void testSingleLineWithTransitWalk() {

        TransitRouter router = connectionScanRouter;

        Coord fromCoord = new Coord(3800, 5100);
        Coord toCoord = new Coord(16100, 5050);
        FakeFacility from = new FakeFacility(fromCoord);
        FakeFacility to = new FakeFacility(toCoord);
        double departure = 60*60*5;

        List<Leg> legs = router.calcRoute(from, to, departure, null);

        assertEquals(3, legs.size());
        assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
        assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
        ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
        assertEquals(Id.create("0", TransitStopFacility.class), ptRoute.getAccessStopId());
        assertEquals(Id.create("6", TransitStopFacility.class), ptRoute.getEgressStopId());
        assertEquals(f.blueLine.getId(), ptRoute.getLineId());
        assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());
        double actualTravelTime = 0.0;
        for (Leg leg : legs) {
            actualTravelTime += leg.getTravelTime();
        }
        double expectedTravelTime = 29.0 * 60 + // agent takes the *:06 course, arriving in D at *:29
        CoordUtils.calcEuclideanDistance(
                f.schedule.getFacilities().get(Id.create("6", TransitStopFacility.class)).getCoord(),
                toCoord) / travelDisutility.config.getBeelineWalkSpeed();
        assertEquals(expectedTravelTime, actualTravelTime, MatsimTestCase.EPSILON);
    }

    public void testFromToSameStop() {

        TransitRouter router = connectionScanRouter;

        Coord fromCoord = new Coord((double) 3800, (double) 5100);
        Coord toCoord = new Coord((double) 4100, (double) 5050);
        List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 5.0*3600, null);
        assertEquals(1, legs.size());
        assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
        double actualTravelTime = 0.0;
        for (Leg leg : legs) {
            actualTravelTime += leg.getTravelTime();
        }
        double expectedTravelTime =
                CoordUtils.calcEuclideanDistance(fromCoord, toCoord) / travelDisutility.config.getBeelineWalkSpeed();
        assertEquals(expectedTravelTime, actualTravelTime, MatsimTestCase.EPSILON);
    }

    public void testDirectWalkCheaper() {

        TransitRouter router = connectionScanRouter;

        Coord fromCoord = new Coord((double) 4000, (double) 3000);
        Coord toCoord = new Coord((double) 8000, (double) 3000);
        List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 5.0*3600, null);
        assertEquals(1, legs.size());
        assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
        double actualTravelTime = 0.0;
        for (Leg leg : legs) {
            actualTravelTime += leg.getTravelTime();
        }
        double expectedTravelTime =
                CoordUtils.calcEuclideanDistance(fromCoord, toCoord) / travelDisutility.config.getBeelineWalkSpeed();
        assertEquals(expectedTravelTime, actualTravelTime, MatsimTestCase.EPSILON);
    }

    public void testSingleLine_DifferentWaitingTime() {

        TransitRouter router = connectionScanRouter;

        long start = System.currentTimeMillis();

        Coord fromCoord = new Coord((double) 4000, (double) 5002);
        Coord toCoord = new Coord((double) 8000, (double) 5002);

        double inVehicleTime = 7.0*60; // travel time from A to B
        for (int min = 0; min < 30; min += 3) {
            List<Leg> legs = router.calcRoute(new FakeFacility(fromCoord), new FakeFacility(toCoord), 5.0*3600 + min*60, null);
            assertEquals(3, legs.size()); // walk-pt-walk
            double actualTravelTime = 0.0;
            for (Leg leg : legs) {
                actualTravelTime += leg.getTravelTime();
            }
            double waitingTime = ((46 - min) % 20) * 60; // departures at *:06 and *:26 and *:46
            assertEquals("expected different waiting time at 05:"+min, waitingTime, actualTravelTime - inVehicleTime, MatsimTestCase.EPSILON);
        }

        long end = System.currentTimeMillis();
        long completedIn = end - start;

        int hours = (int)(completedIn / 3600000); completedIn -= hours * 3600000;
        int mins = (int)(completedIn / 60000); completedIn -= mins * 60000;
        int secs = (int)(completedIn / 1000); completedIn -= secs * 1000;

        System.out.println(String.format("%d:%d:%d.%d", hours, mins, secs, completedIn));
    }

    public void testLineChange() {

        TransitRouter router = connectionScanRouter;

        long start = System.currentTimeMillis();

        Coord toCoord = new Coord((double) 16100, (double) 10050);
        List<Leg> legs = router.calcRoute(new FakeFacility(new Coord((double) 3800, (double) 5100)), new FakeFacility(toCoord), 6.0*3600, null);
        assertEquals(5, legs.size());
        assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
        assertEquals(TransportMode.pt, legs.get(3).getMode());
        assertEquals(TransportMode.transit_walk, legs.get(4).getMode());
        assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
        ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
        assertEquals(Id.create("0", TransitStopFacility.class), ptRoute.getAccessStopId());
        assertEquals(Id.create("4", TransitStopFacility.class), ptRoute.getEgressStopId());
        assertEquals(f.blueLine.getId(), ptRoute.getLineId());
        assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());
        assertTrue("expected TransitRoute in leg.", legs.get(3).getRoute() instanceof ExperimentalTransitRoute);
        ptRoute = (ExperimentalTransitRoute) legs.get(3).getRoute();
        assertEquals(Id.create("18", TransitStopFacility.class), ptRoute.getAccessStopId());
        assertEquals(Id.create("19", TransitStopFacility.class), ptRoute.getEgressStopId());
        assertEquals(f.greenLine.getId(), ptRoute.getLineId());
        assertEquals(Id.create("green clockwise", TransitRoute.class), ptRoute.getRouteId());
        double actualTravelTime = 0.0;
        for (Leg leg : legs) {
            actualTravelTime += leg.getTravelTime();
        }
        double expectedTravelTime = 31.0 * 60 + // agent takes the *:06 course, arriving in C at *:18, departing at *:21, arriving in K at*:31
                CoordUtils.calcEuclideanDistance(
                        f.schedule.getFacilities().get(Id.create("19", TransitStopFacility.class)).getCoord(), toCoord)
                        / travelDisutility.config.getBeelineWalkSpeed();
        assertEquals(expectedTravelTime, actualTravelTime, MatsimTestCase.EPSILON);

        long end = System.currentTimeMillis();
        long completedIn = end - start;

        int hours = (int)(completedIn / 3600000); completedIn -= hours * 3600000;
        int mins = (int)(completedIn / 60000); completedIn -= mins * 60000;
        int secs = (int)(completedIn / 1000); completedIn -= secs * 1000;

        System.out.println(String.format("%d:%d:%d.%d", hours, mins, secs, completedIn));
    }


    public void testTransferWeights() {
		/* idea: travel from C to F
		 * If starting at the right time, one could take the red line to G and travel back with blue to F.
		 * If one doesn't want to switch lines, one could take the blue line from C to F directly.
		 * Using the red line (dep *:00, change at G *:09/*:12) results in an arrival time of *:19,
		 * using the blue line only (dep *:02) results in an arrival time of *:23. So the line switch
		 * cost must be larger than 4 minutes to have an effect.
		 */

        travelDisutility.config.setUtilityOfLineSwitch_utl(0);

        TransitRouter router = connectionScanRouter;
        List<Leg> legs = router.calcRoute(new FakeFacility(new Coord((double) 11900, (double) 5100)),
                new FakeFacility(new Coord((double) 24100, (double) 4950)),
                6.0*3600 /*- 5.0*60*/, null);
        assertEquals(5, legs.size());
        assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(f.redLine.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getLineId());
        assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
        assertEquals(TransportMode.pt, legs.get(3).getMode());
        assertEquals(f.blueLine.getId(), ((ExperimentalTransitRoute) legs.get(3).getRoute()).getLineId());
        assertEquals(TransportMode.transit_walk, legs.get(4).getMode());

        travelDisutility.config.setUtilityOfLineSwitch_utl(
                300.0 * travelDisutility.config.getMarginalUtilityOfTravelTimePt_utl_s()); // corresponds to 5 minutes transit travel time
        legs = router.calcRoute(new FakeFacility(new Coord((double) 11900, (double) 5100)),
                new FakeFacility(new Coord((double) 24100, (double) 4950)),
                6.0*3600 - 5.0*60, null);
        assertEquals(3, legs.size());
        assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(f.blueLine.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getLineId());
        assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
    }

    public void testTransferTime() {
		/* idea: travel from C to F
		 * If starting at the right time, one could take the red line to G and travel back with blue to F.
		 * If one doesn't want to switch lines, one could take the blue line from C to F directly.
		 * Using the red line (dep *:00, change at G *:09/*:12) results in an arrival time of *:19,
		 * using the blue line only (dep *:02) results in an arrival time of *:23.
		 * For the line switch at G, 3 minutes are available. If the additional "savety" time is larger than
		 * that, the direct connection should be taken.
		 */
        travelDisutility.config.setUtilityOfLineSwitch_utl(0);
        assertEquals(0, travelDisutility.config.getAdditionalTransferTime(), 1e-8);
        TransitRouter router = transitRouterImpl;
        List<Leg> legs = router.calcRoute(new FakeFacility(new Coord((double) 11900, (double) 5100)), new FakeFacility(new Coord((double) 24100, (double) 4950)), 6.0*3600 - 5.0*60, null);
        assertEquals(5, legs.size());
        assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(f.redLine.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getLineId());
        assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
        assertEquals(TransportMode.pt, legs.get(3).getMode());
        assertEquals(f.blueLine.getId(), ((ExperimentalTransitRoute) legs.get(3).getRoute()).getLineId());
        assertEquals(TransportMode.transit_walk, legs.get(4).getMode());

        travelDisutility.config.setAdditionalTransferTime(3.0*60); // 3 mins already enough, as there is a small distance to walk anyway which adds some time
        router = transitRouterImpl; // this is necessary to update the router for any change in config. At least raptor transit router fails without this. Amit Sep'17.
        legs = router.calcRoute(new FakeFacility(new Coord((double) 11900, (double) 5100)), new FakeFacility(new Coord((double) 24100, (double) 4950)), 6.0*3600 - 5.0*60, null);
        assertEquals(3, legs.size());
        assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(f.blueLine.getId(), ((ExperimentalTransitRoute) legs.get(1).getRoute()).getLineId());
        assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
    }

    public void testAfterMidnight() {

        travelDisutility.config.setBeelineWalkSpeed(0.1); // something very slow, so the agent does not walk over night
        TransitRouter router = connectionScanRouter;
        Coord toCoord = new Coord((double) 16100, (double) 5050);
        List<Leg> legs = router.calcRoute(new FakeFacility(new Coord((double) 3800, (double) 5100)), new FakeFacility(toCoord), 25.0*3600, null);
        assertEquals(3, legs.size());
        assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
        assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
        ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
        assertEquals(Id.create("0", TransitStopFacility.class), ptRoute.getAccessStopId());
        assertEquals(Id.create("6", TransitStopFacility.class), ptRoute.getEgressStopId());
        assertEquals(f.blueLine.getId(), ptRoute.getLineId());
        assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());
        double actualTravelTime = 0.0;
        for (Leg leg : legs) {
            actualTravelTime += leg.getTravelTime();
        }
        double expectedTravelTime = 4*3600 + 29.0 * 60 + // arrival at 05:29 at D
                CoordUtils.calcEuclideanDistance(f.schedule.getFacilities().get(Id.create("6", TransitStopFacility.class)).getCoord(), toCoord) / travelDisutility.config.getBeelineWalkSpeed();
        assertEquals(expectedTravelTime, actualTravelTime, MatsimTestUtils.EPSILON);
    }

    public void testCoordFarAway() {
        TransitRouter router = transitRouterImpl;
        double x = +42000;
        double x1 = -2000;
        List<Leg> legs = router.calcRoute(new FakeFacility(new Coord(x1, (double) 0)), new FakeFacility(new Coord(x, (double) 0)), 5.5*3600, null); // should map to stops A and I
        assertEquals(3, legs.size());
        assertEquals(TransportMode.transit_walk, legs.get(0).getMode());
        assertEquals(TransportMode.pt, legs.get(1).getMode());
        assertEquals(TransportMode.transit_walk, legs.get(2).getMode());
        assertTrue("expected TransitRoute in leg.", legs.get(1).getRoute() instanceof ExperimentalTransitRoute);
        ExperimentalTransitRoute ptRoute = (ExperimentalTransitRoute) legs.get(1).getRoute();
        assertEquals(Id.create("0", TransitStopFacility.class), ptRoute.getAccessStopId());
        assertEquals(Id.create("16", TransitStopFacility.class), ptRoute.getEgressStopId());
        assertEquals(f.blueLine.getId(), ptRoute.getLineId());
        assertEquals(Id.create("blue A > I", TransitRoute.class), ptRoute.getRouteId());
    }





    //TODO move or remove
    private boolean areLegListsEqual(List<Leg> list1, List<Leg> list2) {
        if (list1.size() != list2.size())
            return false;
        for (int i = 0; i < list1.size(); i++) {
            if (!areLegsEqual(list1.get(i), list2.get(i)))
                return false;
        }
        return true;
    }

    private boolean areLegsEqual(Leg leg1, Leg leg2) {
        return !(leg1.getDepartureTime() != leg2.getDepartureTime())
                && leg1.getMode().equals(leg2.getMode())
                && !(leg1.getTravelTime() != leg2.getTravelTime())
                && areRoutesEqual(leg1.getRoute(), leg2.getRoute());
    }

    private boolean areRoutesEqual(Route route1, Route route2) {
        if (route1 != null && route2 != null) {
            return (route1.getStartLinkId().equals(route2.getStartLinkId())
                    && route1.getEndLinkId().equals(route2.getEndLinkId()));
        } else {
            return (route1 == null && route2 == null);
        }
    }

}
