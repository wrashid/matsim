package org.matsim.contrib.pseudosimulation.searchacceleration.tryout;

import org.matsim.core.controler.AbstractModule;

import com.google.inject.Singleton;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class SearchAcceleratorModule extends AbstractModule {

	@Override
	public void install() {
		
		this.bind(SearchAccelerator.class).in(Singleton.class);
		
		this.addControlerListenerBinding().to(SearchAccelerator.class);
		this.addPlanStrategyBinding(CloneHypotheticalReplanningStrategy.NAME)
				.toProvider(CloneHypotheticalReplanningStragetyProvider.class);
	}

}
