package org.matsim.core.scoring;

public class ScoreInfoImpl implements ScoreInfo {

	private double score;
	
	public ScoreInfoImpl(double score) {
		super();
		this.score = score;
	}

	@Override
	public Double getScore() {
		return score;
	}

}
