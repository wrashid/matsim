package org.matsim.contrib.carsharing.qsim;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.matsim.contrib.carsharing.bikeshare.BikeshareDepartureHandler;
import org.matsim.core.config.Config;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.qsim.AbstractQSimPlugin;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.agents.PopulationAgentSource;
import org.matsim.core.mobsim.qsim.interfaces.DepartureHandler;

import com.google.inject.Module;

public class CarsharingQSimPlugin extends AbstractQSimPlugin {
	public CarsharingQSimPlugin(Config config) {
		super(config);

	}

	public Collection<? extends Module> modules() {

		return Collections.singletonList(new com.google.inject.AbstractModule() {
			@Override
			protected void configure() {
				install(new CarsharingModule());
				bind(AgentFactory.class).to(CSAgentFactory.class).asEagerSingleton();

			}
		});
	}

	public Collection<Class<? extends AgentSource>> agentSources() {
		return Arrays.asList(ParkCSVehicles.class, PopulationAgentSource.class);
	}
	
	public Collection<Class<? extends DepartureHandler>> departureHandlers() {
		return Arrays.asList(BikeshareDepartureHandler.class);
	}
}
