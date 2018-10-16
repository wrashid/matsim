/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.spatialDrt.scheduler;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithPathData;
import org.matsim.contrib.drt.run.Drt;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.dvrp.data.Fleet;
import org.matsim.contrib.dvrp.data.Vehicle;
import org.matsim.contrib.dvrp.data.Vehicles;
import org.matsim.contrib.dvrp.path.VrpPathWithTravelData;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.StayTask;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.dvrp.tracker.OnlineDriveTaskTracker;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.contrib.dvrp.util.LinkTimePair;
import org.matsim.contrib.spatialDrt.schedule.DrtQueueTask;
import org.matsim.contrib.spatialDrt.schedule.DrtStopTask;
import org.matsim.contrib.spatialDrt.schedule.VehicleImpl;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.router.util.TravelTime;

import java.util.List;

/**
 * @author michalm
 */
public class RequestInsertionScheduler {
	private final Fleet fleet;
	private final MobsimTimer timer;
	private final TravelTime travelTime;
	private final DrtScheduleTimingUpdater scheduleTimingUpdater;
	private final DrtTaskFactory taskFactory;
	@Inject
	private ModifyLanes modifyLanes;

	@Inject
	public RequestInsertionScheduler(@Drt Fleet fleet, MobsimTimer timer,
                                     @Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime,
									 DrtScheduleTimingUpdater scheduleTimingUpdater, DrtTaskFactory taskFactory) {
		this.fleet = fleet;
		this.timer = timer;
		this.travelTime = travelTime;
		this.scheduleTimingUpdater = scheduleTimingUpdater;
		this.taskFactory = taskFactory;
	}

	public void initSchedules(boolean changeStartLinkToLastLinkInSchedule) {
		fleet.resetSchedules();
		for (Vehicle veh : fleet.getVehicles().values()) {
			if (changeStartLinkToLastLinkInSchedule) {
				Vehicles.changeStartLinkToLastLinkInSchedule(veh);
			}
			veh.getSchedule()
					.addTask(taskFactory.createStayTask(veh, veh.getServiceBeginTime(), veh.getServiceEndTime(),
							veh.getStartLink()));
		}
	}

	public void scheduleRequest(VehicleData.Entry vehicleEntry, DrtRequest request, InsertionWithPathData insertion) {
		insertPickup(vehicleEntry, request, insertion);
		insertDropoff(vehicleEntry, request, insertion);
	}

	public void insertPickup(VehicleData.Entry vehicleEntry, DrtRequest request, InsertionWithPathData insertion) {
		double stopDuration = vehicleEntry.vehicle.getCapacity() * (((VehicleImpl)vehicleEntry.vehicle).getVehicleType().getAccessTime() + ((VehicleImpl)vehicleEntry.vehicle).getVehicleType().getEgressTime());

		Schedule schedule = vehicleEntry.vehicle.getSchedule();
		List<VehicleData.Stop> stops = vehicleEntry.stops;

		DrtTask currentTask = (DrtTask)schedule.getCurrentTask();
		if (currentTask instanceof DrtQueueTask){
			currentTask = (DrtTask)schedule.getTasks().get(schedule.getCurrentTask().getTaskIdx() + 1);
		}
		Task beforePickupTask;

		if (insertion.getPickupIdx() == 0 && currentTask.getDrtTaskType() == DrtTask.DrtTaskType.DRIVE) {
			LinkTimePair diversion = ((OnlineDriveTaskTracker)currentTask.getTaskTracker()).getDiversionPoint();
			if (diversion != null) { // divert vehicle
				beforePickupTask = currentTask;
				VrpPathWithTravelData vrpPath = VrpPaths.createPath(vehicleEntry.start.link, request.getFromLink(),
						vehicleEntry.start.time, insertion.getPathToPickup(), travelTime);
				((OnlineDriveTaskTracker)beforePickupTask.getTaskTracker()).divertPath(vrpPath);
			} else { // too late for diversion
				if (!request.getFromLink().getId().equals(vehicleEntry.start.link.getId())) { // add a new drive task
					VrpPathWithTravelData vrpPath = VrpPaths.createPath(vehicleEntry.start.link, request.getFromLink(),
							vehicleEntry.start.time, insertion.getPathToPickup(), travelTime);
					beforePickupTask = new DrtDriveTask(vrpPath);
					schedule.addTask(currentTask.getTaskIdx() + 1, beforePickupTask);
				} else { // no need for a new drive task
					beforePickupTask = currentTask;
				}
			}
		} else { // insert pickup after an existing stop/stay task
			DrtStayTask stayTask = null;
			DrtStopTask stopTask = null;
			if (insertion.getPickupIdx() == 0) {
				if (currentTask.getDrtTaskType() == DrtTask.DrtTaskType.STAY) {
					stayTask = (DrtStayTask)currentTask; // ongoing stay task
					double now = timer.getTimeOfDay();
					if (stayTask.getEndTime() > now) { // stop stay task; a new stop/drive task can be inserted now
						stayTask.setEndTime(now);
					}
				} else if (currentTask instanceof DrtStopTask){
					stopTask = (DrtStopTask)currentTask; // ongoing stop task
				}
			} else {
				stopTask = (DrtStopTask) stops.get(insertion.getPickupIdx() - 1).task; // future stop task
			}

			if (stopTask != null && request.getFromLink().getId().equals(stopTask.getLink().getId())) { // no detour; no new stop task
				// add pickup request to stop task
				stopTask.addPickupRequest(request);
				request.setPickupTask(stopTask);

				/// ADDED
				//// TODO this is copied, but has not been updated !!!!!!!!!!!!!!!
				// add drive from pickup
				if (insertion.getPickupIdx() == insertion.getDropoffIdx()) {
					// remove drive i->i+1 (if there is one)
					if (insertion.getPickupIdx() < stops.size()) {// there is at least one following stop
						DrtStopTask nextStopTask = (DrtStopTask) stops.get(insertion.getPickupIdx()).task;
						if (stopTask.getTaskIdx() + 2 != nextStopTask.getTaskIdx()) {// there must a drive task in
							// between
							throw new RuntimeException();
						}
						if (stopTask.getTaskIdx() + 2 == nextStopTask.getTaskIdx()) {// there must a drive task in
							// between
							int driveTaskIdx = stopTask.getTaskIdx() + 1;
							schedule.removeTask(schedule.getTasks().get(driveTaskIdx));
						}
					}

					Link toLink = request.getToLink(); // pickup->dropoff

					VrpPathWithTravelData vrpPath = VrpPaths.createPath(request.getFromLink(), toLink,
							stopTask.getEndTime(), insertion.getPathFromPickup(), travelTime);
					Task driveFromPickupTask = new DrtDriveTask(vrpPath);
					schedule.addTask(stopTask.getTaskIdx() + 1, driveFromPickupTask);

					// update timings
					// TODO should be enough to update the timeline only till dropoffIdx...
					scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle, stopTask.getTaskIdx() + 2,
							driveFromPickupTask.getEndTime());
					///////
				}

				return;
			} else {
				StayTask stayOrStopTask = stayTask != null ? stayTask : stopTask;

				// remove drive i->i+1 (if there is one)
				if (insertion.getPickupIdx() < stops.size()) {// there is at least one following stop
					DrtStopTask nextStopTask = (DrtStopTask) stops.get(insertion.getPickupIdx()).task;

					// check: if there is at most one drive task in between
					if (stayOrStopTask.getTaskIdx() + 2 != nextStopTask.getTaskIdx() //
							&& stayTask != null && stayTask.getTaskIdx() + 1 != nextStopTask.getTaskIdx()) {
						throw new RuntimeException();
					}
					if (stayOrStopTask.getTaskIdx() + 2 == nextStopTask.getTaskIdx()) {
						// removing the drive task that is in between
						int driveTaskIdx = stayOrStopTask.getTaskIdx() + 1;
						schedule.removeTask(schedule.getTasks().get(driveTaskIdx));
					}
				}

				if (stayTask != null && request.getFromLink().getId().equals(stayTask.getLink().getId())) {
					// the bus stays where it is
					beforePickupTask = stayTask;
				} else {// add drive task to pickup location
					// insert drive i->pickup
					VrpPathWithTravelData vrpPath = VrpPaths.createPath(stayOrStopTask.getLink(), request.getFromLink(),
							stayOrStopTask.getEndTime(), insertion.getPathToPickup(), travelTime);
					beforePickupTask = new DrtDriveTask(vrpPath);
					schedule.addTask(stayOrStopTask.getTaskIdx() + 1, beforePickupTask);
				}
			}
		}

		// insert pickup stop task
		double startTime = beforePickupTask.getEndTime();
		int taskIdx = beforePickupTask.getTaskIdx() + 1;
		DrtStopTask pickupStopTask = new DrtStopTask(startTime, startTime + stopDuration, request.getFromLink());

		schedule.addTask(taskIdx, pickupStopTask);
		pickupStopTask.addPickupRequest(request);
		request.setPickupTask(pickupStopTask);
		// add drive from pickup
		Link toLink = insertion.getPickupIdx() == insertion.getDropoffIdx() ? request.getToLink() // pickup->dropoff
				: stops.get(insertion.getPickupIdx()).task.getLink(); // pickup->i+1

		VrpPathWithTravelData vrpPath = VrpPaths.createPath(request.getFromLink(), toLink, startTime + stopDuration,
				insertion.getPathFromPickup(), travelTime);
		Task driveFromPickupTask = new DrtDriveTask(vrpPath);
		schedule.addTask(taskIdx + 1, driveFromPickupTask);

		// update timings
		// TODO should be enough to update the timeline only till dropoffIdx...
		scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle, taskIdx + 2, driveFromPickupTask.getEndTime());
	}

	public void insertDropoff(VehicleData.Entry vehicleEntry, DrtRequest request, InsertionWithPathData insertion) {
		double stopDuration = vehicleEntry.vehicle.getCapacity() * (((VehicleImpl)vehicleEntry.vehicle).getVehicleType().getAccessTime() + ((VehicleImpl)vehicleEntry.vehicle).getVehicleType().getEgressTime());
		Schedule schedule = vehicleEntry.vehicle.getSchedule();
		List<VehicleData.Stop> stops = vehicleEntry.stops;

		Task driveToDropoffTask;
		if (insertion.getPickupIdx() == insertion.getDropoffIdx()) { // no drive to dropoff
			int pickupTaskIdx = request.getPickupTask().getTaskIdx();
			driveToDropoffTask = schedule.getTasks().get(pickupTaskIdx + 1);
		} else {
			DrtStopTask stopTask = (DrtStopTask) stops.get(insertion.getDropoffIdx() - 1).task;
			if (request.getToLink().getId().equals(stopTask.getLink().getId())) { // no detour; no new stop task
				// add dropoff request to stop task
				stopTask.addDropoffRequest(request);
				request.setDropoffTask(stopTask);
				return;
			} else { // add drive task to dropoff location

				// remove drive j->j+1 (if j is not the last stop)
				if (insertion.getDropoffIdx() < stops.size()) {
					DrtStopTask nextStopTask = (DrtStopTask) stops.get(insertion.getDropoffIdx()).task;
					if (stopTask.getTaskIdx() + 2 != nextStopTask.getTaskIdx()) {
						throw new IllegalStateException();
					}
					int driveTaskIdx = stopTask.getTaskIdx() + 1;
					schedule.removeTask(schedule.getTasks().get(driveTaskIdx));
				}

				// insert drive i->dropoff
				VrpPathWithTravelData vrpPath = VrpPaths.createPath(stopTask.getLink(), request.getToLink(),
						stopTask.getEndTime(), insertion.getPathToDropoff(), travelTime);
				driveToDropoffTask = new DrtDriveTask(vrpPath);
				schedule.addTask(stopTask.getTaskIdx() + 1, driveToDropoffTask);
			}
		}

		// insert dropoff stop task
		double startTime = driveToDropoffTask.getEndTime();
		int taskIdx = driveToDropoffTask.getTaskIdx() + 1;
		DrtStopTask dropoffStopTask = new DrtStopTask(startTime, startTime + stopDuration, request.getToLink());
		schedule.addTask(taskIdx, dropoffStopTask);
		dropoffStopTask.addDropoffRequest(request);
		request.setDropoffTask(dropoffStopTask);

		// add drive from dropoff
		if (insertion.getDropoffIdx() == stops.size()) {// bus stays at dropoff
			if (taskIdx + 2 == schedule.getTaskCount()) {// remove stay task from the end of schedule,
				DrtStayTask oldStayTask = (DrtStayTask)schedule.getTasks().get(taskIdx + 1);
				schedule.removeTask(oldStayTask);
			}
			if (taskIdx + 1 == schedule.getTaskCount()) {

				schedule.addTask(new DrtStayTask(dropoffStopTask.getEndTime(), Math.max(vehicleEntry.vehicle.getServiceEndTime(), dropoffStopTask.getEndTime()),
						dropoffStopTask.getLink()));
			} else {
				throw new RuntimeException();
			}
		} else {
			Link toLink = stops.get(insertion.getDropoffIdx()).task.getLink(); // dropoff->j+1

			VrpPathWithTravelData vrpPath = VrpPaths.createPath(request.getToLink(), toLink, startTime + stopDuration,
					insertion.getPathFromDropoff(), travelTime);
			Task driveFromDropoffTask = new DrtDriveTask(vrpPath);
			schedule.addTask(taskIdx + 1, driveFromDropoffTask);

			// update timings
			scheduleTimingUpdater.updateTimingsStartingFromTaskIdx(vehicleEntry.vehicle, taskIdx + 2, driveFromDropoffTask.getEndTime());
		}
	}

    public void insertQuequingTask(Vehicle vehicle){
		Schedule schedule = vehicle.getSchedule();
		int currentTaskIdx = schedule.getCurrentTask().getTaskIdx();
		DrtStopTask nextTask = (DrtStopTask) schedule.getTasks().get(currentTaskIdx + 1);
		if (schedule.getCurrentTask() instanceof DrtQueueTask){
			schedule.getCurrentTask().setEndTime(schedule.getCurrentTask().getEndTime() + 1);
		}else{
			schedule.addTask(currentTaskIdx + 1, new DrtQueueTask(timer.getTimeOfDay(), timer.getTimeOfDay() + 1.0, nextTask.getLink()));
			modifyLanes.modifyLanes(nextTask.getLink().getId(), timer.getTimeOfDay(), -1.0);
			schedule.nextTask();
		}

	}


}
