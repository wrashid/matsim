package org.matsim.contrib.freight.carrier;

import java.util.Collection;
import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.core.scoring.ScoreInfo;

/**
 * 
 * A specific plan of a carrier, and its score. 
 * 
 * @author mzilske, sschroeder
 * 
 */
public class CarrierPlan implements BasicPlan {

	private final Carrier carrier;
	
	private final Collection<ScheduledTour> scheduledTours;

	private ScoreInfo scoreInfo = null;

	public CarrierPlan(final Carrier carrier, final Collection<ScheduledTour> scheduledTours) {
		this.scheduledTours = scheduledTours;
		this.carrier = carrier;
	}

	public Carrier getCarrier() {
		return carrier;
	}

	@Override
	public ScoreInfo getScoreInfo() {
		return scoreInfo;
	}
	@Override
	public Double getScore() {
		return scoreInfo.getScore();
	}

	@Override
	public void setScoreInfo(ScoreInfo scoreInfo) {
		this.scoreInfo = scoreInfo;
	}

	public Collection<ScheduledTour> getScheduledTours() {
		return scheduledTours;
	}

}
