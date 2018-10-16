package org.matsim.contrib.drt.optimizer.insertion;

import org.matsim.contrib.drt.data.DrtRequest;
import org.matsim.contrib.drt.optimizer.VehicleData;

public interface InsertionCostCalculator {
    public static final double INFEASIBLE_SOLUTION_COST = Double.MAX_VALUE / 2;
    public interface PenaltyCalculator {
        double calcPenalty(double maxWaitTimeViolation, double maxTravelTimeViolation);
    }

    public static class RejectSoftConstraintViolations implements PenaltyCalculator {
        @Override
        public double calcPenalty(double maxWaitTimeViolation, double maxTravelTimeViolation) {
            return maxWaitTimeViolation > 0 || maxTravelTimeViolation > 0 ? INFEASIBLE_SOLUTION_COST : 0;
        }
    }

    public static class DiscourageSoftConstraintViolations implements PenaltyCalculator {
        //XXX try to keep penalties reasonably high to prevent people waiting or travelling for hours
        //XXX however, at the same time prefer max-wait-time to max-travel-time violations
        private static double MAX_WAIT_TIME_VIOLATION_PENALTY = 1;// 1 second of penalty per 1 second of late departure
        private static double MAX_TRAVEL_TIME_VIOLATION_PENALTY = 10;// 10 seconds of penalty per 1 second of late arrival

        @Override
        public double calcPenalty(double maxWaitTimeViolation, double maxTravelTimeViolation) {
            return MAX_WAIT_TIME_VIOLATION_PENALTY * maxWaitTimeViolation
                    + MAX_TRAVEL_TIME_VIOLATION_PENALTY * maxTravelTimeViolation;
        }
    }
    double calculate(DrtRequest drtRequest, VehicleData.Entry vEntry, InsertionWithDetourTimes insertion);
}
