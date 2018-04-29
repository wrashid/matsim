package org.matsim.contrib.pseudosimulation.searchacceleration.tryout;

import javax.inject.Provider;

import org.matsim.core.replanning.PlanStrategy;

import com.google.inject.Inject;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class CloneHypotheticalReplanningStragetyProvider implements Provider<PlanStrategy> {
	
	@Inject
	SearchAccelerator searchAccelerator;
	
	@Override
	public PlanStrategy get() {
		return new CloneHypotheticalReplanningStrategy(this.searchAccelerator);
	}

}
