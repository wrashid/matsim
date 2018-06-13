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
package org.matsim.contrib.pseudosimulation.searchacceleration.recipes;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Mah2009Recipe implements ReplannerIdentifierRecipe {

	private final Map<Id<Person>, Double> person2utilityGain;

	private final double averageUtilityGain;

	private final double meanLambda;

	public Mah2009Recipe(final Map<Id<Person>, Double> person2utilityGain, final double meanLambda) {

		double averageUtilityGain = 0;
		for (Double val : person2utilityGain.values()) {
			averageUtilityGain += val;
		}
		averageUtilityGain /= person2utilityGain.size();

		final double averageUtilityGainThreshold = 1e-9;
		if (averageUtilityGain < averageUtilityGainThreshold) {
			throw new RuntimeException("average utility gain of " + averageUtilityGain + " is below threshold value of "
					+ averageUtilityGainThreshold);
		}

		this.person2utilityGain = person2utilityGain;
		this.averageUtilityGain = averageUtilityGain;
		this.meanLambda = meanLambda;
	}

	@Override
	public boolean isReplanner(final Id<Person> personId, final double deltaScoreIfYes, final double deltaScoreIfNo) {
		final double inclusionProba = this.meanLambda * this.person2utilityGain.get(personId) / this.averageUtilityGain;
		return (MatsimRandom.getRandom().nextDouble() < inclusionProba);
	}
}
