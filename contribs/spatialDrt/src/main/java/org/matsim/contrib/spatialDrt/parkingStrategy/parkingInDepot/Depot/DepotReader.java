package org.matsim.contrib.spatialDrt.parkingStrategy.parkingInDepot.Depot;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.data.file.ReaderUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.AbstractRoutingNetworkLink;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;
import sun.nio.ch.Net;

import java.util.Stack;

public class DepotReader extends MatsimXmlParser {

    private static final String DEPOT = "depot";
    private static final double DEFAULT_CAPACITY = 0;
    private final DepotManager depotManager;
    private final Network network;


    public DepotReader(DepotManager depotManager, Network network) {
        this.depotManager = depotManager;
        this.network = network;
    }

    @Override
    public void startTag(String name, Attributes atts, Stack<String> context) {
        if (DEPOT.equals(name)) {
            Depot depot = createDepot(atts);
            depotManager.addDepot(depot);
        }
    }

    @Override
    public void endTag(String name, String content, Stack<String> context) {
    }

    private Depot createDepot(Attributes atts) {
        Id<Depot> id = Id.create(atts.getValue("id"), Depot.class);
        Link link = network.getLinks().get(Id.createLinkId(atts.getValue("link")));
        //TODO: Create specific links for depots
        double capacity = ReaderUtils.getDouble(atts, "capacity", DEFAULT_CAPACITY);
        switch (atts.getValue("type")){
            case ("depot"):
                return new DepotImpl(id, link, capacity);
            case ("HDB"):
                return new HDBDepot(id, link, capacity);
            default:
                throw new RuntimeException("Wrong input depot type!");
        }
    }

}
