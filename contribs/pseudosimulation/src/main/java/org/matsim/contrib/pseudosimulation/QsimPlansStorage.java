package org.matsim.contrib.pseudosimulation;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.events.IterationStartsEvent;
import org.matsim.core.controler.listener.BeforeMobsimListener;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.controler.listener.IterationStartsListener;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QsimPlansStorage implements IterationEndsListener,IterationStartsListener {

	private final MobSimSwitcher mobSimSwitcher;
	private final Scenario scenario;
	private Map<Id<Person>,List<Plan>> plansCollection;

	@Inject
	QsimPlansStorage(MobSimSwitcher mobSimSwitcher, Scenario scenario){
		this.mobSimSwitcher = mobSimSwitcher;
		this.scenario = scenario;
	}

	/*
	at the end of a qsim iteration, write the population to cold storage
	 */
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {
		if (!mobSimSwitcher.isQSimIteration())
			return;
		plansCollection = new HashMap<>();
		for (Person person : scenario.getPopulation().getPersons().values()) {
			ArrayList<Plan> plans = new ArrayList<>();
			plans.addAll(person.getPlans());
			plansCollection.put(person.getId(), plans);
		}
	}

	@Override
	public void notifyIterationStarts(IterationStartsEvent event) {
		if (!mobSimSwitcher.isQSimIteration())
			return;
		for (Person person : scenario.getPopulation().getPersons().values()) {
			person.getPlans().clear();
			for (Plan plan : plansCollection.get(person.getId())) {
				person.addPlan(plan);
			}
		}
	}
}
