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

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.ReplanningContext;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class CloneHypotheticalReplanningStrategy implements PlanStrategy {

	public static final String NAME = "CloneHypotheticalReplanning";

	private final SearchAccelerator searchAccelerator;
	
	public CloneHypotheticalReplanningStrategy(final SearchAccelerator searchAccelerator) {
		this.searchAccelerator = searchAccelerator;
	}
	
	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		// TODO Just testing. Need to get rid of this cast.
		this.searchAccelerator.replan((Person) person);
	}

	@Override
	public void init(ReplanningContext replanningContext) {
	}

	@Override
	public void finish() {
	}
}
