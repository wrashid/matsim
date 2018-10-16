//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.matsim.contrib.spatialDrt.firstLastAVPTRouter.linkLinkTimes;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.spatialDrt.firstLastAVPTRouter.TransitRouterFirstLastAVPT;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class LinkLinkTimeCalculatorAV implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, LinkEnterEventHandler, Provider<LinkLinkTime> {
    private final Map<Id<Link>, Map<Id<Link>, LinkLinkTimeData>> linkLinkTimes;
    private final Map<Id<Link>, Map<Id<Link>, Double>> scheduledLinkLinkTimes;
    private final Map<Id<Vehicle>, Id<Link>> prevLinks;
    private final Map<Id<Vehicle>, Id<Link>> departureLinks;
    private final Map<Id<Vehicle>, Double> departureTimes;
    private final Map<Id<Person>, Id<Link>> avPersons;
    private double timeSlot;

    @Inject
    public LinkLinkTimeCalculatorAV(Network network, Config config, EventsManager eventsManager) {
        this(network, config.travelTimeCalculator().getTraveltimeBinSize(), (int)(config.qsim().getEndTime() - config.qsim().getStartTime()));
        eventsManager.addHandler(this);
    }

    public LinkLinkTimeCalculatorAV(Network network, int timeSlot, int totalTime) {
        this.linkLinkTimes = new HashMap(5000);
        this.scheduledLinkLinkTimes = new HashMap(5000);
        for(Link linkA : network.getLinks().values()) {
            Map<Id<Link>, Double> sMap = new HashMap<>();
            scheduledLinkLinkTimes.put(linkA.getId(), sMap);
            Map<Id<Link>, LinkLinkTimeData> map = new HashMap<>();
            linkLinkTimes.put(linkA.getId(), map);
            for (Link linkB : network.getLinks().values())
                if (linkA != linkB) {
                    double distance = CoordUtils.calcEuclideanDistance(linkA.getCoord(), linkB.getCoord());
                    sMap.put(linkB.getId(), distance/(20.0/3.6));
                    map.put(linkB.getId(), new LinkLinkTimeDataArray(totalTime / timeSlot + 1));
                }
        }
        this.prevLinks = new HashMap<>(1000);
        this.departureLinks = new HashMap<>(1000);
        this.departureTimes = new HashMap<>(1000);
        this.avPersons = new HashMap<>();
        this.timeSlot = (double)timeSlot;
    }

    private double getLinkLinkTime(Id<Link> linkOId, Id<Link> linkDId, double time) {
        if(linkOId==linkDId)
            return 0;
        LinkLinkTimeData linkLinkTimeData = this.linkLinkTimes.get(linkOId).get(linkDId);
        return linkLinkTimeData.getNumData((int)(time / this.timeSlot)) == 0 ? ((Double)((Map)this.scheduledLinkLinkTimes.get(linkOId)).get(linkDId)).doubleValue() : linkLinkTimeData.getLinkLinkTime((int)(time / this.timeSlot));
    }

    private double getLinkLinkTimeVariance(Id<Link> linkOId, Id<Link> linkDId, double time) {
        if(linkOId==linkDId)
            return 0;
        LinkLinkTimeData linkLinkTimeData = (LinkLinkTimeData)((Map)this.linkLinkTimes.get(linkOId)).get(linkDId);
        return linkLinkTimeData.getNumData((int)(time / this.timeSlot)) == 0 ? 0.0D : linkLinkTimeData.getLinkLinkTimeVariance((int)(time / this.timeSlot));
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        if(event.getLegMode().equals(TransitRouterFirstLastAVPT.AV_TAXI_MODE))
            avPersons.put(event.getPersonId(), event.getLinkId());
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        Id<Link> linkId = avPersons.remove(event.getPersonId());
        if(linkId!=null) {
            departureTimes.put(event.getVehicleId(), event.getTime());
            departureLinks.put(event.getVehicleId(), linkId);
        }
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        Id<Link> departureLink = departureLinks.get(event.getVehicleId());
        if(departureLink!=null && avPersons.get(event.getPersonId())!=null) {
            Double time = departureTimes.remove(event.getVehicleId());
            linkLinkTimes.get(departureLink).get(prevLinks.get(event.getVehicleId())).addLinkLinkTime((int)(time/this.timeSlot), event.getTime()-time);
            prevLinks.remove(event.getVehicleId());
        }
    }

    @Override
    public void handleEvent(LinkEnterEvent event) {
        if(departureLinks.get(event.getVehicleId())!=null)
            prevLinks.put(event.getVehicleId(), event.getLinkId());
    }

    public void reset(int iteration) {
        for(Map<Id<Link>, LinkLinkTimeData> map:this.linkLinkTimes.values())
            for(LinkLinkTimeData linkLinkTimeData:map.values())
                linkLinkTimeData.resetLinkLinkTimes();
        this.departureTimes.clear();
        this.departureLinks.clear();
        this.prevLinks.clear();
    }

    public LinkLinkTime get() {
        return new LinkLinkTime() {
            private static final long serialVersionUID = 1L;

            public double getLinkLinkTime(Id<Link> linkOId, Id<Link> linkDId, double time) {
                return LinkLinkTimeCalculatorAV.this.getLinkLinkTime(linkOId, linkDId, time);
            }

            public double getLinkLinkTimeVariance(Id<Link> linkOId, Id<Link> linkDId, double time) {
                return LinkLinkTimeCalculatorAV.this.getLinkLinkTimeVariance(linkOId, linkDId, time);
            }
        };
    }
}
