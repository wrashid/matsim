package org.matsim.pt.connectionScan.conversion;

import edu.kit.ifv.mobitopp.publictransport.model.Stop;

public class StopWithMatsimOffsets {

    private Stop stop;
    private double arrivalOffset;
    private double departureOffset;

    StopWithMatsimOffsets(Stop stop, double arrivalOffset, double departureOffset) {

        this.stop = stop;
        this.arrivalOffset = arrivalOffset;
        this.departureOffset = departureOffset;
    }

    public Stop getStop() {
        return stop;
    }

    public double getArrivalOffset() {
        return arrivalOffset;
    }

    public double getDepartureOffset() {
        return departureOffset;
    }
}
