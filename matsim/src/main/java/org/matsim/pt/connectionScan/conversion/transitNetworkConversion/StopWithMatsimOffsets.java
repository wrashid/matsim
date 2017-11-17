package org.matsim.pt.connectionScan.conversion.transitNetworkConversion;

import edu.kit.ifv.mobitopp.publictransport.model.Stop;

public class StopWithMatsimOffsets {

    private Stop stop;
    private double arrivalOffset;
    private double departureOffset;

    private StopWithMatsimOffsets(Stop stop, double arrivalOffset, double departureOffset) {

        this.stop = stop;
        this.arrivalOffset = arrivalOffset;
        this.departureOffset = departureOffset;
    }

    public static StopWithMatsimOffsets from(Stop stop, double arrivalOffset, double departureOffset) {

        if (hasInvalidValue(arrivalOffset)) arrivalOffset = departureOffset;
        if (hasInvalidValue(departureOffset)) departureOffset = arrivalOffset;

        return new StopWithMatsimOffsets(stop, arrivalOffset, departureOffset);
    }

    private static boolean hasInvalidValue(double offset) {

        return Double.isNaN(offset) || Double.isInfinite(offset);
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
