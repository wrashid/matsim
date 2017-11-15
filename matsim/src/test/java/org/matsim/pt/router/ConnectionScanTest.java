package org.matsim.pt.router;

import edu.kit.ifv.mobitopp.publictransport.connectionscan.TransitNetwork;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Route;
import org.matsim.pt.connectionScan.ConnectionScan;
import org.matsim.pt.connectionScan.conversion.transitNetworkConversion.NetworkConverter;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.List;

/**
 * Junit Test for the TransitLeastCostPathTree.
 *
 * @author gabriel.thunig on 23.05.2016.
 */
public class ConnectionScanTest {

    private TransitNetwork network;
    private TransitRouterNetworkTravelTimeAndDisutility travelDisutility;

    /**
     * Instantiates a new TransitLeastCostPathTree object with a sample transitSchedule and default configuration.
     */
    public void instantiateNetworkAndTravelDisutility() {
        Fixture f = new Fixture();
        f.init();
        TransitRouterConfig config = new TransitRouterConfig(f.scenario.getConfig().planCalcScore(),
                f.scenario.getConfig().plansCalcRoute(), f.scenario.getConfig().transitRouter(),
                f.scenario.getConfig().vspExperimental());
        NetworkConverter networkConverter = new NetworkConverter(f.schedule);
        network = networkConverter.convert();

        PreparedTransitSchedule preparedTransitSchedule = new PreparedTransitSchedule(f.schedule);
        travelDisutility = new TransitRouterNetworkTravelTimeAndDisutility(config, preparedTransitSchedule);
    }

    /**
     * Check whether the @Before-instantiation is instantiating a network and a travelDisutility.
     */
    @Test
    public void TestNetworkAndTravelDisutilityInstantiated() {
        instantiateNetworkAndTravelDisutility();
        Assert.assertNotNull(network);
        Assert.assertNotNull(travelDisutility);
    }

    @Test
    public void TestSingleConnectionFromBToC() {
        instantiateNetworkAndTravelDisutility();
        Fixture f = new Fixture();
        f.init();

        ConnectionScan connectionScan = new ConnectionScan(travelDisutility.config, travelDisutility, f.schedule);
        TransitRouterImpl transitRouterImpl = new TransitRouterImpl(travelDisutility.config, f.schedule);

        TransitScheduleFactoryImpl factory = new TransitScheduleFactoryImpl();
        TransitStopFacility from = factory.createTransitStopFacility(Id.create("0", TransitStopFacility.class), new Coord((double) 8000, (double) 5002), false);
        TransitStopFacility to = factory.createTransitStopFacility(Id.create("1", TransitStopFacility.class), new Coord((double) 12000, (double) 5002), false);
        double departure = 60*60*5 + 60*10;

        List<Leg> result = connectionScan.calcRoute(from, to, departure, null);
        List<Leg> result2 = transitRouterImpl.calcRoute(from, to, departure, null);

        Assert.assertNotNull("No Route calculated.", result);
        Assert.assertTrue("Different Result from TransitRouterImpl", areLegListsEqual(result, result2));

    }

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
        if (leg1.getDepartureTime() != leg2.getDepartureTime())
            return false;
        if (!leg1.getMode().equals(leg2.getMode()))
            return false;
        if (leg1.getTravelTime() != leg2.getTravelTime())
            return false;
        if (!areRoutesEqual(leg1.getRoute(), leg2.getRoute()))
            return false;

        return true;
    }

    private boolean areRoutesEqual(Route route1, Route route2) {
        if (route1 == null && route2 == null)
            return true;
        if (!route1.getStartLinkId().equals(route2.getStartLinkId()))
            return false;
        if (!route1.getEndLinkId().equals(route2.getEndLinkId()))
            return false;

        return true;
    }

//    /**
//     * Get the very next transitNode.
//     * @param person Which person we are routing for. For default leave null.
//     * @param coord The origin of the tree.
//     * @param departureTime The time the person departures at the origin.
//     * @return the next transitNode.
//     */
//    private Map<Node, InitialNode> locateWrappedNearestTransitNode(Person person, Coord coord, double departureTime) {
//        TransitRouterNetwork.TransitRouterNetworkNode nearestNode = network.getNearestNode(coord);
//        Map<Node, InitialNode> wrappedNearestNodes = new LinkedHashMap<>();
//        Coord toCoord = nearestNode.stop.getStopFacility().getCoord();
//        double initialTime = travelDisutility.getWalkTravelTime(person, coord, toCoord);
//        double initialCost = travelDisutility.getWalkTravelDisutility(person, coord, toCoord);
//        wrappedNearestNodes.put(nearestNode, new InitialNode(initialCost, initialTime + departureTime));
//        return wrappedNearestNodes;
//    }
//
//    /**
//     * Try to route a standard connection.
//     */
//    @Test
//    public void TestValidRouting() {
//        instantiateNetworkAndTravelDisutility();
//        Coord fromCoord = new Coord(1050d, 1050d);
//        Map<Node, InitialNode> wrappedFromNodes = this.locateWrappedNearestTransitNode(null, fromCoord, 28800);
//		TransitLeastCostPathTree tree = new TransitLeastCostPathTree(network, travelDisutility, travelDisutility, wrappedFromNodes, null);
//        Coord toCoord = new Coord(3950d, 1050d);
//        Map<Node, InitialNode> wrappedToNodes = this.locateWrappedNearestTransitNode(null, toCoord, 28800);
//        Path path = tree.getPath(wrappedToNodes);
//        Assert.assertNotNull(path);
//        double pathCost = path.travelCost;
//        Assert.assertEquals(1.8d, pathCost, MatsimTestUtils.EPSILON);
//        double pathTime = path.travelTime;
//        Assert.assertEquals(540d, pathTime, MatsimTestUtils.EPSILON);
//    }
//
//    /**
//     * Try to route a connection with interchange.
//     */
//    @Test
//    public void TestValidRoutingWithInterchange() {
//        instantiateNetworkAndTravelDisutility();
//        Coord fromCoord = new Coord(1050d, 1050d);
//        Map<Node, InitialNode> wrappedFromNodes = this.locateWrappedNearestTransitNode(null, fromCoord, 28800);
//		TransitLeastCostPathTree tree = new TransitLeastCostPathTree(network, travelDisutility, travelDisutility, wrappedFromNodes, null);
//        Coord toCoord = new Coord(2050d, 2960d);
//        Map<Node, InitialNode> wrappedToNodes = this.locateWrappedNearestTransitNode(null, toCoord, 28800);
//        Path path = tree.getPath(wrappedToNodes);
//        Assert.assertNotNull(path);
//        double pathCost = path.travelCost;
//        Assert.assertEquals(1.7706666666666668d, pathCost, MatsimTestUtils.EPSILON);
//        double pathTime = path.travelTime;
//        Assert.assertEquals(231.20000000000073d, pathTime, MatsimTestUtils.EPSILON);
//    }
//
//    @Ignore
//    @Test
//    public void TestSpeedImprovementOnStopCriterion() {
//        Fixture f = new Fixture();
//        TestTimeCost tc = new TestTimeCost();
//        for (int i = 0; i < f.count-1; i++) {
//            tc.setData(Id.create(i, Link.class), 1.0, 1.0);
//        }
//        Map<Node, InitialNode> fromNodes = new HashMap<>();
//        fromNodes.put(f.network.getNodes().get(Id.create(0, Node.class)), new InitialNode(0.0, 0.0));
//        Map<Node, InitialNode> toNodes = new HashMap<>();
//        toNodes.put(f.network.getNodes().get(Id.create(1, Node.class)), new InitialNode(0.0, 0.0));
//        for (Node node : fromNodes.keySet()) {
//            System.out.println("From Node = " + node.getCoord());
//        }
//        for (Node node : toNodes.keySet()) {
//            System.out.println("To Node = " + node.getCoord());
//        }
//        long startTime = System.currentTimeMillis();
//        new TransitLeastCostPathTree(f.network, tc, tc, fromNodes, null);
//        long endTime = System.currentTimeMillis();
//        long elapsedTimeWithoutStopCreterion = (endTime - startTime);
//        startTime = System.currentTimeMillis();
//        new TransitLeastCostPathTree(f.network, tc, tc, fromNodes, toNodes, null);
//        endTime = System.currentTimeMillis();
//        long elapsedTimeWithStopCreterion = (endTime - startTime);
//        Double bareRatio = (double)elapsedTimeWithoutStopCreterion / (double)elapsedTimeWithStopCreterion;
//        System.out.println("bareRatio = " + bareRatio);
//        int ratio = (int) ((bareRatio) * 10);
//        System.out.println("Time without stop criterion = " + elapsedTimeWithoutStopCreterion);
//        System.out.println("Time with stop criterion = " + elapsedTimeWithStopCreterion);
//        System.out.println("ratio = " + ratio);
//        Assert.assertThat("Bad ratio",
//                ratio,
//                greaterThan(15));
//    }
//
//    /**
//     * Creates a simple network to be used in TestSpeedImprovementOnStopCriterion.
//     *
//     * <pre>
//     *   (0)--(1)--(2)--...--(9997)--(9998)--(9999)
//     * </pre>
//     *
//     * @author gthunig
//     */
//	/*package*/ static class Fixture {
//        /*package*/ Network network;
//
//        int count = 10000;
//
//        public Fixture() {
//            this.network = NetworkUtils.createNetwork();
//            Node linkStartNode = NetworkUtils.createAndAddNode(this.network, Id.create(0, Node.class), new Coord((double) 0, (double) 0));
//            for (int i = 1; i < count; i++) {
//                Node linkEndNode = NetworkUtils.createAndAddNode(this.network, Id.create(i, Node.class), new Coord((double) i, (double) 0));
//		final Node fromNode = linkStartNode;
//		final Node toNode = linkEndNode;
//                NetworkUtils.createAndAddLink(this.network,Id.create(i-1, Link.class), fromNode, toNode, 1000.0, 10.0, 2000.0, (double) 1 );
//                linkStartNode = linkEndNode;
//            }
//
//        }
//    }

}
