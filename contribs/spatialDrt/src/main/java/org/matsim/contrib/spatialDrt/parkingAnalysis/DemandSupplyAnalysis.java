package org.matsim.contrib.spatialDrt.parkingAnalysis;

import com.google.inject.Inject;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.drt.vrpagent.DrtActionCreator;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;

import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;


import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DemandSupplyAnalysis implements ActivityStartEventHandler, ActivityEndEventHandler, IterationEndsListener {
    private final double vehicleLength = 8.0; //TODO: Later Move to the DRTConfig or vehicle File
    private final Map<Id<Link>, Integer> supply = new HashMap<>();
    private final Map<Id<Link>, Integer> demand = new HashMap<>();
    private final Map<Id<Link>, Integer> demandCounter = new HashMap<>();
    private final Map<Id<Person>, ActivityStartEvent> startEventMap = new HashMap<>();
    private final OutputDirectoryHierarchy controlerIO;

    @Inject
    public DemandSupplyAnalysis(Network network, OutputDirectoryHierarchy controlerIO, EventsManager eventsManager){
        for (Link link : network.getLinks().values()){
            supply.put(link.getId(), (int) Math.floor(link.getLength() / vehicleLength));
            demand.put(link.getId(),0);
            demandCounter.put(link.getId(), 0);
        }
        this.controlerIO = controlerIO;
        eventsManager.addHandler(this);
    }

    @Override
    public void handleEvent(ActivityEndEvent event) {
        if (event.getActType().equals(DrtActionCreator.DRT_STAY_NAME)){
            ActivityStartEvent startEvent = startEventMap.get(event.getPersonId());
            startEventMap.remove(event.getPersonId());
            if (demandCounter.containsKey(event.getLinkId())){
                int count = demandCounter.get(event.getLinkId());
                if (event.getTime() != startEvent.getTime()) {
                    int max = demand.get(event.getLinkId());
                    demand.put(event.getLinkId(), Math.max(count, max));
                }
                count--;
                demandCounter.put(event.getLinkId(), count);
            }
        }
    }

    @Override
    public void handleEvent(ActivityStartEvent event) {
        if (event.getActType().equals(DrtActionCreator.DRT_STAY_NAME)){
            startEventMap.put(event.getPersonId(),event);
            if (demandCounter.containsKey(event.getLinkId())){
                int count = demandCounter.get(event.getLinkId());
                count++;
                demandCounter.put(event.getLinkId(), count);
            }
        }
    }

    @Override
    public void notifyIterationEnds(IterationEndsEvent event) {
        BufferedWriter bw = IOUtils.getBufferedWriter(controlerIO.getOutputPath() +"/demand_supply.csv");
        try {
            bw.write("LinkID;Demand;Supply");
            for (Id<Link> linkId : supply.keySet()){
                bw.newLine();
                bw.write(linkId + ";" + demand.get(linkId) + ";" + supply.get(linkId));
            }
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
