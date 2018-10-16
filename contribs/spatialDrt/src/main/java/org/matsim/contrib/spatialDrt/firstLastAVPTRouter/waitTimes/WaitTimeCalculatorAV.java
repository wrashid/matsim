package org.matsim.contrib.spatialDrt.firstLastAVPTRouter.waitTimes;

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
import org.matsim.api.core.v01.events.handler.PersonStuckEventHandler;
import org.matsim.api.core.v01.population.*;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeData;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeDataArray;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.pt.routes.ExperimentalTransitRoute;
import org.matsim.pt.routes.ExperimentalTransitRouteFactory;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class WaitTimeCalculatorAV implements PersonDepartureEventHandler, PersonEntersVehicleEventHandler, PersonStuckEventHandler, Provider<WaitTime> {
    private final double timeSlot;
    private final Map<Id<TransitStopFacility>, WaitTimeData> waitTimes;
    private final Map<Id<TransitStopFacility>, double[]> scheduledWaitTimes;
    private final Map<Id<Person>, Double> agentsWaitingData;
    private final Map<Id<Person>, Integer> agentsCurrentLeg;
    private final Population population;

    @Inject
    public WaitTimeCalculatorAV(Population population, TransitSchedule transitSchedule, Config config, EventsManager eventsManager) {
        this(population, transitSchedule, config.travelTimeCalculator().getTraveltimeBinSize(), (int)(config.qsim().getEndTime() - config.qsim().getStartTime()));
        eventsManager.addHandler(this);
    }

    public WaitTimeCalculatorAV(Population population, TransitSchedule transitSchedule, int timeSlot, int totalTime) {
        this.waitTimes = new HashMap(1000);
        this.scheduledWaitTimes = new HashMap(1000);
        this.agentsWaitingData = new HashMap();
        this.agentsCurrentLeg = new HashMap();
        this.population = population;
        this.timeSlot = (double)timeSlot;
        for(TransitStopFacility stop: transitSchedule.getFacilities().values()) {
            waitTimes.put(stop.getId(), new WaitTimeDataArray(totalTime / timeSlot + 1));
            double[] cacheWaitTimes = new double[totalTime / timeSlot + 1];
            for(int i = 0; i < cacheWaitTimes.length; ++i)
                cacheWaitTimes[i] = 5*60;
            scheduledWaitTimes.put(stop.getId(), cacheWaitTimes);
        }
    }

    private double getRouteStopWaitTime(Id<TransitLine> lineId, Id<TransitRoute> routeId, Id<TransitStopFacility> stopId, double time) {
        WaitTimeData waitTimeData = this.waitTimes.get(stopId);
        if (waitTimeData.getNumData((int)(time/this.timeSlot))==0) {
            double[] waitTimes = this.scheduledWaitTimes.get(stopId);
            return waitTimes[(int)(time/this.timeSlot)<waitTimes.length ? (int)(time/this.timeSlot) : waitTimes.length-1];
        }
        else
            return waitTimeData.getWaitTime((int)(time/this.timeSlot));
    }

    public void reset(int iteration) {
        for(WaitTimeData waitTimeData: waitTimes.values())
            waitTimeData.resetWaitTimes();
        this.agentsWaitingData.clear();
        this.agentsCurrentLeg.clear();
    }

    public void handleEvent(PersonDepartureEvent event) {
        Integer currentLeg = (Integer)this.agentsCurrentLeg.get(event.getPersonId());
        if (currentLeg == null)
            currentLeg = Integer.valueOf(0);
        else
            currentLeg = currentLeg.intValue() + 1;
        this.agentsCurrentLeg.put(event.getPersonId(), currentLeg);
        if (event.getLegMode().equals("pt") && this.agentsWaitingData.get(event.getPersonId()) == null)
            this.agentsWaitingData.put(event.getPersonId(), event.getTime());
        else if (this.agentsWaitingData.get(event.getPersonId()) != null)
            new RuntimeException("Departing with old data");
    }

    public void handleEvent(PersonEntersVehicleEvent event) {
        Double startWaitingTime = this.agentsWaitingData.get(event.getPersonId());
        if (startWaitingTime != null) {
            int legs = 0;
            int currentLeg = this.agentsCurrentLeg.get(event.getPersonId()).intValue();
            for(PlanElement planElement: this.population.getPersons().get(event.getPersonId()).getSelectedPlan().getPlanElements())
                if (planElement instanceof Leg) {
                    if (currentLeg == legs) {
                        Route route = ((Leg)planElement).getRoute();
                        ExperimentalTransitRoute eRoute = (ExperimentalTransitRoute)(new ExperimentalTransitRouteFactory()).createRoute(route.getStartLinkId(), route.getEndLinkId());
                        eRoute.setStartLinkId(route.getStartLinkId());
                        eRoute.setEndLinkId(route.getEndLinkId());
                        eRoute.setRouteDescription(route.getRouteDescription());
                        WaitTimeData data = this.waitTimes.get(eRoute.getAccessStopId());
                        data.addWaitTime((int)(startWaitingTime.doubleValue() / this.timeSlot), event.getTime() - startWaitingTime.doubleValue());
                        this.agentsWaitingData.remove(event.getPersonId());
                        break;
                    }
                    ++legs;
                }
        }
    }

    public void handleEvent(PersonStuckEvent event) {
        Double startWaitingTime = this.agentsWaitingData.get(event.getPersonId());
        if (startWaitingTime != null) {
            int legs = 0;
            int currentLeg = this.agentsCurrentLeg.get(event.getPersonId()).intValue();
            for(PlanElement planElement: this.population.getPersons().get(event.getPersonId()).getSelectedPlan().getPlanElements())
                if (planElement instanceof Leg) {
                    if (currentLeg == legs) {
                        Route route = ((Leg)planElement).getRoute();
                        ExperimentalTransitRoute eRoute = (ExperimentalTransitRoute)(new ExperimentalTransitRouteFactory()).createRoute(route.getStartLinkId(), route.getEndLinkId());
                        eRoute.setStartLinkId(route.getStartLinkId());
                        eRoute.setEndLinkId(route.getEndLinkId());
                        eRoute.setRouteDescription(route.getRouteDescription());
                        WaitTimeData data = this.waitTimes.get(eRoute.getAccessStopId());
                        if (data != null)
                            data.addWaitTime((int)(startWaitingTime.doubleValue() / this.timeSlot), event.getTime() - startWaitingTime.doubleValue());
                        this.agentsWaitingData.remove(event.getPersonId());
                        break;
                    }
                    ++legs;
                }
        }

    }

    public WaitTime get() {
        return new WaitTime() {
            private static final long serialVersionUID = 1L;

            public double getRouteStopWaitTime(Id<TransitLine> lineId, Id<TransitRoute> routeId, Id<TransitStopFacility> stopId, double time) {
                return WaitTimeCalculatorAV.this.getRouteStopWaitTime(lineId, routeId, stopId, time);
            }
        };
    }
}
