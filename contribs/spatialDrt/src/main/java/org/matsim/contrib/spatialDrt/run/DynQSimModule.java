/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
 * *********************************************************************** */

package org.matsim.contrib.spatialDrt.run;


import com.google.inject.Provides;

import org.matsim.contrib.spatialDrt.dynAgent.DynActivityEngineModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.mobsim.framework.AgentSource;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.AbstractQSimModule;

import org.matsim.core.mobsim.qsim.components.QSimComponents;


import java.util.ArrayList;
import java.util.Collection;

public class DynQSimModule extends AbstractQSimModule {
	public final static String DYN_AGENT_SOURCE_NAME = "DynAgentSource";
	private final Class<? extends AgentSource> agentSourceClass;

	public DynQSimModule(Class<? extends AgentSource> agentSourceClass) {
		this.agentSourceClass = agentSourceClass;
	}

	@Override
	public void configureQSim() {
		install(new DynActivityEngineModule());
		bindAgentSource(DYN_AGENT_SOURCE_NAME).to(agentSourceClass);
	}

	public static void configureComponents(QSimComponents components) {
		DynActivityEngineModule.configureComponents(components);
		components.activeAgentSources.add(DYN_AGENT_SOURCE_NAME);
	}
}
