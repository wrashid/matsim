package org.matsim.api.core.v01.population;

import org.matsim.api.core.v01.Identifiable;

import java.util.List;

public interface HasPlansAndId<T extends BasicPlan, I> extends Identifiable<I> {

	/**
	 * Seems that <? extends T> is actually more restrictive than <T>, i.e. we may later switch from 
	 * <? extends T> to <T>, but not the other way around.
	 * <p></p>
	 * Practically, with <? extends T>, you cannot do getPlans().add( new MyPlans() ), where MyPlans is derived from T.
	 */
	List<? extends T> getPlans();

	/**
	 * adds the plan to the Person's List of plans and
	 * sets the reference to this person in the Plan instance.
	 */
	boolean addPlan(T p);
	
	boolean removePlan(T p);

	T getSelectedPlan();

	void setSelectedPlan(T selectedPlan);

	T createCopyOfSelectedPlanAndMakeSelected() ;

}