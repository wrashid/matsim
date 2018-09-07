package org.matsim.pt.connectionScan.model;

import edu.kit.ifv.mobitopp.time.SimpleTime;
import edu.kit.ifv.mobitopp.time.Time;

public class Day {

    public static Time getDay() {

//        //Human time (GMT): Friday, 10. March 2017 00:00:00
//        //Human time (your time zone): Freitag, 10. März 2017 01:00:00 GMT+01:00
//        //but somehow ends up to be some Th, 14 00:00:00
//        return SimpleTime.ofSeconds(1489104000);
        return new SimpleTime();
    }
}
