package org.matsim.contrib.spatialDrt.dynAgent;

import org.matsim.core.mobsim.qsim.AbstractQSimModule;
import org.matsim.core.mobsim.qsim.ActivityEngineModule;
import org.matsim.core.mobsim.qsim.components.QSimComponents;

public class SpatialDrtActivityEngineModule extends AbstractQSimModule {
	public final static String DYN_ACTIVITY_ENGINE_NAME = "DynActivityEngine";

	@Override
	protected void configureQSim() {
		bind(SpatialDrtActivityEngine.class).asEagerSingleton();

		bindMobsimEngine(DYN_ACTIVITY_ENGINE_NAME).to(SpatialDrtActivityEngine.class);
		bindActivityHandler(DYN_ACTIVITY_ENGINE_NAME).to(SpatialDrtActivityEngine.class);
	}

	public static void configureComponents(QSimComponents components) {
		components.activeMobsimEngines.remove(ActivityEngineModule.ACTIVITY_ENGINE_NAME);
		components.activeMobsimEngines.add(SpatialDrtActivityEngineModule.DYN_ACTIVITY_ENGINE_NAME);

		components.activeActivityHandlers.remove(ActivityEngineModule.ACTIVITY_ENGINE_NAME);
		components.activeActivityHandlers.add(SpatialDrtActivityEngineModule.DYN_ACTIVITY_ENGINE_NAME);
	}
}
