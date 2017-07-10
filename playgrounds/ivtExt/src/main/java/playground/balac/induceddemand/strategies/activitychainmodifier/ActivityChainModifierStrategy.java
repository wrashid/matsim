package playground.balac.induceddemand.strategies.activitychainmodifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Provider;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.ScoringParametersForPerson;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

import com.google.inject.Inject;
import com.google.inject.name.Named;


public class ActivityChainModifierStrategy implements Provider<PlanStrategy>{
	
	private Provider<TripRouter> tripRouterProvider;
	private Scenario scenario;
	private final Map<String, QuadTree<ActivityFacility>> shopFacilityQuadTree;
	private final Map<String, QuadTree<ActivityFacility>> leisureFacilityQuadTree;
	private LeastCostPathCalculatorFactory pathCalculatorFactory;
	private Map<String, TravelTime> travelTimes;
	private Map<String, TravelDisutilityFactory> travelDisutilityFactories;
	private ScoringFunctionFactory scoringFunctionFactory;
	private HashMap scoreChange;
	private ScoringParametersForPerson parametersForPerson;
	private final ActivityFacilities facilities;

	@Inject
	public ActivityChainModifierStrategy(final Scenario scenario, 
			Provider<TripRouter> tripRouterProvider, @Named("shopQuadTree") HashMap shopFacilityQuadTree,
			   @Named("leisureQuadTree") HashMap leisureFacilityQuadTree,
			   LeastCostPathCalculatorFactory pathCalculatorFactory, Map<String,TravelTime> travelTimes,
			   Map<String,TravelDisutilityFactory> travelDisutilityFactories, ScoringFunctionFactory scoringFunctionFactory,
			   @Named("scoreChangeMap") HashMap scoreChange, ScoringParametersForPerson parametersForPerson,  final ActivityFacilities facilities) {
		
		this.scenario = scenario;
		this.tripRouterProvider = tripRouterProvider;
		this.shopFacilityQuadTree = shopFacilityQuadTree;
		this.leisureFacilityQuadTree = leisureFacilityQuadTree;
		this.pathCalculatorFactory = pathCalculatorFactory;
		this.travelTimes = travelTimes;
		this.travelDisutilityFactories = travelDisutilityFactories;
		this.scoringFunctionFactory = scoringFunctionFactory;
		this.scoreChange = scoreChange;
		this.parametersForPerson = parametersForPerson;
		this.facilities = facilities;
		
	}	
	@Override
	public PlanStrategy get() {
		Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<Plan,Person>()) ;
		builder.addStrategyModule(new ModifyActivityChain(scenario, tripRouterProvider,
	    		shopFacilityQuadTree, leisureFacilityQuadTree, pathCalculatorFactory, travelTimes,
	    		travelDisutilityFactories,scoringFunctionFactory, scoreChange, parametersForPerson, facilities));
		return builder.build() ;
	}

}
