package org.matsim.contrib.spatialDrt.firstLastAVPTRouter;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;

public class TransitRouterParams {
    final double marginalUtilityAV_s;
    final double marginalUtilityAVTaxi_s;
    final double marginalUtilityWalk_s;
    final double marginalUtilityAV_m;
    final double marginalUtilityAVTaxi_m;
    final double marginalUtilityWalk_m;
    final double avWaiting;
    final double initialCostAVTaxi;
    final double initialCostAV;
    final double initialCostWalk;
    public TransitRouterParams(PlanCalcScoreConfigGroup pcsConfig){
        this.marginalUtilityAV_s = pcsConfig.getOrCreateModeParams("drt").getMarginalUtilityOfTraveling() /3600;
        this.marginalUtilityAVTaxi_s = pcsConfig.getOrCreateModeParams("drtaxi").getMarginalUtilityOfTraveling()/3600;
        this.marginalUtilityWalk_s = pcsConfig.getOrCreateModeParams("walk").getMarginalUtilityOfTraveling() /3600;
        this.marginalUtilityAV_m = pcsConfig.getOrCreateModeParams("drt").getMarginalUtilityOfDistance() + pcsConfig.getMarginalUtilityOfMoney() * pcsConfig.getModes().get("drt").getMonetaryDistanceRate();
        this.marginalUtilityAVTaxi_m = pcsConfig.getOrCreateModeParams("drtaxi").getMarginalUtilityOfDistance() + pcsConfig.getMarginalUtilityOfMoney() * pcsConfig.getModes().get("drtaxi").getMonetaryDistanceRate();
        this.marginalUtilityWalk_m = pcsConfig.getOrCreateModeParams("walk").getMarginalUtilityOfDistance() + pcsConfig.getMarginalUtilityOfMoney() * pcsConfig.getModes().get("walk").getMonetaryDistanceRate();
        this.initialCostAVTaxi = -pcsConfig.getOrCreateModeParams("drtaxi").getConstant();
        this.initialCostAV = -pcsConfig.getOrCreateModeParams("drt").getConstant();
        this.initialCostWalk = -pcsConfig.getOrCreateModeParams("walk").getConstant();
        this.avWaiting = 5.0*60;
    }
}

