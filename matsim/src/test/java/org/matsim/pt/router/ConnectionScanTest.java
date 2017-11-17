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
import org.matsim.pt.connectionScan.ConnectionScan;
import org.matsim.pt.connectionScan.conversion.transitNetworkConversion.NetworkConverter;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.testcases.MatsimTestCase;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Junit Test for the TransitLeastCostPathTree.
 *
 * @author gabriel.thunig on 23.05.2016.
 */
public class ConnectionScanTest extends TestCase {

    private TransitNetwork network;
    private TransitRouterNetworkTravelTimeAndDisutility travelDisutility;
    private ConnectionScan connectionScan;
    private TransitRouterImpl transitRouterImpl;
    private Fixture f;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        instantiateNetworkAndTravelDisutility();
        instantiateFixtureAndRouter();
    }

    /**
     * Instantiates a new TransitLeastCostPathTree object with a sample transitSchedule and default configuration.
     */
    private void instantiateNetworkAndTravelDisutility() {
        Fixture f = new Fixture();
        f.init();
        TransitRouterConfig config = new TransitRouterConfig(f.scenario.getConfig().planCalcScore(),
                f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
                f.scenario.getConfig().vspExperimental());

        PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(f.schedule);
        travelDisutility = new TransitRouterNetworkTravelTimeAndDisutility(config, preparedTransitSchedule);

        NetworkConverter networkConverter = new NetworkConverter(f.schedule, config, travelDisutility);
        network = networkConverter.convert();

    }

    private void instantiateFixtureAndRouter() {

        f = new Fixture();
        f.init();

        connectionScan = new ConnectionScan(travelDisutility.config, f.schedule);
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
        Assert.assertNotNull(connectionScan);
        Assert.assertNotNull(transitRouterImpl);
    }

    public void testSingleConnection() {

        FakeFacility from = new FakeFacility(new Coord((double) 8000, (double) 5002));
        FakeFacility to = new FakeFacility(new Coord((double) 12000, (double) 5002));
        double departure = 60*60*5 + 60*10;

        List<Leg> legs = connectionScan.calcRoute(from, to, departure, null);

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

        List<Leg> legs = connectionScan.calcRoute(from, to, departure, null);

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

        TransitRouter router = connectionScan;

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

        TransitRouter router = connectionScan;

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

        TransitRouter router = connectionScan;

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

        TransitRouter router = connectionScan;

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
    }

    public void testLineChange() {

        TransitRouter router = connectionScan;

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
        //TODO is that legit?
        assertEquals(expectedTravelTime, actualTravelTime, 0.2);//MatsimTestCase.EPSILON);
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
