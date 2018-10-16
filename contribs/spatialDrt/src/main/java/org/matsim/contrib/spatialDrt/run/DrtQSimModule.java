/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** *
 */

package org.matsim.contrib.spatialDrt.run;

import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import org.matsim.contrib.drt.optimizer.DrtOptimizer;
import org.matsim.contrib.drt.optimizer.VehicleData;
import org.matsim.contrib.drt.optimizer.insertion.ParallelPathDataProvider;
import org.matsim.contrib.drt.optimizer.insertion.PrecalculablePathDataProvider;
import org.matsim.contrib.drt.optimizer.insertion.UnplannedRequestInserter;
import org.matsim.contrib.drt.run.Drt;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.schedule.DrtTaskFactory;
import org.matsim.contrib.drt.schedule.DrtTaskFactoryImpl;
import org.matsim.contrib.dvrp.optimizer.VrpOptimizer;
import org.matsim.contrib.dvrp.run.MobsimTimerProvider;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelDisutilityProvider;
import org.matsim.contrib.spatialDrt.bayInfrastructure.VehicleLength;
import org.matsim.contrib.spatialDrt.dynAgent.VrpAgentLogic;
import org.matsim.contrib.spatialDrt.parkingStrategy.DefaultDrtOptimizer;
import org.matsim.contrib.spatialDrt.parkingStrategy.MixedParkingStrategy;
import org.matsim.contrib.spatialDrt.parkingStrategy.ParkingStrategy;
import org.matsim.contrib.spatialDrt.parkingStrategy.alwaysRoaming.RoamingStrategy;
import org.matsim.contrib.spatialDrt.parkingStrategy.insertionOptimizer.DefaultUnplannedRequestInserter;
import org.matsim.contrib.spatialDrt.parkingStrategy.noParkingStrategy.NoParkingStrategy;
import org.matsim.contrib.spatialDrt.parkingStrategy.parkingInDepot.Depot.DepotManager;
import org.matsim.contrib.spatialDrt.parkingStrategy.parkingInDepot.Depot.DepotManagerDifferentDepots;
import org.matsim.contrib.spatialDrt.parkingStrategy.parkingInDepot.Depot.DepotManagerSameDepot;
import org.matsim.contrib.spatialDrt.parkingStrategy.parkingInDepot.ParkingInDepot;
import org.matsim.contrib.spatialDrt.parkingStrategy.parkingOntheRoad.ParkingOntheRoad;
import org.matsim.contrib.spatialDrt.passenger.PassengerEngine;
import org.matsim.contrib.spatialDrt.passenger.PassengerRequestCreator;
import org.matsim.contrib.spatialDrt.schedule.DrtActionCreator;
import org.matsim.contrib.spatialDrt.schedule.DrtRequestCreator;
import org.matsim.contrib.spatialDrt.schedule.VehicleDataEntryFactoryImpl;
import org.matsim.contrib.spatialDrt.scheduler.*;
import org.matsim.core.mobsim.framework.MobsimTimer;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtQSimModule extends AbstractQSimModule {
	@Override
	protected void configureQSim() {
		bind(MobsimTimer.class).toProvider(MobsimTimerProvider.class).asEagerSingleton();
		DvrpTravelDisutilityProvider.bindTravelDisutilityForOptimizer(binder(), DefaultDrtOptimizer.DRT_OPTIMIZER);

		bind(DrtOptimizer.class).to(DefaultDrtOptimizer.class).asEagerSingleton();


		bind(VehicleData.EntryFactory.class).to(VehicleDataEntryFactoryImpl.class).asEagerSingleton();

		bind(DrtTaskFactory.class).to(DrtTaskFactoryImpl.class).asEagerSingleton();
		bind(ModifyLanes.class);
		bind(EmptyVehicleRelocator.class).asEagerSingleton();
		bind(DrtScheduleInquiry.class).asEagerSingleton();
		bind(RequestInsertionScheduler.class).asEagerSingleton();
		bind(DrtScheduleTimingUpdater.class).asEagerSingleton();

		bind(DefaultUnplannedRequestInserter.class).asEagerSingleton();
		bind(UnplannedRequestInserter.class).to(DefaultUnplannedRequestInserter.class);

		bind(ParallelPathDataProvider.class).asEagerSingleton();
		bind(PrecalculablePathDataProvider.class).to(ParallelPathDataProvider.class);

		Named modeNamed = Names.named(DrtConfigGroup.get(getConfig()).getMode());
		bind(VrpOptimizer.class).annotatedWith(modeNamed).to(DrtOptimizer.class);
		bind(VrpAgentLogic.DynActionCreator.class).annotatedWith(modeNamed)
				.to(DrtActionCreator.class)
				.asEagerSingleton();
		bind(PassengerRequestCreator.class).annotatedWith(modeNamed).to(DrtRequestCreator.class).asEagerSingleton();
		bind(PassengerEngine.class).annotatedWith(Drt.class).to(Key.get(PassengerEngine.class, modeNamed));

		// ATOD
		AtodConfigGroup drtCfg = AtodConfigGroup.get(getConfig());
		bind(VehicleLength.class).asEagerSingleton();
		if (drtCfg.getParkingStrategy().equals(ParkingStrategy.Strategies.AlwaysRoaming)){
			bind(ParkingStrategy.class).to(RoamingStrategy.class).asEagerSingleton();
		}else if (drtCfg.getParkingStrategy().equals(ParkingStrategy.Strategies.ParkingOntheRoad)){
			bind(ParkingStrategy.class).to(ParkingOntheRoad.class).asEagerSingleton();
		}else if (drtCfg.getParkingStrategy().equals(ParkingStrategy.Strategies.ParkingInDepot)){
			bind(ParkingStrategy.class).to(ParkingInDepot.class).asEagerSingleton();
			bind(DepotManager.class).to(DepotManagerSameDepot.class).asEagerSingleton();
		}else if (drtCfg.getParkingStrategy().equals(ParkingStrategy.Strategies.MixedParking)){
			bind(ParkingOntheRoad.class).asEagerSingleton();
			bind(ParkingInDepot.class).asEagerSingleton();
			bind(ParkingStrategy.class).to(MixedParkingStrategy.class).asEagerSingleton();
			bind(DepotManager.class).to(DepotManagerDifferentDepots.class).asEagerSingleton();
		}else if (drtCfg.getParkingStrategy().equals(ParkingStrategy.Strategies.NoParkingStrategy)){
			bind(ParkingStrategy.class).to(NoParkingStrategy.class).asEagerSingleton();
		}else{
			throw new RuntimeException("Parking strategy: " + drtCfg.getParkingStrategy().toString() + " does not exist");
		}
	}
}
