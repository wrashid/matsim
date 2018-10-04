package org.matsim.pt.connectionScan.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

public class TransitScheduleAnalyzer {

    public static void main(String[] args) {

//        String file = "C:\\Users\\gthunig\\VSP\\matsim\\matsim\\matsim-berlin\\scenarios\\berlin-v5.1-1pct\\b5_1\\b5_1.output_transitSchedule.xml.gz";
        String file = "C:\\Users\\gthunig\\Desktop\\Vsp\\ConnectionScan\\Scenarien\\Cottbus\\cb04\\output_transitSchedule.xml.gz";

        Config config = ConfigUtils.createConfig();
        config.transit().setTransitScheduleFile(file);
        Scenario scenario = ScenarioUtils.loadScenario(config);

        TransitSchedule transitSchedule = scenario.getTransitSchedule();

        System.out.println(transitSchedule.getTransitLines().size());
        System.out.println(transitSchedule.getFacilities().size());
    }

}
