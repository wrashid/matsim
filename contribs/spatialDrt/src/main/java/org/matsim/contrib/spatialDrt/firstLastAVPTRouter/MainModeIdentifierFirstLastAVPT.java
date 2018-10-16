package org.matsim.contrib.spatialDrt.firstLastAVPTRouter;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.router.MainModeIdentifier;

import java.util.List;
import java.util.Set;


public final class MainModeIdentifierFirstLastAVPT implements MainModeIdentifier {
    private Set<String> mainModes;
    public MainModeIdentifierFirstLastAVPT(Set<String> mainModes) {
        this.mainModes = mainModes;
    }

    public String identifyMainMode(List<? extends PlanElement> tripElements) {
        for(PlanElement planElement:tripElements)
            if(planElement instanceof Leg && mainModes.contains(((Leg)planElement).getMode()))
                return ((Leg)planElement).getMode();
        return "pt";
    }
}
