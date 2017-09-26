/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.agarwalamit.munich;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.emissions.EmissionModule;
import org.matsim.contrib.emissions.example.EmissionControlerListener;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyFactory;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import playground.agarwalamit.InternalizationEmissionAndCongestion.EmissionCongestionTravelDisutilityCalculatorFactory;
import playground.agarwalamit.InternalizationEmissionAndCongestion.InternalizeEmissionsCongestionControlerListener;
import playground.benjamin.internalization.EmissionCostModule;
import playground.benjamin.internalization.EmissionTravelDisutilityCalculatorFactory;
import playground.benjamin.internalization.InternalizeEmissionsControlerListener;
import playground.ikaddoura.analysis.welfare.WelfareAnalysisControlerListener;
import playground.vsp.congestion.controler.MarginalCongestionPricingContolerListener;
import playground.vsp.congestion.handlers.CongestionHandlerImplV3;
import playground.vsp.congestion.handlers.TollHandler;
import playground.vsp.congestion.routing.TollDisutilityCalculatorFactory;

/**
 * @author amit
 */

public class SubPopMunichControler {

	public static void main(String[] args) {

		boolean offline = ! (args.length>0) ;

		if(offline){
			args = new String[]{
					"false",
					"false",
					"false",
					"/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/ijst_branch12Feb2015/input/",
					"1.0",
					"/Users/amit/Documents/repos/runs-svn/detEval/emissionCongestionInternalization/ijst_branch12Feb2015/output/bau/"
			};
		}

		boolean internalizeEmission = Boolean.valueOf(args [0]); 
		boolean internalizeCongestion = Boolean.valueOf(args [1]);
		boolean internalizeBoth = Boolean.valueOf(args [2]);

		String inputFilesDir = args[3];
		String configFileName = "/config_subActivities_subPop_baseCaseCtd.xml";

		String emissionEfficiencyFactor ="1.0";
		String considerCO2Costs = "true";
		String emissionCostFactor = args[5];

		String outputDir = args[6];

		Config config = ConfigUtils.loadConfig(inputFilesDir+configFileName);
		config.controler().setOutputDirectory(outputDir);

		if(offline){
			config.network().setInputFile(inputFilesDir+"/network-86-85-87-84_simplifiedWithStrongLinkMerge---withLanes.xml");
			config.plans().setInputFile(inputFilesDir+"/output_plans.xml.gz");
			config.plans().setInputPersonAttributeFile(inputFilesDir+"/output_personAttributes.xml.gz");
			config.counts().setCountsFileName(null);
		}

		Controler controler = new Controler(config);

		controler.addPlanStrategyFactory("SubtourModeChoice_".concat("COMMUTER_REV_COMMUTER"), new PlanStrategyFactory() {
			String [] availableModes = {"car","pt_COMMUTER_REV_COMMUTER"};
			String [] chainBasedModes = {"car","bike"};
			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario,
					EventsManager eventsManager) {
				PlanStrategyImpl.Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
				builder.addStrategyModule(new SubtourModeChoice(scenario.getConfig().global().getNumberOfThreads(), availableModes, chainBasedModes, false));
				builder.addStrategyModule(new ReRoute(scenario));
				return builder.build();
			}
		});

		// following is must since, feb15, it is not possible to add two replanning strategies together, even for different sub pop except "ChangeExpBeta"
		controler.addPlanStrategyFactory("ReRoute_".concat("COMMUTER_REV_COMMUTER"), new PlanStrategyFactory() {

			@Override
			public PlanStrategy createPlanStrategy(Scenario scenario,
					EventsManager eventsManager) {
				PlanStrategyImpl.Builder builder = new Builder(new RandomPlanSelector<Plan, Person>());
				builder.addStrategyModule(new ReRoute(scenario));
				return builder.build();
			}
		});


		EmissionsConfigGroup ecg = new EmissionsConfigGroup();
		controler.getConfig().addModule(ecg);

		ecg.setAverageColdEmissionFactorsFile(inputFilesDir+"/EFA_ColdStart_vehcat_2005average.txt");
		ecg.setAverageWarmEmissionFactorsFile(inputFilesDir+"/EFA_HOT_vehcat_2005average.txt");
		ecg.setDetailedColdEmissionFactorsFile(inputFilesDir+"/EFA_ColdStart_SubSegm_2005detailed.txt");
		ecg.setDetailedWarmEmissionFactorsFile(inputFilesDir+"/EFA_HOT_SubSegm_2005detailed.txt");
		ecg.setEmissionRoadTypeMappingFile(inputFilesDir+"/roadTypeMapping.txt");
		ecg.setEmissionVehicleFile(inputFilesDir+"/emissionVehicles_1pct.xml.gz");

		ecg.setUsingDetailedEmissionCalculation(true);
		//===only emission events genertaion; used with all runs for comparisons
		EmissionModule emissionModule = new EmissionModule(ScenarioUtils.loadScenario(config));
		emissionModule.setEmissionEfficiencyFactor(Double.parseDouble(emissionEfficiencyFactor));
		emissionModule.createLookupTables();
		emissionModule.createEmissionHandler();

		if(internalizeEmission){
			// this is needed by *both* following modules:
			EmissionCostModule emissionCostModule = new EmissionCostModule(Double.parseDouble(emissionCostFactor), Boolean.parseBoolean(considerCO2Costs));

			// this affects the router by overwriting its generalized cost function (TravelDisutility):
			EmissionTravelDisutilityCalculatorFactory emissionTducf = new EmissionTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule);
			controler.setTravelDisutilityFactory(emissionTducf);
			controler.addControlerListener(new InternalizeEmissionsControlerListener(emissionModule, emissionCostModule));

		} else if(internalizeCongestion){

			TollHandler tollHandler = new TollHandler(controler.getScenario());
			TollDisutilityCalculatorFactory tollDisutilityCalculatorFactory = new TollDisutilityCalculatorFactory(tollHandler);
			controler.setTravelDisutilityFactory(tollDisutilityCalculatorFactory);
			controler.addControlerListener(new MarginalCongestionPricingContolerListener(controler.getScenario(),tollHandler, new CongestionHandlerImplV3(controler.getEvents(), (ScenarioImpl)controler.getScenario()) ));

		} else if(internalizeBoth) {

			TollHandler tollHandler = new TollHandler(controler.getScenario());
			EmissionCostModule emissionCostModule = new EmissionCostModule(Double.parseDouble(emissionCostFactor), Boolean.parseBoolean(considerCO2Costs));
			EmissionCongestionTravelDisutilityCalculatorFactory emissionCongestionTravelDisutilityCalculatorFactory = new EmissionCongestionTravelDisutilityCalculatorFactory(emissionModule, emissionCostModule, tollHandler);
			controler.setTravelDisutilityFactory(emissionCongestionTravelDisutilityCalculatorFactory);
			controler.addControlerListener(new InternalizeEmissionsCongestionControlerListener(emissionModule, emissionCostModule, (ScenarioImpl) controler.getScenario(), tollHandler));

		}

		controler.setOverwriteFiles(true);
		controler.getConfig().controler().setCreateGraphs(true);
		controler.setDumpDataAtEnd(true);
		controler.addControlerListener(new WelfareAnalysisControlerListener((ScenarioImpl) controler.getScenario()));

		if(internalizeEmission==false && internalizeBoth ==false){
			controler.addControlerListener(new EmissionControlerListener());
		}

		controler.run();
	}
}
