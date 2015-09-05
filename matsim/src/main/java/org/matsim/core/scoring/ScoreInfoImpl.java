package org.matsim.core.scoring;

public class ScoreInfoImpl implements ScoreInfo {

	private Double score;
	
	public ScoreInfoImpl(Double score) {
		super();
		this.score = score;
	}

	@Override
	public Double getScore() {
		return score;
	}

}
