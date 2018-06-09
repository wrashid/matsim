package org.matsim.contrib.carsharing.runExample;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.carsharing.bikeshare.BikeshareFleet;
import org.matsim.contrib.carsharing.config.CarsharingConfigGroup;
import org.matsim.contrib.carsharing.control.listeners.CarsharingListener;
import org.matsim.contrib.carsharing.events.handlers.PersonArrivalDepartureHandler;
import org.matsim.contrib.carsharing.manager.CarsharingManagerInterface;
import org.matsim.contrib.carsharing.manager.CarsharingManagerNew;
import org.matsim.contrib.carsharing.manager.demand.CurrentTotalDemand;
import org.matsim.contrib.carsharing.manager.demand.CurrentTotalDemandImpl;
import org.matsim.contrib.carsharing.manager.demand.DemandHandler;
import org.matsim.contrib.carsharing.manager.demand.VehicleChoiceAgent;
import org.matsim.contrib.carsharing.manager.demand.VehicleChoiceAgentImpl;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipContainer;
import org.matsim.contrib.carsharing.manager.demand.membership.MembershipReader;
import org.matsim.contrib.carsharing.manager.routers.RouteCarsharingTrip;
import org.matsim.contrib.carsharing.manager.routers.RouteCarsharingTripImpl;
import org.matsim.contrib.carsharing.manager.routers.RouterProvider;
import org.matsim.contrib.carsharing.manager.routers.RouterProviderImpl;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyContainer;
import org.matsim.contrib.carsharing.manager.supply.CarsharingSupplyInterface;
import org.matsim.contrib.carsharing.manager.supply.costs.CostsCalculatorContainer;
import org.matsim.contrib.carsharing.models.ChooseTheCompany;
import org.matsim.contrib.carsharing.models.ChooseTheCompanyExample;
import org.matsim.contrib.carsharing.models.ChooseVehicleType;
import org.matsim.contrib.carsharing.models.ChooseVehicleTypeExample;
import org.matsim.contrib.carsharing.models.KeepingTheCarModel;
import org.matsim.contrib.carsharing.models.KeepingTheCarModelExample;
import org.matsim.contrib.carsharing.qsim.CarsharingQSimModule;
import org.matsim.contrib.carsharing.readers.CarsharingXmlReaderNew;
import org.matsim.contrib.carsharing.replanning.CarsharingSubtourModeChoiceStrategy;
import org.matsim.contrib.carsharing.replanning.RandomTripToCarsharingStrategy;
import org.matsim.contrib.carsharing.router.CarsharingRoute;
import org.matsim.contrib.carsharing.router.CarsharingRouteFactory;
import org.matsim.contrib.carsharing.scoring.CarsharingScoringFunctionFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.vehicles.Vehicle;

import com.google.inject.Key;
import com.google.inject.name.Names;

/**
 * @author balac
 */

public class RunCarsharing {

	public static void main(String[] args) {
		Logger.getLogger("org.matsim.core.controler.Injector").setLevel(Level.OFF);

		final Config config = ConfigUtils.loadConfig(args[0]);

		if (Integer.parseInt(config.getModule("qsim").getValue("numberOfThreads")) > 1)
			Logger.getLogger("org.matsim.core.controler").warn(
					"Carsharing contrib is not stable for parallel qsim!! If the error occures please use 1 as the number of threads.");

		CarsharingUtils.addConfigModules(config);

		final Scenario sc = ScenarioUtils.loadScenario(config);
		sc.getPopulation().getFactory().getRouteFactories().setRouteFactory(CarsharingRoute.class,
				new CarsharingRouteFactory());

		final Controler controler = new Controler(sc);

		installCarSharing(controler);

		controler.run();

	}

	public static void installCarSharing(final Controler controler) {

		final Scenario scenario = controler.getScenario();
		TransportModeNetworkFilter filter = new TransportModeNetworkFilter(scenario.getNetwork());
		Set<String> modes = new HashSet<>();
		modes.add("car");
		Network networkFF = NetworkUtils.createNetwork();
		filter.filter(networkFF, modes);
		CarsharingXmlReaderNew reader = new CarsharingXmlReaderNew(networkFF);

		final CarsharingConfigGroup configGroup = (CarsharingConfigGroup) scenario.getConfig()
				.getModule(CarsharingConfigGroup.GROUP_NAME);

		reader.readFile(configGroup.getvehiclelocations());

		Set<String> carsharingCompanies = reader.getCompanies().keySet();

		MembershipReader membershipReader = new MembershipReader();

		membershipReader.readFile(configGroup.getmembership());

		final MembershipContainer memberships = membershipReader.getMembershipContainer();

		final CostsCalculatorContainer costsCalculatorContainer = CarsharingUtils
				.createCompanyCostsStructure(carsharingCompanies);

		final CarsharingListener carsharingListener = new CarsharingListener();
		// final CarsharingSupplyInterface carsharingSupplyContainer = new
		// CarsharingSupplyContainer(controler.getScenario());

		final KeepingTheCarModel keepingCarModel = new KeepingTheCarModelExample();
		final ChooseTheCompany chooseCompany = new ChooseTheCompanyExample();
		final ChooseVehicleType chooseCehicleType = new ChooseVehicleTypeExample();
		final RouterProvider routerProvider = new RouterProviderImpl();
		final CurrentTotalDemand currentTotalDemand = new CurrentTotalDemandImpl(networkFF);
		// final CarsharingManagerInterface carsharingManager = new
		// CarsharingManagerNew();
		final RouteCarsharingTrip routeCarsharingTrip = new RouteCarsharingTripImpl();
		final VehicleChoiceAgent vehicleChoiceAgent = new VehicleChoiceAgentImpl();
		// ===adding carsharing objects on supply and demand infrastructure ===
		controler.addOverridingModule(new CarsharingQSimModule());
		//controler.addOverridingModule(new DvrpTravelTimeModule());

		// bikeshare

		double minx = (1.0D / 0.0D);
		double miny = (1.0D / 0.0D);
		double maxx = (-1.0D / 0.0D);
		double maxy = (-1.0D / 0.0D);
		for (Link l : networkFF.getLinks().values()) {
			if (l.getCoord().getX() < minx)
				minx = l.getCoord().getX();
			if (l.getCoord().getY() < miny)
				miny = l.getCoord().getY();
			if (l.getCoord().getX() > maxx)
				maxx = l.getCoord().getX();
			if (l.getCoord().getY() <= maxy)
				continue;
			maxy = l.getCoord().getY();
		}
		minx -= 1.0D;
		miny -= 1.0D;
		maxx += 1.0D;
		maxy += 1.0D;
		QuadTree<Id<Vehicle>> availableBikesLocationQuadTree = new QuadTree<>(minx, miny, maxx, maxy);
		Map<Id<Vehicle>, Coord> bikeCoordMap = new HashMap<>();
		BikeshareFleet bikeshareFleet = new BikeshareFleet(availableBikesLocationQuadTree, bikeCoordMap);
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(BikeshareFleet.class).toInstance(bikeshareFleet);
				bind(KeepingTheCarModel.class).toInstance(keepingCarModel);
				bind(ChooseTheCompany.class).toInstance(chooseCompany);
				bind(ChooseVehicleType.class).toInstance(chooseCehicleType);
				bind(RouterProvider.class).toInstance(routerProvider);
				bind(CurrentTotalDemand.class).toInstance(currentTotalDemand);
				bind(RouteCarsharingTrip.class).toInstance(routeCarsharingTrip);
				bind(CostsCalculatorContainer.class).toInstance(costsCalculatorContainer);
				bind(MembershipContainer.class).toInstance(memberships);
				bind(CarsharingSupplyInterface.class).to(CarsharingSupplyContainer.class);
				bind(CarsharingManagerInterface.class).to(CarsharingManagerNew.class);
				bind(VehicleChoiceAgent.class).toInstance(vehicleChoiceAgent);
				bind(DemandHandler.class).asEagerSingleton();
			//	bind(Network.class).annotatedWith(Names.named(DvrpRoutingNetworkProvider.DVRP_ROUTING))
			//			.to(Network.class);

				bind(Network.class).annotatedWith(Names.named("carnetwork")).toInstance(networkFF);
			//	bind(TravelTime.class).annotatedWith(Names.named("ff"))
			//			.to(Key.get(TravelTime.class, Names.named(DvrpTravelTimeModule.DVRP_ESTIMATED)));
			}

		});

		// === carsharing specific replanning strategies ===

		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				this.addPlanStrategyBinding("RandomTripToCarsharingStrategy").to(RandomTripToCarsharingStrategy.class);
				this.addPlanStrategyBinding("CarsharingSubtourModeChoiceStrategy")
						.to(CarsharingSubtourModeChoiceStrategy.class);
			}
		});

		// === adding qsimfactory, controller listeners and event handlers
		controler.addOverridingModule(new AbstractModule() {
			@Override
			public void install() {
				addControlerListenerBinding().toInstance(carsharingListener);
				addControlerListenerBinding().to(CarsharingManagerNew.class);
				// bindScoringFunctionFactory().to(CarsharingScoringFunctionFactory.class);
				addEventHandlerBinding().to(PersonArrivalDepartureHandler.class);
				addEventHandlerBinding().to(DemandHandler.class);
			}
		});
		// === adding carsharing specific scoring factory ===
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {

				bindScoringFunctionFactory().to(CarsharingScoringFunctionFactory.class);
			}
		});

		// === routing moduels for carsharing trips ===

		controler.addOverridingModule(CarsharingUtils.createRoutingModule());
	}

}
