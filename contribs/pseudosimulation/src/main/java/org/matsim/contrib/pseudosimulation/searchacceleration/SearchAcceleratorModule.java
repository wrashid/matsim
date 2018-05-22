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

	// -------------------- MEMBERS --------------------

	private final TimeDiscretization timeDiscr;

	private final ReplanningParameterContainer replanningParameterProvider;

	// -------------------- CONSTRUCTION --------------------

	public SearchAcceleratorModule(final TimeDiscretization timeDiscr,
			final ReplanningParameterContainer replanningParameterProvider) {
		this.timeDiscr = timeDiscr;
		this.replanningParameterProvider = replanningParameterProvider;
	}

	// -------------------- OVERRIDING OF AbstractModule --------------------

	@Override
	public void install() {

		this.bind(TimeDiscretization.class).toInstance(this.timeDiscr);
		this.bind(ReplanningParameterContainer.class).toInstance(this.replanningParameterProvider);

		this.bind(SearchAccelerator.class).in(Singleton.class);
		this.addControlerListenerBinding().to(SearchAccelerator.class);
		this.addEventHandlerBinding().to(SearchAccelerator.class);

		this.addPlanStrategyBinding(AcceptIntendedReplanningStrategy.STRATEGY_NAME)
				.toProvider(AcceptIntendedReplanningStragetyProvider.class);
	}
}
