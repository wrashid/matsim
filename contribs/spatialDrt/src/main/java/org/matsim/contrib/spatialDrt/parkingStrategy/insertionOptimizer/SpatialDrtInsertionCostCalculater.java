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

package org.matsim.contrib.spatialDrt.parkingStrategy.insertionOptimizer;



import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator;
import org.matsim.contrib.drt.optimizer.insertion.InsertionWithDetourTimes;
import org.matsim.contrib.drt.optimizer.insertion.InsertionCostCalculator.PenaltyCalculator;

import org.matsim.contrib.drt.optimizer.insertion.InsertionWithPathData;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtDriveTask;
import org.matsim.contrib.drt.schedule.DrtStayTask;
import org.matsim.contrib.drt.schedule.DrtTask;
import org.matsim.contrib.dvrp.path.VrpPaths;
import org.matsim.contrib.dvrp.schedule.Schedule;
import org.matsim.contrib.dvrp.schedule.Schedules;
import org.matsim.contrib.dvrp.schedule.StayTaskImpl;
import org.matsim.contrib.dvrp.schedule.Task;
import org.matsim.contrib.spatialDrt.schedule.DrtQueueTask;
import org.matsim.contrib.spatialDrt.schedule.VehicleImpl;
import org.matsim.core.mobsim.framework.MobsimTimer;

/**
 * @author michalm
 */
public class SpatialDrtInsertionCostCalculater implements InsertionCostCalculator {

//	public interface PenaltyCalculator {
//		double calcPenalty(double maxWaitTimeViolation, double maxTravelTimeViolation);
//	}
//
//	public static class RejectSoftConstraintViolations implements PenaltyCalculator {
//		@Override
//		public double calcPenalty(double maxWaitTimeViolation, double maxTravelTimeViolation) {
//			return maxWaitTimeViolation > 0 || maxTravelTimeViolation > 0 ? INFEASIBLE_SOLUTION_COST : 0;
//		}
//	}
//
//	public static class DiscourageSoftConstraintViolations implements PenaltyCalculator {
//		//XXX try to keep penalties reasonably high to prevent people waiting or travelling for hours
//		//XXX however, at the same time prefer max-wait-time to max-travel-time violations
//		private static double MAX_WAIT_TIME_VIOLATION_PENALTY = 1;// 1 second of penalty per 1 second of late departure
//		private static double MAX_TRAVEL_TIME_VIOLATION_PENALTY = 10;// 10 seconds of penalty per 1 second of late arrival
//
//		@Override
//		public double calcPenalty(double maxWaitTimeViolation, double maxTravelTimeViolation) {
//			return MAX_WAIT_TIME_VIOLATION_PENALTY * maxWaitTimeViolation
//					+ MAX_TRAVEL_TIME_VIOLATION_PENALTY * maxTravelTimeViolation;
//		}
//	}

	private final MobsimTimer timer;
	private final PenaltyCalculator penaltyCalculator;

	public SpatialDrtInsertionCostCalculater(MobsimTimer timer, PenaltyCalculator penaltyCalculator) {
		this.timer = timer;
		this.penaltyCalculator = penaltyCalculator;
	}

	/**
	 * As the main goal is to minimise bus operation time, this method calculates how much longer the bus will operate
	 * after insertion. By returning a value equal or higher than INFEASIBLE_SOLUTION_COST, the insertion is considered
	 * infeasible
	 * <p>
	 * The insertion is invalid if some maxTravel/Wait constraints for the already scheduled requests are not fulfilled
	 * or the vehicle's time window is violated (hard constraints). This is denoted by returning INFEASIBLE_SOLUTION_COST.
	 * <p>
	 * However, not fulfilling the maxTravel/Time constraints (soft constraints) is penalised using
	 * PenaltyCalculator. If the penalty is at least as high as INFEASIBLE_SOLUTION_COST, the soft
	 * constraint becomes effectively a hard one.
	 *
	 * @return cost of insertion (values higher or equal to INFEASIBLE_SOLUTION_COST represent an infeasible insertion)
	 */

    public double calculate(DrtRequest drtRequest, VehicleData.Entry vEntry, InsertionWithDetourTimes insertion) {
        double pickupDetourTimeLoss = calculatePickupDetourTimeLoss(drtRequest, vEntry, insertion);
        double dropoffDetourTimeLoss = calculateDropoffDetourTimeLoss(drtRequest, vEntry, insertion);

        // this is what we want to minimise
        double totalTimeLoss = pickupDetourTimeLoss + dropoffDetourTimeLoss;

        boolean constraintsSatisfied = areConstraintsSatisfied(drtRequest, vEntry, insertion, pickupDetourTimeLoss,
                totalTimeLoss);
        return constraintsSatisfied ? totalTimeLoss : INFEASIBLE_SOLUTION_COST;
    }

    private double calculatePickupDetourTimeLoss(DrtRequest drtRequest, VehicleData.Entry vEntry,
                                                 InsertionWithDetourTimes insertion) {
        // 'no detour' is also possible now for pickupIdx==0 if the currentTask is STOP
        boolean ongoingStopTask = insertion.getPickupIdx() == 0
                && (((DrtTask)vEntry.vehicle.getSchedule().getCurrentTask()).getDrtTaskType() == DrtTask.DrtTaskType.STOP ||
                vEntry.vehicle.getSchedule().getCurrentTask() instanceof DrtQueueTask || vEntry.vehicle.getSchedule().getCurrentTask() instanceof DrtChargeTask);

        if ((ongoingStopTask && drtRequest.getFromLink().getId().equals(vEntry.start.link)) //
                || (insertion.getPickupIdx() > 0 //
                && drtRequest.getFromLink().getId().equals(vEntry.stops.get(insertion.getPickupIdx() - 1).task.getLink().getId()))) {
            if (insertion.getPickupIdx() != insertion.getDropoffIdx()) {// not: PICKUP->DROPOFF
                return 0;// no detour
            }

            // PICKUP->DROPOFF
            // no extra drive to pickup and stop (==> toPickupTT == 0 and stopDuration == 0)
            double fromPickupTT = insertion.getTimeFromPickup();
            double replacedDriveTT = calculateReplacedDriveDuration(vEntry, insertion.getPickupIdx());
            return fromPickupTT - replacedDriveTT;
        }

        double toPickupTT = insertion.getTimeToPickup();
        double fromPickupTT = insertion.getTimeFromPickup();
        double replacedDriveTT = insertion.getPickupIdx() == insertion.getDropoffIdx() // PICKUP->DROPOFF ?
                ? 0 // no drive following the pickup is replaced (only the one following the dropoff)
                : calculateReplacedDriveDuration(vEntry, insertion.getPickupIdx());
        return toPickupTT + vEntry.vehicle.getCapacity() * (((VehicleImpl)vEntry.vehicle).getVehicleType().getAccessTime() + ((VehicleImpl)vEntry.vehicle).getVehicleType().getEgressTime()) + fromPickupTT - replacedDriveTT;
    }

    private double calculateDropoffDetourTimeLoss(DrtRequest drtRequest, VehicleData.Entry vEntry,
                                                  InsertionWithDetourTimes insertion) {
        if (insertion.getDropoffIdx() > 0
                && drtRequest.getToLink().getId().equals(vEntry.stops.get(insertion.getDropoffIdx() - 1).task.getLink().getId())) {
            return 0; // no detour
        }

        double toDropoffTT = insertion.getDropoffIdx() == insertion.getPickupIdx() // PICKUP->DROPOFF ?
                ? 0 // PICKUP->DROPOFF taken into account as fromPickupTT
                : insertion.getTimeToDropoff();
        double fromDropoffTT = insertion.getDropoffIdx() == vEntry.stops.size() // DROPOFF->STAY ?
                ? 0 //
                : insertion.getTimeFromDropoff();
        double replacedDriveTT = insertion.getDropoffIdx() == insertion.getPickupIdx() // PICKUP->DROPOFF ?
                ? 0 // replacedDriveTT already taken into account in pickupDetourTimeLoss
                : calculateReplacedDriveDuration(vEntry, insertion.getDropoffIdx());
        return toDropoffTT + vEntry.vehicle.getCapacity() * (((VehicleImpl)vEntry.vehicle).getVehicleType().getAccessTime() + ((VehicleImpl)vEntry.vehicle).getVehicleType().getEgressTime()) + fromDropoffTT - replacedDriveTT;
    }

    private double calculateReplacedDriveDuration(VehicleData.Entry vEntry, int insertionIdx) {
        if (insertionIdx == vEntry.stops.size()) {
            return 0;// end of route - bus would wait there
        }

        double replacedDriveStartTime = getDriveToInsertionStartTime(vEntry, insertionIdx);
        double replacedDriveEndTime = vEntry.stops.get(insertionIdx).task.getBeginTime();
        return replacedDriveEndTime - replacedDriveStartTime;
    }

    private boolean areConstraintsSatisfied(DrtRequest drtRequest, VehicleData.Entry vEntry,
                                            InsertionWithDetourTimes insertion, double pickupDetourTimeLoss, double totalTimeLoss) {
        // this is what we cannot violate
        // vehicle's time window cannot be violated

        DrtStayTask lastTask = (DrtStayTask)Schedules.getLastTask(vEntry.vehicle.getSchedule());
        double timeSlack = vEntry.vehicle.getServiceEndTime() //-600
                - Math.max(lastTask.getBeginTime(), timer.getTimeOfDay());
        if (timeSlack < totalTimeLoss) {
            return false;
        }

        Task currentTask = vEntry.vehicle.getSchedule().getCurrentTask();
        if (currentTask instanceof DrtStayTask){
            return true; // idle vehicles always satisfy the contraints
        }
        Task nextTask = vEntry.vehicle.getSchedule().getTasks().get(vEntry.vehicle.getSchedule().getCurrentTask().getTaskIdx() + 1);
        if (currentTask instanceof DrtDriveTask && nextTask instanceof DrtStayTask){
            return true; // vehicles coming back to depot always satisfy the contraints
        }
        for (int s = insertion.getPickupIdx(); s < insertion.getDropoffIdx(); s++) {
            VehicleData.Stop stop = vEntry.stops.get(s);
            // all stops after pickup are delayed by pickupDetourTimeLoss
            if (stop.task.getBeginTime() + pickupDetourTimeLoss > stop.maxArrivalTime //
                    || stop.task.getEndTime() + pickupDetourTimeLoss > stop.maxDepartureTime) {
                return false;
            }
        }

        // this is what we cannot violate
        for (int s = insertion.getDropoffIdx(); s < vEntry.stops.size(); s++) {
            VehicleData.Stop stop = vEntry.stops.get(s);
            // all stops after dropoff are delayed by totalTimeLoss
            if (stop.task.getBeginTime() + totalTimeLoss > stop.maxArrivalTime //
                    || stop.task.getEndTime() + totalTimeLoss > stop.maxDepartureTime) {
                return false;
            }
        }

        // reject solutions when maxWaitTime for the new request is violated
        double driveToPickupStartTime = getDriveToInsertionStartTime(vEntry, insertion.getPickupIdx());
        double pickupEndTime = driveToPickupStartTime + insertion.getTimeToPickup() + vEntry.vehicle.getCapacity() * (((VehicleImpl)vEntry.vehicle).getVehicleType().getAccessTime() + ((VehicleImpl)vEntry.vehicle).getVehicleType().getEgressTime());

        if (pickupEndTime > drtRequest.getLatestStartTime()) {
            return false;
        }

        // reject solutions when latestArrivalTime for the new request is violated
        double dropoffStartTime = insertion.getPickupIdx() == insertion.getDropoffIdx()
                ? pickupEndTime + insertion.getTimeFromPickup()
                : vEntry.stops.get(insertion.getDropoffIdx() - 1).task.getEndTime() + pickupDetourTimeLoss
                + insertion.getTimeToDropoff();

        if (dropoffStartTime > drtRequest.getLatestArrivalTime()) {
            return false;
        }
        return true;// all constraints satisfied
    }

    private double getDriveToInsertionStartTime(VehicleData.Entry vEntry, int insertionIdx) {
        return (insertionIdx == 0) ? vEntry.start.time : vEntry.stops.get(insertionIdx - 1).task.getEndTime();
    }

}
