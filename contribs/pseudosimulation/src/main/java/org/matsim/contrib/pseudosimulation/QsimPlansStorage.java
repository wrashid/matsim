package org.matsim.contrib.pseudosimulation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QsimPlansStorage implements BeforeMobsimListener {

	private final MobSimSwitcher mobSimSwitcher;
	private final Scenario scenario;
	private Map<Id<Person>,List<Plan>> plansCollection = new HashMap<>();

	@Inject
	QsimPlansStorage(MobSimSwitcher mobSimSwitcher, Scenario scenario){
		this.mobSimSwitcher = mobSimSwitcher;
		this.scenario = scenario;
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		for (Person person : scenario.getPopulation().getPersons().values()) {

		}

	}
}
