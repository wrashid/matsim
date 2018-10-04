package org.matsim.pt.connectionScan.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.ArrayList;
import java.util.List;

public class PlansFileComperator {

    public static void main(String[] args) {
        String file1 = "C:\\Users\\gthunig\\VSP\\matsim\\matsim\\matsim\\output\\BerlinScenario\\CS2\\b5_1.output_plans.xml.gz";
        String file2 = "C:\\Users\\gthunig\\VSP\\matsim\\matsim\\matsim\\output\\BerlinScenario\\D1\\b5_1.output_plans.xml.gz";
//        String file1 = "C:\\Users\\gthunig\\VSP\\matsim\\matsim\\matsim\\output\\CottbusScenario\\CSTest\\output_plans.xml.gz";
//        String file2 = "C:\\Users\\gthunig\\VSP\\matsim\\matsim\\matsim\\output\\CottbusScenario\\D1\\output_plans.xml.gz";

        Population population1 = readPopulationFile(file1);
        Population population2 = readPopulationFile(file2);

        int counter1 = 0;
        int counter2 = 0;
        int counter3 = 0;
        int counter4 = 0;

        for (Person person1 : population1.getPersons().values()) {
            Person person2 = population2.getPersons().get(person1.getId());

            List<Double> travelTimes1 = new ArrayList<>();
            List<Integer> changes1 = new ArrayList<>();
            List<Double> travelTimes2 = new ArrayList<>();
            List<Integer> changes2 = new ArrayList<>();

            double travelTime = 0;
            int changes = 0;
            for (PlanElement planElement : person1.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    if (((Activity) planElement).getType().equals("pt interaction")) {
                        changes++;
                    } else {
                        travelTimes1.add(travelTime);
                        changes1.add(changes);
                        travelTime = 0;
                        changes = 0;
                    }
                }
                if (planElement instanceof Leg) {
                    travelTime += ((Leg) planElement).getTravelTime();
                }
            }
            for (PlanElement planElement : person2.getSelectedPlan().getPlanElements()) {
                if (planElement instanceof Activity) {
                    if (((Activity) planElement).getType().equals("pt interaction")) {
                        changes++;
                    } else {
                        travelTimes2.add(travelTime);
                        changes2.add(changes);
                        travelTime = 0;
                        changes = 0;
                    }
                }
                if (planElement instanceof Leg) {
                    travelTime += ((Leg) planElement).getTravelTime();
                }
            }

            for (int i = 0; i < travelTimes1.size(); i++) {
                if ((travelTimes1.get(i) - travelTimes2.get(i)) < -1) {
                    counter1++;
                }
                counter4++;
            }

            for (int i = 0; i < travelTimes1.size(); i++) {
                if ((travelTimes1.get(i) - travelTimes2.get(i)) > 1) {
                    counter2++;
                }
            }

            for (int i = 0; i < changes1.size(); i++) {
                if (Math.abs(changes1.get(i) - changes2.get(i)) > 0) {
                    counter3++;
                }
            }
        }
        System.out.println(counter1);
        System.out.println(counter2);
        System.out.println(counter3);
        System.out.println(counter4);

        double scoreDifference = 0;
        double highestScore = 0;
        int size = 0;
        int size2 = 0;
        for (Person person : population1.getPersons().values()) {

            double score1 = person.getSelectedPlan().getScore();
            double score2 = population2.getPersons().get(person.getId()).getSelectedPlan().getScore();
            double currentDifference = Math.abs(score1 - score2);
            if (currentDifference > 100) {
                size2++;
            } else {
                scoreDifference += currentDifference;
            }
            if (currentDifference > highestScore)
                highestScore = currentDifference;
            size++;
        }
        System.out.println(scoreDifference);
        System.out.println(size);
        System.out.println(scoreDifference / size);
        System.out.println(highestScore);
    }

    static Population readPopulationFile(String populationFile) {

        Config config = ConfigUtils.createConfig();
        config.plans().setInputFile(populationFile);
        Scenario scenario = ScenarioUtils.loadScenario(config);
        return scenario.getPopulation();
    }
}
