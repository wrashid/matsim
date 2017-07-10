package playground.balac.induceddemand.strategies.activitychainmodifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

public class ModifyAndChooseChain implements PlanAlgorithm {


	private StageActivityTypes stageActivityTypes;
	private Map<String, QuadTree<ActivityFacility>> leisureFacilityQuadTree;
	private final TripRouter routingHandler;
	private ScoringParametersForPerson parametersForPerson;
	private Map<String, QuadTree<ActivityFacility>> shopFacilityQuadTree;
	private LeastCostPathCalculator pathCalculator;
	private Scenario scenario;
	private ScoringFunctionFactory scoringFunctionFactory;
	private ActivityFacilities facilities;
	private HashMap scoreChange;

	public ModifyAndChooseChain(Random localInstance, StageActivityTypes stageActivityTypes, Scenario scenario, 
			LeastCostPathCalculator pathCalculator, Map shopFacilityQuadTree, Map leisureFacilityQuadTree,
			ScoringFunctionFactory scoringFunctionFactory, HashMap scoreChange, ScoringParametersForPerson parametersForPerson
			, final TripRouter routingHandler, final ActivityFacilities facilities) {
		
		this.stageActivityTypes = stageActivityTypes;
		this.leisureFacilityQuadTree = leisureFacilityQuadTree;
		this.routingHandler = routingHandler;
		this.parametersForPerson = parametersForPerson;
		this.shopFacilityQuadTree = shopFacilityQuadTree;
		this.pathCalculator = pathCalculator;
		this.scenario = scenario;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.facilities = facilities;
		this.scoreChange = scoreChange;
	}	

	@Override
	public void run(Plan plan) {

		NeighboursCreator nc = new NeighboursCreator(stageActivityTypes, shopFacilityQuadTree, leisureFacilityQuadTree, 
				scenario, pathCalculator, scoringFunctionFactory, scoreChange, parametersForPerson, routingHandler, facilities);
		nc.findBestNeighbour(plan);
	}

}
