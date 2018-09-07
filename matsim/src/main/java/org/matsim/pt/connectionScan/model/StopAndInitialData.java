package org.matsim.pt.connectionScan.model;

import edu.kit.ifv.mobitopp.publictransport.model.Stop;

public class StopAndInitialData {

    private Stop stop;

    private double initialDistance;
    private double initialTime;

    public StopAndInitialData(Stop stop) {
        this.stop = stop;
    }

    public Stop getStop() {
        return stop;
    }

    public double getInitialDistance() {
        return initialDistance;
    }

    public void setInitialDistance(double initialDistance) {
        this.initialDistance = initialDistance;
    }

    public double getInitialTime() {
        return initialTime;
    }

    public void setInitialTime(double initialTime) {
        this.initialTime = initialTime;
    }
}
