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
package org.matsim.contrib.pseudosimulation.searchacceleration.logging;

import org.matsim.contrib.pseudosimulation.searchacceleration.AccelerationAnalyzer;

import floetteroed.utilities.statisticslogging.Statistic;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class UnweightedCountDifferences2 implements Statistic<AccelerationAnalyzer> {

	public static final String LABEL = "UnweightedCountDifferences2";
	
	@Override
	public String label() {
		return LABEL;
	}

	@Override
	public String value(AccelerationAnalyzer arg0) {
		return Statistic.toString(arg0.getSumOfUnweightedCountDifferences2());
	}

}
