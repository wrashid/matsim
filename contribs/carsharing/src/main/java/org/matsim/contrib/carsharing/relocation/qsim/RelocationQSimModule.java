package org.matsim.contrib.carsharing.relocation.qsim;

import java.util.Map;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.relocation.demand.CarsharingVehicleRelocationContainer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.agents.AgentFactory;
import org.matsim.core.mobsim.qsim.components.QSimComponents;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

public class RelocationQSimModule extends AbstractQSimModule {
	static public String CARSHARING_RELOCATION_VEHICLES_SOURCE = "RelocationAgentSource";

	public RelocationQSimModule() {

	}

	@Override
	protected void configureQSim() {
		bind(RelocationAgentSource.class).asEagerSingleton();
	}
	
	@Provides
	@Singleton
	public RelocationAgentSource provideAgentSource(QSim qsim, Scenario scenario, AgentFactory agentFactory,
			CarsharingVehicleRelocationContainer carsharingVehicleRelocation, Provider<TripRouter> routerProvider,
			CarsharingSupplyInterface carsharingSupply, @Named("ra") LeastCostPathCalculator lcpc) {
		return new RelocationAgentSource(scenario, qsim, carsharingVehicleRelocation, routerProvider, carsharingSupply,
				lcpc);
	}
	
	@Provides
	@Singleton
	@Named("ra")
	LeastCostPathCalculator provideRAPathCalculator(@Named("ff") TravelTime travelTimes,
			@Named("carnetwork") Network network, LeastCostPathCalculatorFactory pathCalculatorFactory,
			Map<String, TravelDisutilityFactory> travelDisutilityFactories, Scenario scenario) {

		TravelDisutility travelDisutility = travelDisutilityFactories.get(TransportMode.car)
				.createTravelDisutility(travelTimes);
		return pathCalculatorFactory.createPathCalculator(network, travelDisutility, travelTimes);
	}

	static public void configureComponents(QSimComponents components) {
		components.activeAgentSources.add(RelocationQSimModule.CARSHARING_RELOCATION_VEHICLES_SOURCE);
	}
}
