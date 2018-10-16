package org.matsim.contrib.spatialDrt.scheduler;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.network.NetworkChangeEvent;

import javax.inject.Inject;

public class ModifyLanes {
    @Inject
    private Network network;
    @Inject
    private QSim qSim;

    public void modifyLanes(Id<Link> linkId, double time, double change){
        Link currentLink = network.getLinks().get(linkId);
        double numOfLanes = currentLink.getNumberOfLanes();
        if (numOfLanes == 1){
            change = 0.5 * change;
        }
        NetworkChangeEvent event = new NetworkChangeEvent(time + Math.random());
        event.addLink(currentLink);
        NetworkChangeEvent.ChangeValue capacityChange = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, (numOfLanes + change)/numOfLanes * currentLink.getCapacity() / 3600.0);
        NetworkChangeEvent.ChangeValue lanesChange = new NetworkChangeEvent.ChangeValue(NetworkChangeEvent.ChangeType.ABSOLUTE_IN_SI_UNITS, numOfLanes + change);
        event.setLanesChange(lanesChange);
        event.setFlowCapacityChange(capacityChange);
        qSim.addNetworkChangeEvent(event);
    }
}
