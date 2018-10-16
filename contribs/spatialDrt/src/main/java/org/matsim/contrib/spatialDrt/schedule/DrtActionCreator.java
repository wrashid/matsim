/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.spatialDrt.schedule;


import com.google.inject.Inject;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.run.Drt;
import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizerWithOnlineTracking;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.vrpagent.VrpActivity;

import org.matsim.contrib.dvrp.vrpagent.VrpLegFactory;
import org.matsim.contrib.dynagent.DynAction;
import org.matsim.contrib.spatialDrt.dwelling.BusStopActivity;
import org.matsim.contrib.spatialDrt.dynAgent.DynAgent;
import org.matsim.contrib.spatialDrt.dynAgent.VrpAgentLogic;
import org.matsim.contrib.spatialDrt.passenger.PassengerEngine;
import org.matsim.contrib.spatialDrt.scheduler.DrtScheduleTimingUpdater;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.QSim;

/**
 * @author michalm
 */
public class DrtActionCreator implements VrpAgentLogic.DynActionCreator {
	public static final String DRT_STAY_NAME = "DrtStay";
	public final static String DRT_STOP_NAME = "DrtBusStop";
	public static final String DRT_QUEUE_NAME = "DrtQueue";
	private final DrtScheduleTimingUpdater drtScheduler;
	private final PassengerEngine passengerEngine;
	private final VrpLegFactory legFactory;

	@Inject
	public DrtActionCreator(@Drt PassengerEngine passengerEngine, DrtOptimizer optimizer, DrtScheduleTimingUpdater drtScheduler, MobsimTimer timer, DvrpConfigGroup dvrpCfg) {
		this(passengerEngine, v -> VrpLegFactory.createWithOnlineTracker(dvrpCfg.getMobsimMode(), v, optimizer, timer),drtScheduler);
	}

	public DrtActionCreator(PassengerEngine passengerEngine, VrpLegFactory legFactory, DrtScheduleTimingUpdater drtScheduler) {
		this.passengerEngine = passengerEngine;
		this.legFactory = legFactory;
		this.drtScheduler = drtScheduler;
	}

	@Override
	public DynAction createAction(DynAgent dynAgent, Vehicle vehicle, double now) {
		DrtTask task = (DrtTask)vehicle.getSchedule().getCurrentTask();
		switch (task.getDrtTaskType()) {
			case DRIVE:
				return legFactory.create(vehicle);

			case STOP:
				DrtStopTask t = (DrtStopTask) task;
				return new BusStopActivity(passengerEngine, dynAgent, t, t.getDrtDropoffRequests(), t.getDrtPickupRequests(),
						DRT_STOP_NAME, drtScheduler, vehicle, ((VehicleImpl) vehicle).getVehicleType().getAccessTime(), ((VehicleImpl) vehicle).getVehicleType().getEgressTime(), now);

			case STAY:
				if (task instanceof DrtQueueTask) {
					return new VrpActivity(DRT_QUEUE_NAME, (StayTask) task);
				}
				return new VrpActivity(DRT_STAY_NAME, (StayTask) task);
			default:
				throw new IllegalStateException();
		}
	}
}
