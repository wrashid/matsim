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

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.ReflectiveConfigGroup;
import org.matsim.core.router.util.TravelTime;

import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.Units;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class AccelerationConfigGroup extends ReflectiveConfigGroup implements ReplanningParameterContainer {

	// ==================== MATSim-SPECIFICS ====================

	// -------------------- CONSTANTS --------------------

	public static final String GROUP_NAME = "acceleration";

	// -------------------- mode --------------------

	public static enum ModeType {
		off, accelerate, mah2007, mah2009
	};

	private ModeType modeTypeField = null;

	@StringGetter("mode")
	public ModeType getModeTypeField() {
		return this.modeTypeField;
	}

	@StringSetter("mode")
	public void setModeTypeField(final ModeType modeTypeField) {
		this.modeTypeField = modeTypeField;
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

	// private double relativeCongestionTreshold = Double.NaN;
	//
	// @StringGetter("relativeCongestionThreshold")
	// public double getRelativeCongestionThreshold() {
	// return this.relativeCongestionTreshold;
	// }
	//
	// @StringSetter("relativeCongestionThreshold")
	// public void setRelativeCongestionThreshold(final double
	// relativeCongestionThreshold) {
	// this.relativeCongestionTreshold = relativeCongestionThreshold;
	// }

	// -------------------- baselineReplanningRate --------------------

	private double baselineReplanningRate = Double.NaN;

	@StringGetter("baselineReplanningRate")
	public double getBaselineReplanningRate() {
		return this.baselineReplanningRate;
	}

	@StringSetter("baselineReplanningRate")
	public void setBaselineReplanningRate(final double baselineReplanningRate) {
		this.baselineReplanningRate = baselineReplanningRate;
	}

	// -------------------- congestionProportionalWeighting --------------------

	// private boolean congestionProportionalWeighting = false;
	//
	// @StringGetter("congestionProportionalWeighting")
	// public boolean getCongestionProportionalWeighting() {
	// return this.congestionProportionalWeighting;
	// }
	//
	// @StringSetter("congestionProportionalWeighting")
	// public void setCongestionProportionalWeighting(final boolean
	// congestionProportionalWeighting) {
	// this.congestionProportionalWeighting = congestionProportionalWeighting;
	// }

	// -------------------- randomizeIfNoImprovement --------------------

	private boolean randomizeIfNoImprovement = false;

	@StringGetter("randomizeIfNoImprovement")
	public boolean getRandomizeIfNoImprovement() {
		return this.randomizeIfNoImprovement;
	}

	@StringSetter("randomizeIfNoImprovement")
	public void setRandomizeIfNoImprovement(final boolean randomizeIfNoImprovement) {
		this.randomizeIfNoImprovement = randomizeIfNoImprovement;
	}

	// ========== IMPLEMENTATION OF ReplanningParameterContainer ==========

	// -------------------- STATIC UTILITIES --------------------

	public static Map<Id<Link>, Double> newUniformLinkWeights(final Network network) {
		final Map<Id<Link>, Double> weights = new LinkedHashMap<>();
		for (Link link : network.getLinks().values()) {
			weights.put(link.getId(), 1.0);
		}
		return weights;
	}

	public static Map<Id<Link>, Double> newOneOverCapacityLinkWeights(final Network network) {
		final Map<Id<Link>, Double> weights = new LinkedHashMap<>();
		for (Link link : network.getLinks().values()) {
			final double cap_veh_h = link.getFlowCapacityPerSec() * Units.VEH_H_PER_VEH_S;
			if (cap_veh_h <= 1e-6) {
				throw new RuntimeException("link " + link.getId() + " has capacity of " + cap_veh_h + " veh/h");
			}
			weights.put(link.getId(), 1.0 / cap_veh_h);
		}
		return weights;
	}

	// -------------------- MEMBERS --------------------

	private Network network = null; // needs to be explicitly set

	private TimeDiscretization myTimeDiscretization = null; // lazy initialization

	private Map<Id<Link>, Double> linkWeights = null; // lazy initialization

	// -------------------- CONSTRUCTION AND INITIALIZATION --------------------

	public AccelerationConfigGroup() {
		super(GROUP_NAME);
	}

	public void setNetwork(final Network network) {
		this.network = network;
	}

	// -------------------- INTERNALS --------------------

	private Link linkFromLocObj(final Object linkId) {
		if (!(linkId instanceof Id<?>)) {
			throw new RuntimeException("linkId is of type " + linkId.getClass().getSimpleName());
		}
		final Link link = this.network.getLinks().get(linkId);
		if (link == null) {
			throw new RuntimeException(
					"locObj (i.e. linkId) " + linkId + " does not refer to an existing network link");
		}
		return link;
	}

	private double congestionFactor(final Link link, int timeBin, final TravelTime travelTimes) {
		final int time_s = this.getTimeDiscretization().getBinCenterTime_s(timeBin);
		final double minimalTT_s = link.getLength() / link.getFreespeed(time_s);
		final double realizedTT_s = travelTimes.getLinkTravelTime(link, time_s, null, null);
		return (realizedTT_s / Math.max(minimalTT_s, 1e-9));
	}

	// -------------------- INTERFACE IMPLEMENTATION --------------------

	@Override
	public TimeDiscretization getTimeDiscretization() {
		if (this.myTimeDiscretization == null) {
			this.myTimeDiscretization = new TimeDiscretization(this.getStartTime_s(), this.getBinSize_s(),
					this.getBinCnt());
		}
		return this.myTimeDiscretization;
	}

	@Override
	public double getMeanReplanningRate(int iteration) {
		return this.getMeanReplanningRate();
	}

	@Override
	public double getRegularizationWeight(int iteration, Double deltaN2) {
		if (this.getRegularizationType() == RegularizationType.absolute) {
			return this.getRegularizationWeight();
		} else if (this.getRegularizationType() == RegularizationType.relative) {
			return this.getRegularizationWeight() * this.getMeanReplanningRate(iteration) * deltaN2;
		} else {
			throw new RuntimeException("Unknown regularizationType: " + this.getRegularizationType());
		}
	}

	// @Override
	// public boolean isCongested(Object linkId, int timeBin, TravelTime
	// travelTimes) {
	// if (!(linkId instanceof Id<?>)) {
	// throw new RuntimeException("linkId is of type " +
	// linkId.getClass().getSimpleName());
	// }
	// final Link link = this.network.getLinks().get(linkId);
	// return (this.congestionFactor(link, timeBin, travelTimes) >=
	// this.relativeCongestionTreshold);
	// }

	@Override
	public double getWeight(Object linkId, int bin, TravelTime travelTimes) {
		if (this.linkWeights == null) {
			if (this.weightingField == LinkWeighting.uniform) {
				this.linkWeights = newUniformLinkWeights(network);
			} else if (this.weightingField == LinkWeighting.oneOverCapacity) {
				this.linkWeights = newOneOverCapacityLinkWeights(network);
			} else {
				throw new RuntimeException("unhandled link weighting \"" + this.weightingField + "\"");
			}
		}
		if (!(linkId instanceof Id<?>)) {
			throw new RuntimeException("linkId is of type " + linkId.getClass().getSimpleName());
		}
		// if (this.isCongested(linkId, bin, travelTimes)) {
		// if (this.getCongestionProportionalWeighting()) {
		// return this.linkWeights.get(linkId) *
		// this.congestionFactor(this.linkFromLocObj(linkId), bin, travelTimes);
		// } else {
		return this.linkWeights.get(linkId);
		// }
		// } else {
		// return 0.0;
		// }
	}

}
