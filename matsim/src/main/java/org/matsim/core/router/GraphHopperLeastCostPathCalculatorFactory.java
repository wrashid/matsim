package org.matsim.core.router;

import com.graphhopper.routing.Dijkstra;
import com.graphhopper.routing.util.*;
import com.graphhopper.storage.GraphBuilder;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.util.EdgeIteratorState;
import gnu.trove.procedure.TIntProcedure;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GraphHopperLeastCostPathCalculatorFactory implements LeastCostPathCalculatorFactory {
    @Override
    public LeastCostPathCalculator createPathCalculator(final Network network, final TravelDisutility travelCosts, final TravelTime travelTimes) {
        EncodingManager encodingManager = new EncodingManager(new CarFlagEncoder());
        final GraphHopperStorage graph = new GraphBuilder(encodingManager).create();

        final Map<Integer, Id<Node>> gh2Matsim = new HashMap<>();
        final Map<Id<Node>, Integer> matsim2Gh = new HashMap<>();
        final Map<Integer, Link> edge2Link = new HashMap<>();
        int i=0;
        for (Map.Entry<Id<Node>, ? extends Node> entry : network.getNodes().entrySet()) {
            gh2Matsim.put(i, entry.getKey());
            matsim2Gh.put(entry.getKey(), i);
            i++;
        }
        for (Map.Entry<Id<Link>, ? extends Link> entry : network.getLinks().entrySet()) {
            EdgeIteratorState edge = graph.edge(matsim2Gh.get(entry.getValue().getFromNode().getId()),
                    matsim2Gh.get(entry.getValue().getToNode().getId()), entry.getValue().getLength(), false);
            edge2Link.put(edge.getEdge(), entry.getValue());
        }
        final FlagEncoder car = encodingManager.getEncoder("car");
        return new LeastCostPathCalculator() {
            @Override
            public Path calcLeastCostPath(Node fromNode, Node toNode, double starttime, Person person, Vehicle vehicle) {
                final Dijkstra dijkstra = new Dijkstra(graph, car, new Weighting() {
                    @Override
                    public double getMinWeight(double v) {
                        return 0;
                    }

                    @Override
                    public double calcWeight(EdgeIteratorState edgeIteratorState, boolean b, int i) {
                        return travelCosts.getLinkTravelDisutility(edge2Link.get(edgeIteratorState.getEdge()), 0.0, null, null);
                    }

                    @Override
                    public FlagEncoder getFlagEncoder() {
                        return car;
                    }

                    @Override
                    public String getName() {
                        return "Ulrich";
                    }

                    @Override
                    public boolean matches(HintsMap hintsMap) {
                        return true;
                    }
                }, TraversalMode.NODE_BASED);
                com.graphhopper.routing.Path path = dijkstra.calcPath(matsim2Gh.get(fromNode.getId()), matsim2Gh.get(toNode.getId()));
                final ArrayList<Node> nodes = new ArrayList<>();
                path.calcNodes().forEach(new TIntProcedure() {
                    @Override
                    public boolean execute(int i) {
                        nodes.add(network.getNodes().get(gh2Matsim.get(i)));
                        return true;
                    }
                });
                ArrayList<Link> links = new ArrayList<>();
                double travelTime = 0.0;
                for (EdgeIteratorState edge : path.calcEdges()) {
                    Link link = edge2Link.get(edge.getEdge());
                    links.add(link);
                    travelTime += travelTimes.getLinkTravelTime(link, 0.0, null, null);
                }
                return new Path(nodes, links, travelTime, path.getWeight());
            }
        };
    }
}
