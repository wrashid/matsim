package org.matsim.contrib.pseudosimulation.searchacceleration.tryout;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class CloneHypotheticalReplanningStrategy implements PlanStrategy {

	public static final String NAME = "CloneHypotheticalReplanning";

	private final SearchAccelerator searchAccelerator;
	
	public CloneHypotheticalReplanningStrategy(final SearchAccelerator searchAccelerator) {
		this.searchAccelerator = searchAccelerator;
	}
	
	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		SearchAccelerator.log("searchAccelerator = " + this.searchAccelerator, true);
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		//SearchAccelerator.log("searchAccelerator = " + this.searchAccelerator, true);
	}

	@Override
	public void finish() {
		// SearchAccelerator.log("searchAccelerator = " + this.searchAccelerator, true);
	}
}
