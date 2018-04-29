package org.matsim.contrib.pseudosimulation.searchacceleration.tryout;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationUtils;

/**
 * Copied and simplified from opdytsintegration. 
 * 
 * @author Gunnar Flötteröd
 * 
 */
class PopulationState {

	// -------------------- MEMBERS --------------------

	/**
	 * A map of persons on lists of (deep copies of) all plans of the respective
	 * person. The plan order in the lists matters. Contains an empty but
	 * non-null list for every person that does not have any plans.
	 */
	private final Map<Id<Person>, List<? extends Plan>> person2planList = new LinkedHashMap<>();

	/**
	 * A map of indices pointing to the currently selected plan of every person.
	 * Contains a null value for every person that does not have a selected
	 * plan. Uses an index instead of a reference because references do not
	 * survive deep copies.
	 */
	private final Map<Id<Person>, Integer> person2selectedPlanIndex = new LinkedHashMap<>();
	
	// -------------------- IMPLEMENTATION --------------------

	public PopulationState(final Population population) {
		for (Person person : population.getPersons().values()) {
			if (person.getSelectedPlan() == null) {
				this.person2selectedPlanIndex.put(person.getId(), null);
			} else {
				final int selectedPlanIndex = person.getPlans().indexOf(
						person.getSelectedPlan());
				if (selectedPlanIndex < 0) {
					throw new RuntimeException("The selected plan of person "
							+ person.getId()
							+ " cannot be found in its plan list.");
				}
				this.person2selectedPlanIndex.put(person.getId(),
						selectedPlanIndex);
			}
			this.person2planList.put(person.getId(),
					newDeepCopy(person.getPlans()));
		}
	}

	public void set(final Population population) {
		for (Id<Person> personId : this.person2planList.keySet()) {
			final Person person = population.getPersons().get(personId);
			person.getPlans().clear();
			final List<? extends Plan> copiedPlans = newDeepCopy(this.person2planList
					.get(personId));
			for (Plan plan : copiedPlans) {
				person.addPlan(plan);
			}
			person.setSelectedPlan(getSelectedPlan(copiedPlans,
					this.person2selectedPlanIndex.get(personId)));
		}
	}
	
	// -------------------- HELPERS AND INTERNALS --------------------

	private static List<? extends Plan> newDeepCopy(
			final List<? extends Plan> fromPlanList) {
		final List<Plan> toPlanList = new ArrayList<>(fromPlanList.size());
		for (Plan fromPlan : fromPlanList) {
			final Plan toPlan = PopulationUtils.createPlan(fromPlan.getPerson());
			PopulationUtils.copyFromTo(fromPlan, toPlan);
			toPlanList.add(toPlan);
		}
		return toPlanList;
	}

	private static Plan getSelectedPlan(final List<? extends Plan> plans,
			final Integer index) {
		if (index == null) {
			return null;
		} else {
			return plans.get(index);
		}
	}
}
