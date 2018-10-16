package org.matsim.contrib.spatialDrt.dwelling;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.core.mobsim.framework.MobsimAgent;

import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import org.matsim.contrib.dvrp.data.Vehicle;


public interface DrtPassengerAccessEgress {
    /**
     * @param agent agent to be handled
     * @param time time the agent should be handled
     * @return true, if handled correctly, otherwise false, e.g. vehicle has no capacity left
     */
    public boolean handlePassengerEntering(final PassengerRequest request,  final double time);

    /**
     * @param agent agent to be handled
     * @param time time the agent should be handled
     * @return true, if handled correctly, otherwise false
     */
    public boolean handlePassengerLeaving(final PassengerRequest request, final double time);
}
