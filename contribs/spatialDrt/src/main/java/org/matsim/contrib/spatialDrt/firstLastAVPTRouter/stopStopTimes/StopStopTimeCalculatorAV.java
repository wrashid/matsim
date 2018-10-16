//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.matsim.contrib.spatialDrt.firstLastAVPTRouter.stopStopTimes;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.PersonEntersVehicleEvent;
import org.matsim.api.core.v01.events.PersonLeavesVehicleEvent;
import org.matsim.api.core.v01.events.handler.PersonEntersVehicleEventHandler;
import org.matsim.api.core.v01.events.handler.PersonLeavesVehicleEventHandler;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeData;
import org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTimeDataArray;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.VehicleArrivesAtFacilityEvent;
import org.matsim.core.api.experimental.events.VehicleDepartsAtFacilityEvent;
import org.matsim.core.api.experimental.events.handler.VehicleArrivesAtFacilityEventHandler;
import org.matsim.core.api.experimental.events.handler.VehicleDepartsAtFacilityEventHandler;
import org.matsim.core.config.Config;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.vehicles.Vehicle;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Singleton
public class StopStopTimeCalculatorAV implements PersonEntersVehicleEventHandler, PersonLeavesVehicleEventHandler, VehicleDepartsAtFacilityEventHandler, VehicleArrivesAtFacilityEventHandler, Provider<StopStopTime> {
    private final Map<Id<TransitStopFacility>, Map<Id<TransitStopFacility>, StopStopTimeData>> stopStopTimes;
    private final Map<Id<TransitStopFacility>, Map<Id<TransitStopFacility>, Double>> scheduledStopStopTimes;
    private final Map<Id<Vehicle>, Id<TransitStopFacility>> arrivingVehicles;
    private final Map<Id<Person>, Tuple<Id<TransitStopFacility>, Double>> inTransitPeople;
    private double timeSlot;

    @Inject
    public StopStopTimeCalculatorAV(TransitSchedule transitSchedule, Config config, EventsManager eventsManager) {
        this(transitSchedule, config.travelTimeCalculator().getTraveltimeBinSize(), (int)(config.qsim().getEndTime() - config.qsim().getStartTime()));
        eventsManager.addHandler(this);
    }

    public StopStopTimeCalculatorAV(TransitSchedule transitSchedule, int timeSlot, int totalTime) {
        this.stopStopTimes = new HashMap(5000);
        this.scheduledStopStopTimes = new HashMap(5000);
        for(TransitStopFacility stopA : transitSchedule.getFacilities().values()) {
            Map<Id<TransitStopFacility>, Double> sMap = new HashMap<>();
            scheduledStopStopTimes.put(stopA.getId(), sMap);
            Map<Id<TransitStopFacility>, StopStopTimeData> map = new HashMap<>();
            stopStopTimes.put(stopA.getId(), map);
            for (TransitStopFacility stopB : transitSchedule.getFacilities().values())
                if (stopA != stopB) {
                    double distance = CoordUtils.calcEuclideanDistance(stopA.getCoord(), stopB.getCoord());
                    sMap.put(stopB.getId(), distance/(20.0/3.6));
                    map.put(stopB.getId(), new StopStopTimeDataArray(totalTime / timeSlot + 1));
                }
        }
        this.inTransitPeople = new HashMap<>(1000);
        this.arrivingVehicles = new HashMap<>(1000);
        this.timeSlot = (double)timeSlot;
    }

    private double getStopStopTime(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time) {
        StopStopTimeData stopStopTimeData = (StopStopTimeData)((Map)this.stopStopTimes.get(stopOId)).get(stopDId);
        return stopStopTimeData.getNumData((int)(time / this.timeSlot)) == 0 ? ((Double)((Map)this.scheduledStopStopTimes.get(stopOId)).get(stopDId)).doubleValue() : stopStopTimeData.getStopStopTime((int)(time / this.timeSlot));
    }

    private double getStopStopTimeVariance(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time) {
        StopStopTimeData stopStopTimeData = (StopStopTimeData)((Map)this.stopStopTimes.get(stopOId)).get(stopDId);
        return stopStopTimeData.getNumData((int)(time / this.timeSlot)) == 0 ? 0.0D : stopStopTimeData.getStopStopTimeVariance((int)(time / this.timeSlot));
    }

    @Override
    public void handleEvent(VehicleDepartsAtFacilityEvent event) {
        arrivingVehicles.remove(event.getVehicleId());
    }

    @Override
    public void handleEvent(VehicleArrivesAtFacilityEvent event) {
        arrivingVehicles.put(event.getVehicleId(), event.getFacilityId());
    }

    @Override
    public void handleEvent(PersonEntersVehicleEvent event) {
        Id<TransitStopFacility> id = arrivingVehicles.get(event.getVehicleId());
        if(id!=null)
            inTransitPeople.put(event.getPersonId(), new Tuple<>(id, event.getTime()));
    }

    @Override
    public void handleEvent(PersonLeavesVehicleEvent event) {
        Tuple<Id<TransitStopFacility>, Double> tuple = inTransitPeople.get(event.getPersonId());
        Id<TransitStopFacility> id = arrivingVehicles.get(event.getVehicleId());
        if(tuple!=null && id!=null) {
            Map<Id<TransitStopFacility>, StopStopTimeData> map = stopStopTimes.get(tuple.getFirst());
            if(map!=null) {
                StopStopTimeData data = map.get(id);
                if(data!=null)
                    data.addStopStopTime((int)(tuple.getSecond()/this.timeSlot), event.getTime()-tuple.getSecond());
            }
        }
    }

    public void reset(int iteration) {
        for(Map<Id<TransitStopFacility>, StopStopTimeData> map:this.stopStopTimes.values())
            for(StopStopTimeData stopStopTimeData:map.values())
                stopStopTimeData.resetStopStopTimes();
        this.inTransitPeople.clear();
        this.arrivingVehicles.clear();
    }

    public StopStopTime get() {
        return new StopStopTime() {
            private static final long serialVersionUID = 1L;

            public double getStopStopTime(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time) {
                return StopStopTimeCalculatorAV.this.getStopStopTime(stopOId, stopDId, time);
            }

            public double getStopStopTimeVariance(Id<TransitStopFacility> stopOId, Id<TransitStopFacility> stopDId, double time) {
                return StopStopTimeCalculatorAV.this.getStopStopTimeVariance(stopOId, stopDId, time);
            }
        };
    }
}
