/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * MetaPopulationPlan.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package playground.mzilske.metapopulation;

import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.core.scoring.ScoreInfo;

class MetaPopulationPlan implements BasicPlan {

    private double scaleFactor;
    private ScoreInfo scoreInfo;

    public MetaPopulationPlan(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

    @Override
    public void setScoreInfo(ScoreInfo scoreInfo) {
        this.scoreInfo = scoreInfo;
    }

    @Override
    public ScoreInfo getScoreInfo() {
        return this.scoreInfo;
    }

    @Override
    public Double getScore() {
        return this.scoreInfo.getScore();
    }
    
    public double getScaleFactor() {
        return scaleFactor;
    }

    public void setScaleFactor(double scaleFactor) {
        this.scaleFactor = scaleFactor;
    }

}
