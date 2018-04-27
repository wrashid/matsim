package org.matsim.contrib.pseudosimulation.searchacceleration.examples.matsimdummy;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.events.handler.EventHandler;

import java.util.Map;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class DummyPSim {

	public DummyPSim() {
	}

	public Map<Id<Person>, Plan> getNewPlanForAllAgents() {
		/*
		 * TODO Run the same plan strategies as defined in the original MATSim
		 * configuration.
		 */
		return null;
	}

	public void executePlans(final Map<Id<Person>, Plan> plans, final EventHandler eventHandler) {
		/*
		 * TODO Execute the plans in the PSim, feed the event handler.
		 */
	}

}
