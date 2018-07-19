/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package org.matsim.contrib.emissions.spatialAnalysis;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.emissions.types.WarmPollutant;

/**
 * Created by Amit on 7/18/2018.
 */

public class AnalysisExample {

    public static void main(String[] args) {

        Map<Double,Map<Id<Link>,SortedMap<String,Double>>> linkEmissionsBau = new HashMap<>();
        Map<Double,Map<Id<Link>,SortedMap<String,Double>>> linkEmissionsPolicy = new HashMap<>();

        // setting of input data
        SpatialDataInputs inputs = new SpatialDataInputs(SpatialDataInputs.EmissionSourceMethod.line,bau, policyCase);
        inputs.setBoundingBox(xMin, xMax, yMin, yMax);
        inputs.setTargetCRS(targetCRS);
        inputs.setGridInfo(GridType.HEX, gridSize);
        inputs.setSmoothingRadius(smoothingRadius);
        inputs.setShapeFile(shapeFile);

//		SpatialInterpolation plot = new SpatialInterpolation(inputs,runDir+"/analysis/spatialPlots/"+noOfBins+"timeBins/");
        SpatialInterpolation plot = new SpatialInterpolation(inputs,runDir+"/analysis/spatialPlots/"+noOfBins+"timeBins/", true);

        EmissionLinkAnalyzer emsLnkAna = new EmissionLinkAnalyzer(LoadMyScenarios.getSimulationEndTime(inputs.initialCaseConfig), inputs.initialCaseEmissionEventsFile, noOfBins);
        emsLnkAna.preProcessData();
        emsLnkAna.postProcessData();
        linkEmissionsBau = emsLnkAna.getLink2TotalEmissions();

        if(inputs.isComparing){
            emsLnkAna = new EmissionLinkAnalyzer(LoadMyScenarios.getSimulationEndTime(inputs.compareToCaseConfig), inputs.compareToCaseEmissionEventsFile, noOfBins);
            emsLnkAna.preProcessData();
            emsLnkAna.postProcessData();
            linkEmissionsPolicy = emsLnkAna.getLink2TotalEmissions();
        }

        Scenario sc = LoadMyScenarios.loadScenarioFromNetwork(inputs.initialCaseNetworkFile);

        EmissionTimebinDataWriter writer = new EmissionTimebinDataWriter();
        writer.openWriter(runDir+"/analysis/spatialPlots/"+noOfBins+"timeBins/"+"viaData_NO2_"+GridType.HEX+"_"+gridSize+"_"+smoothingRadius+"_line_"+policyName+"_diff.txt");

        for(double time :linkEmissionsBau.keySet()){
            for(Link l : sc.getNetwork().getLinks().values()){
                Id<Link> id = l.getId();

                if(plot.isInResearchArea(l)){
                    double emiss = 0;
                    if(inputs.isComparing){
                        double linkEmissionBau =0;
                        double linkEmissionPolicy =0;

                        if(linkEmissionsBau.get(time).containsKey(id) && linkEmissionsPolicy.get(time).containsKey(id)) {
                            linkEmissionBau = countScaleFactor * linkEmissionsBau.get(time).get(id).get(WarmPollutant.NO2.toString());
                            linkEmissionPolicy = countScaleFactor * linkEmissionsPolicy.get(time).get(id).get(WarmPollutant.NO2.toString());
                        } else if(linkEmissionsBau.get(time).containsKey(id)){
                            linkEmissionBau = countScaleFactor * linkEmissionsBau.get(time).get(id).get(WarmPollutant.NO2.toString());
                        } else if(linkEmissionsPolicy.get(time).containsKey(id)){
                            linkEmissionPolicy = countScaleFactor * linkEmissionsPolicy.get(time).get(id).get(WarmPollutant.NO2.toString());
                        }
                        emiss = linkEmissionPolicy - linkEmissionBau;

                    } else {
                        if(linkEmissionsBau.get(time).containsKey(id)) emiss = countScaleFactor * linkEmissionsBau.get(time).get(id).get(WarmPollutant.NO2.toString());
                        else emiss =0;
                    }

                    plot.processLink(l,  emiss);

                }
            }
            writer.writeData(time, plot.getCellWeights());
            //			plot.writeRData("NO2_"+(int)time/3600+"h",isWritingGGPLOTData);
            plot.reset();
        }
        writer.closeWriter();


    }

}
