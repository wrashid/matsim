/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration.utils;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.population.io.PopulationWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 *
 * @author Gunnar Flötteröd
 * 
 *         based on
 * 
 * @author nagel
 *
 *         because VspPlansCleaner is, as far as I understand, locked into the
 *         dependency injection framework.
 *
 */
public class RouteAndTimeCleaner {

	private static double randomTime_s() {
		return Math.random() * 3600 * 24;
	}

	public static void keepOnlySelected(final Scenario scenario) {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Plan plan = person.getSelectedPlan();
			person.getPlans().clear();
			person.addPlan(plan);
			person.setSelectedPlan(plan);
		}
	}

	public static void removeTimeAndRoute(final Scenario scenario) {

		PlansConfigGroup plansConfigGroup = scenario.getConfig().plans();

		PlansConfigGroup.ActivityDurationInterpretation actDurInterp = plansConfigGroup
				.getActivityDurationInterpretation();

		for (Person person : scenario.getPopulation().getPersons().values()) {

			Plan plan = person.getSelectedPlan();

			for (PlanElement pe : plan.getPlanElements()) {
				if (pe instanceof Activity) {
					Activity act = (Activity) pe;

					act.setStartTime(randomTime_s());
					act.setMaximumDuration(randomTime_s());
					act.setEndTime(randomTime_s());

					// act.setStartTime(Time.getUndefinedTime());
					// act.setMaximumDuration(Time.getUndefinedTime());
					// act.setEndTime(Time.getUndefinedTime());

					// if (actDurInterp ==
					// PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime) {
					//
					// // person stays at the activity either until its duration is over or until
					// its
					// // end time, whatever comes first
					// // do nothing
					//
					// } else if (actDurInterp ==
					// PlansConfigGroup.ActivityDurationInterpretation.endTimeOnly) {
					//
					// // always set duration to undefined:
					// act.setMaximumDuration(Time.UNDEFINED_TIME);
					//
					// } else if (actDurInterp ==
					// PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration) {
					//
					// // set duration to undefined if there is an activity end time:
					// if (act.getEndTime() != Time.UNDEFINED_TIME) {
					// act.setMaximumDuration(Time.UNDEFINED_TIME);
					// }
					//
					// } else {
					// throw new IllegalStateException("should not happen");
					// }
					//
					// if (plansConfigGroup.isRemovingUnneccessaryPlanAttributes()) {
					//
					// }

				} else if (pe instanceof Leg) {
					Leg leg = (Leg) pe;

					leg.setDepartureTime(randomTime_s());
					leg.setTravelTime(randomTime_s());
					leg.setRoute(null);

					// if (plansConfigGroup.isRemovingUnneccessaryPlanAttributes()) {
					// leg.setDepartureTime(Time.UNDEFINED_TIME);
					// Leg r = (leg); // given by activity end time; everything else confuses
					// r.setTravelTime(Time.UNDEFINED_TIME - r.getDepartureTime());
					// leg.setTravelTime(Time.UNDEFINED_TIME); // added apr'2015
					// }
				}
			}
		}
	}

	public static void main(String[] args) {
		//
		// System.out.println("exiting");
		// System.exit(0);

		String path = "/Users/GunnarF/NoBackup/data-workspace/searchacceleration" + "/rerun-2015-11-23a_No_Toll_large/";
		Config config = ConfigUtils.loadConfig(path + "matsim-config.xml");

		Scenario scenario = ScenarioUtils.loadScenario(config);

		keepOnlySelected(scenario);

		PopulationWriter writer = new PopulationWriter(scenario.getPopulation());
		writer.write(path + "selected-converged-plans.xml");
	}

}
