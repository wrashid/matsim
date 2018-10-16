package org.matsim.contrib.spatialDrt.dwelling;

import org.matsim.contrib.spatialDrt.schedule.AtodRequest;
import org.apache.log4j.Logger;
import org.matsim.contrib.drt.data.DrtRequest;

import java.util.List;

public class DrtStopHandler {
    private final static Logger log = Logger.getLogger(org.matsim.core.mobsim.qsim.pt.ComplexTransitStopHandler.class);

    private boolean doorsOpen = false;
    private double passengersLeavingTimeFraction = 0.0;
    private double passengersEnteringTimeFraction = 0.0;

    private final double personEntersTime;
    private final double personLeavesTime;

    // TODO make it dynamic
    private static final double openDoorsDuration = 3.0;
    private static final double closeDoorsDuration = 3.0;


    public DrtStopHandler(double accessTime, double egressTime){
        personEntersTime = accessTime;
        personLeavesTime = egressTime;
    }

    public double handleDrtTransitStop(double now, List<DrtRequest> dropoffRequests, List<DrtRequest> pickupRequests,
                                       DrtPassengerAccessEgress handler){
        double stopTime = 0.0;

        int cntEgress = dropoffRequests.size();
        int cntAccess = pickupRequests.size();

        if (!this.doorsOpen) {
            // doors are closed

            if ((cntAccess > 0) || (cntEgress > 0)) {
                // case doors are shut, but passengers want to leave or enter
                // the veh
                this.doorsOpen = true;
                stopTime = openDoorsDuration; // Time to open doors
            } else {
                // case nobody wants to leave or enter the veh
                stopTime = 0.0;
            }

        } else {
            // doors are already open

            if ((cntAccess > 0) || (cntEgress > 0)) {
                // somebody wants to leave or enter the veh

                if (cntEgress > 0) {

                    if (this.passengersLeavingTimeFraction < 1.0) {
                        // next passenger can leave the veh

                        while (this.passengersLeavingTimeFraction < 1.0) {
                            if (dropoffRequests.size() == 0) {
                                break;
                            }

                            if(handler.handlePassengerLeaving(dropoffRequests.get(0), now)){
                                dropoffRequests.remove(0);
                                this.passengersLeavingTimeFraction += personLeavesTime;
                            } else {
                                break;
                            }

                        }

                        this.passengersLeavingTimeFraction -= 1.0;
                        stopTime = 1.0;

                    } else {
                        // still time needed to allow next passenger to leave
                        this.passengersLeavingTimeFraction -= 1.0;
                        stopTime = 1.0;
                    }

                } else {
                    this.passengersLeavingTimeFraction -= 1.0;
                    this.passengersLeavingTimeFraction = Math.max(0, this.passengersLeavingTimeFraction);

                    if (cntAccess > 0) {

                        if (this.passengersEnteringTimeFraction < 1.0) {

                            // next passenger can enter the veh

                            while (this.passengersEnteringTimeFraction < 1.0) {
                                if (pickupRequests.size() == 0) {
                                    break;
                                }

                                if(handler.handlePassengerEntering(pickupRequests.get(0), now)){
                                    pickupRequests.remove(0);
                                    this.passengersEnteringTimeFraction += personEntersTime;
                                } else {
                                    break;
                                }

                            }

                            this.passengersEnteringTimeFraction -= 1.0;
                            stopTime = 1.0;

                        } else {
                            // still time needed to allow next passenger to enter
                            this.passengersEnteringTimeFraction -= 1.0;
                            stopTime = 1.0;
                        }

                    } else {
                        this.passengersEnteringTimeFraction -= 1.0;
                        this.passengersEnteringTimeFraction = Math.max(0, this.passengersEnteringTimeFraction);
                    }
                }

            } else {

                // nobody left to handle

                if (this.passengersEnteringTimeFraction < 1.0 && this.passengersLeavingTimeFraction < 1.0) {
                    // every passenger entered or left the veh so close and
                    // leave

                    this.doorsOpen = false;
                    this.passengersEnteringTimeFraction = 0.0;
                    this.passengersLeavingTimeFraction = 0.0;
                    stopTime = closeDoorsDuration; // Time to shut the doors
                }

                // somebody is still leaving or entering the veh so wait again

                if (this.passengersEnteringTimeFraction >= 1) {
                    this.passengersEnteringTimeFraction -= 1.0;
                    stopTime = 1.0;
                }

                if (this.passengersLeavingTimeFraction >= 1) {
                    this.passengersLeavingTimeFraction -= 1.0;
                    stopTime = 1.0;
                }

            }

        }

        return stopTime;
    }
}
