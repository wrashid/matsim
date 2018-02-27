package org.matsim.pt.connectionScan.model;

import edu.kit.ifv.mobitopp.publictransport.model.RelativeTime;
import edu.kit.ifv.mobitopp.publictransport.model.Station;
import edu.kit.ifv.mobitopp.publictransport.model.Stop;

import java.awt.geom.Point2D;

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
