/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.searchacceleration;

import org.matsim.core.controler.AbstractModule;

import com.google.inject.Singleton;

import floetteroed.utilities.TimeDiscretization;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class SearchAcceleratorModule extends AbstractModule {

	private final TimeDiscretization timeDiscr;

	private final ReplanningParameterContainer replanningParameterProvider;

	private final LinkWeightContainer linkWeightProvider;

	public SearchAcceleratorModule(final TimeDiscretization timeDiscr,
			final ReplanningParameterContainer replanningParameterProvider,
			final LinkWeightContainer linkWeightProvider) {
		this.timeDiscr = timeDiscr;
		this.replanningParameterProvider = replanningParameterProvider;
		this.linkWeightProvider = linkWeightProvider;
	}

	@Override
	public void install() {

		// TODO To what extent may these instances interact with parallel code?
		this.bind(TimeDiscretization.class).toInstance(this.timeDiscr);
		this.bind(ReplanningParameterContainer.class).toInstance(this.replanningParameterProvider);
		this.bind(LinkWeightContainer.class).toInstance(this.linkWeightProvider);

		this.bind(SearchAccelerator.class).in(Singleton.class);
		this.addControlerListenerBinding().to(SearchAccelerator.class);
		this.addEventHandlerBinding().to(SearchAccelerator.class);

		
		this.addPlanStrategyBinding(CloneHypotheticalReplanningStrategy.NAME)
				.toProvider(CloneHypotheticalReplanningStragetyProvider.class);

	}

}
