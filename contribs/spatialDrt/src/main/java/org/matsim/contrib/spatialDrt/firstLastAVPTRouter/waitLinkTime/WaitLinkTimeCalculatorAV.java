package org.matsim.contrib.spatialDrt.firstLastAVPTRouter.waitLinkTime;
//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonStuckEvent;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.spatialDrt.firstLastAVPTRouter.TransitRouterFirstLastAVPT;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class WaitLinkTimeCalculatorAV implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, Provider<WaitLinkTime> {

    private final double timeSlot;
    private final Map<Id<Link>, WaitLinkTimeData> waitLinkTimes;
    private final Map<Id<Link>, double[]> scheduledWaitLinkTimes;
    private final Map<Id<Person>, Double> agentsWaitingData;
    private final Map<Id<Person>, Integer> agentsCurrentLeg;
    private final Map<Id<Person>, Id<Link>> avPersons;
    private final Population population;

    @Inject
    public WaitLinkTimeCalculatorAV(Population population, Network network, Config config, EventsManager eventsManager) {
        this(population, network, config.travelTimeCalculator().getTraveltimeBinSize(), (int)(config.qsim().getEndTime() - config.qsim().getStartTime()));
        eventsManager.addHandler(this);
    }

    public WaitLinkTimeCalculatorAV(Population population, Network network, int timeSlot, int totalTime) {
        this.waitLinkTimes = new HashMap(1000);
        this.scheduledWaitLinkTimes = new HashMap(1000);
        this.agentsWaitingData = new HashMap();
        this.agentsCurrentLeg = new HashMap();
        this.avPersons = new HashMap();
        this.population = population;
        this.timeSlot = (double)timeSlot;
        for(Link link:network.getLinks().values()) {
            waitLinkTimes.put(link.getId(), new WaitLinkTimeDataArray(totalTime / timeSlot + 1));
            double[] cacheWaitLinkTimes = new double[totalTime / timeSlot + 1];
            for(int i = 0; i < cacheWaitLinkTimes.length; ++i)
                cacheWaitLinkTimes[i] = 5*60;
            scheduledWaitLinkTimes.put(link.getId(), cacheWaitLinkTimes);
        }
    }

    private double getRouteStopWaitLinkTime(Id<Link> linkId, double time) {
        WaitLinkTimeData waitTimeData = this.waitLinkTimes.get(linkId);
        if (waitTimeData.getNumData((int)(time/this.timeSlot))==0) {
            double[] waitTimes = this.scheduledWaitLinkTimes.get(linkId);
            return waitTimes[(int)(time/this.timeSlot)<waitTimes.length ? (int)(time/this.timeSlot) : waitTimes.length-1];
        }
        else
            return waitTimeData.getWaitLinkTime((int)(time/this.timeSlot));
    }

    @Override
    public void handleEvent(PersonDepartureEvent event) {
        Integer currentLeg = this.agentsCurrentLeg.get(event.getPersonId());
        if (currentLeg == null)
            currentLeg = Integer.valueOf(0);
        else
            currentLeg = currentLeg.intValue() + 1;
        this.agentsCurrentLeg.put(event.getPersonId(), currentLeg);
        if(event.getLegMode().equals(TransitRouterFirstLastAVPT.AV_TAXI_MODE)) {
            avPersons.put(event.getPersonId(), event.getLinkId());
            agentsWaitingData.put(event.getPersonId(), event.getTime());
        }
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        Id<Link> linkId = avPersons.remove(event.getPersonId());
        Double startWaitingTime = this.agentsWaitingData.remove(event.getPersonId());
        if(linkId!=null && startWaitingTime!=null) {
            WaitLinkTimeData data = this.waitLinkTimes.get(linkId);
            data.addWaitLinkTime((int)(startWaitingTime.doubleValue() / this.timeSlot), event.getTime() - startWaitingTime.doubleValue());
        }
    }

    public void handleEvent(PersonStuckEvent event) {
        Id<Link> linkId = avPersons.remove(event.getPersonId());
        Double startWaitingTime = this.agentsWaitingData.get(event.getPersonId());
        if(linkId!=null && startWaitingTime != null) {
            WaitLinkTimeData data = this.waitLinkTimes.get(linkId);
            data.addWaitLinkTime((int)(startWaitingTime.doubleValue() / this.timeSlot), event.getTime() - startWaitingTime.doubleValue());
        }

    }

    public void reset(int iteration) {
        for(WaitLinkTimeData waitTimeData: waitLinkTimes.values())
            waitTimeData.resetWaitLinkTimes();
        this.agentsWaitingData.clear();
        this.agentsCurrentLeg.clear();
    }

    public WaitLinkTime get() {
        return new WaitLinkTime() {
            @Override
            public double getWaitLinkTime(Id<Link> linkId, double time) {
                return WaitLinkTimeCalculatorAV.this.getRouteStopWaitLinkTime(linkId, time);
            }
        };
    }
}
