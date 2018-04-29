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
package org.matsim.contrib.pseudosimulation.searchacceleration.tryout;

import javax.inject.Singleton;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.pseudosimulation.mobsim.PSim;
import org.matsim.contrib.pseudosimulation.replanning.PlanCatcher;
import org.matsim.contrib.pseudosimulation.searchacceleration.LinkUsageListener;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.IterationStartsListener;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.scenario.ScenarioUtils;

import com.google.inject.Inject;

import floetteroed.utilities.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
@Singleton
public class SearchAccelerator implements IterationStartsListener {

	// -------------------- MEMBERS --------------------

	@Inject
	MatsimServices services;

	@Inject
	Population population;

	@Inject
	ReplanningContext replanningContext;

	@Inject
	Scenario scenario;

	@Inject
	Config config;

	@Inject
	Network network;

	// -------------------- CONSTRUCTION --------------------

	// -------------------- INTERNALS --------------------

	public static void log(String line, boolean terminate) {
		System.out.println(line);
		if (terminate) {
			System.exit(0);
		}
	}

	// --------------- IMPLEMENTATION OF IterationStartsListener ---------------

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {

		log("iteration starts", false);

		log("services = " + services, false);
		log("population = " + population, false);
		log("replanningContext = " + replanningContext, false);
		log("strategyManager = " + services.getStrategyManager(), false);
		log("scenario = " + scenario, false);
		log("config = " + config, false);
		log("network = " + network, false);

		log("starting to replan", false);
		PopulationState popState = new PopulationState(this.population); // memorize
		this.services.getStrategyManager().run(this.population, this.replanningContext);
		popState.set(this.population); // undo the re-planning
		log("replanning done", false);

		log("preparing the plan catcher", false);
		final PlanCatcher planCatcher = new PlanCatcher(); // or injection
		for (Person person : this.population.getPersons().values()) {
			planCatcher.addPlansForPsim(person.getSelectedPlan());
		}
		log("plan catcher ready", false);

		LinkUsageListener listener = new LinkUsageListener(new TimeDiscretization(0, 3600, 24));
		EventsManager eventsManager = new EventsManagerImpl();
		eventsManager.addHandler(listener);

		log("running the psim", false);
		PSim pSim = new PSim(this.scenario, eventsManager, planCatcher.getPlansForPSim(),
				this.services.getLinkTravelTimes());
		pSim.run();
		log("psim done", false);

		log("so far so good", true);
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {
		System.out.println("STARTED..");

		Config config = ConfigUtils
				.loadConfig("C:/Nobackup/Profilen/git-2018/matsim-code-examples/scenarios/equil/config.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);

		StrategySettings stratSets = new StrategySettings();
		stratSets.setStrategyName(CloneHypotheticalReplanningStrategy.NAME);
		stratSets.setWeight(1e3);
		config.strategy().addStrategySettings(stratSets);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);

		controler.addOverridingModule(new SearchAcceleratorModule());

		controler.run();

		System.out.println(".. DONE.");
	}
}
