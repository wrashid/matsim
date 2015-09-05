package org.matsim.api.core.v01.population;

import org.matsim.core.scoring.ScoreInfo;

public interface BasicPlan {

	public abstract Double getScore();
	
	public ScoreInfo getScoreInfo();

	public void setScoreInfo(ScoreInfo score);

}