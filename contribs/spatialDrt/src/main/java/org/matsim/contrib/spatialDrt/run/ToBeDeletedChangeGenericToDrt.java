package org.matsim.contrib.spatialDrt.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.algorithms.PersonAlgorithm;
import org.matsim.core.population.io.StreamingPopulationReader;
import org.matsim.core.population.io.StreamingPopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

public class ToBeDeletedChangeGenericToDrt {
    public static void main(String[] args) {
        Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
        StreamingPopulationReader reader = new StreamingPopulationReader(scenario);
        reader.addAlgorithm(new PersonAlgorithm() {
            @Override
            public void run(Person person) {
                    for (Plan plan : person.getPlans()) {
                        for (PlanElement planElement : plan.getPlanElements()) {
                            if (planElement instanceof Leg && ((Leg) planElement).getMode().equals("drt")){
                                Leg leg = (Leg) planElement;
                                leg.setRoute(new DrtRoute(leg.getRoute().getStartLinkId(),leg.getRoute().getEndLinkId()));
                            }
                        }
                    }
            }
        });
        StreamingPopulationWriter writer = new StreamingPopulationWriter();
        writer.startStreaming("/home/biyu/IdeaProjects/matsim-spatialDRT/contribs/spatialDrt/src/main/resouces/mp_c_tp/0.plans_new.xml");
        reader.addAlgorithm(writer);
        reader.readFile("/home/biyu/IdeaProjects/matsim-spatialDRT/contribs/spatialDrt/src/main/resouces/mp_c_tp/0.plans.xml");
        writer.closeStreaming();
    }
}
