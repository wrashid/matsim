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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.router.util.TravelTime;

import floetteroed.utilities.TimeDiscretization;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AccelerationConfigGroup extends ReflectiveConfigGroup {

	// -------------------- CONSTANTS --------------------

	public static final String GROUP_NAME = "acceleration";

	// -------------------- CONSTRUCTION --------------------

	public AccelerationConfigGroup() {
		super(GROUP_NAME);
	}

	// -------------------- accelerate --------------------

	private boolean accelerate = false;

	@StringGetter("accelerate")
	public boolean getAccelerate() {
		return this.accelerate;
	}

	@StringSetter("accelerate")
	public void setAccelerate(boolean accelerate) {
		this.accelerate = accelerate;
	}

	// -------------------- startTime_s --------------------

	private int startTime_s = 0;

	@StringGetter("startTime_s")
	public int getStartTime_s() {
		return this.startTime_s;
	}

	@StringSetter("startTime_s")
	public void setStartTime_s(int startTime_s) {
		this.startTime_s = startTime_s;
	}

	// -------------------- binSize_s --------------------

	private int binSize_s = 0;

	@StringGetter("binSize_s")
	public int getBinSize_s() {
		return this.binSize_s;
	}

	@StringSetter("binSize_s")
	public void setBinSize_s(int binSize_s) {
		this.binSize_s = binSize_s;
	}

	// -------------------- binCnt_s --------------------

	private int binCnt = 0;

	@StringGetter("binCnt")
	public int getBinCnt() {
		return this.binCnt;
	}

	@StringSetter("binCnt")
	public void setBinCnt(int binCnt) {
		this.binCnt = binCnt;
	}

	// -------------------- meanReplanningRate --------------------

	private double meanReplanningRate = Double.NaN;

	@StringGetter("meanReplanningRate")
	public double getMeanReplanningRate() {
		return this.meanReplanningRate;
	}

	@StringSetter("meanReplanningRate")
	public void setBinCnt(double meanReplanningRate) {
		this.meanReplanningRate = meanReplanningRate;
	}

	// -------------------- regularizationWeight --------------------

	private double regularizationWeight = Double.NaN;

	@StringGetter("regularizationWeight")
	public double getRegularizationWeight() {
		return this.regularizationWeight;
	}

	@StringSetter("regularizationWeight")
	public void setRegularizationWeight(double regularizationWeight) {
		this.regularizationWeight = regularizationWeight;
	}

	// -------------------- weighting --------------------

	public static enum RegularizationType {
		absolute, relative
	};

	private RegularizationType regularizationTypeField = null;

	@StringGetter("regularizationType")
	public RegularizationType getRegularizationType() {
		return this.regularizationTypeField;
	}

	@StringSetter("regularizationType")
	public void setRegularizationType(final RegularizationType regularizationTypeField) {
		this.regularizationTypeField = regularizationTypeField;
	}

	// -------------------- weighting --------------------

	public static enum LinkWeighting {
		uniform, oneOverCapacity
	};

	private LinkWeighting weightingField = null;

	@StringGetter("linkWeighting")
	public LinkWeighting getWeighting() {
		return this.weightingField;
	}

	@StringSetter("linkWeighting")
	public void setWeighting(final LinkWeighting weightingField) {
		this.weightingField = weightingField;
	}

	// -------------------- relativeCongestionTreshold --------------------

	private double relativeCongestionTreshold = Double.NaN;

	@StringGetter("relativeCongestionThreshold")
	public double getRelativeCongestionThreshold() {
		return this.relativeCongestionTreshold;
	}

	@StringSetter("relativeCongestionThreshold")
	public void setRelativeCongestionThreshold(final double relativeCongestionThreshold) {
		this.relativeCongestionTreshold = relativeCongestionThreshold;
	}

	// -------------------- congestionProportionalWeighting --------------------

	private boolean congestionProportionalWeighting = false;

	@StringGetter("congestionProportionalWeighting")
	public boolean getCongestionProportionalWeighting() {
		return this.congestionProportionalWeighting;
	}

	@StringSetter("congestionProportionalWeighting")
	public void setCongestionProportionalWeighting(final boolean congestionProportionalWeighting) {
		this.congestionProportionalWeighting = congestionProportionalWeighting;
	}

	// -------------------- CUSTOM STUFF --------------------

	private TimeDiscretization myTimeDiscretization = null;

	public TimeDiscretization getTimeDiscretization() {
		if (this.myTimeDiscretization == null) {
			this.myTimeDiscretization = new TimeDiscretization(this.getStartTime_s(), this.getBinSize_s(),
					this.getBinCnt());
		}
		return this.myTimeDiscretization;
	}

	public Map<Id<Link>, Double> newLinkWeights(final Network network) {
		if (this.weightingField == LinkWeighting.uniform) {
			return ReplanningParameterContainer.newUniformLinkWeights(network);
		} else if (this.weightingField == LinkWeighting.oneOverCapacity) {
			return ReplanningParameterContainer.newOneOverCapacityLinkWeights(network);
		} else {
			throw new RuntimeException("unkhandled link weighting \"" + this.weightingField + "\"");
		}
	}

	public double congestionFactor(final Link link, int timeBin, final TravelTime travelTimes) {
		final int time_s = this.getTimeDiscretization().getBinCenterTime_s(timeBin);
		final double minimalTT_s = link.getLength() / link.getFreespeed(time_s);
		final double realizedTT_s = travelTimes.getLinkTravelTime(link, time_s, null, null);
		return (realizedTT_s / Math.max(minimalTT_s, 1e-9));
	}

	public boolean isCongested(final Link link, int timeBin, final TravelTime travelTimes) {
		return (this.congestionFactor(link, timeBin, travelTimes) >= this.relativeCongestionTreshold);
	}

}
