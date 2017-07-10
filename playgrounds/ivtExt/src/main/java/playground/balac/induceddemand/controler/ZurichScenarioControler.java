package playground.balac.induceddemand.controler;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.locationchoice.DestinationChoiceConfigGroup;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceBestResponseContext;
import org.matsim.contrib.locationchoice.bestresponse.DestinationChoiceInitializer;
import org.matsim.contrib.socnetsim.utils.QuadTreeRebuilder;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryLogging;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.algorithms.WorldConnectLocations;

import com.google.inject.name.Names;

import playground.balac.induceddemand.config.ActivityStrategiesConfigGroup;
import playground.balac.induceddemand.controler.listener.ActivitiesAnalysisListener;
import playground.balac.induceddemand.strategies.activitychainmodifier.ActivityChainModifierStrategy;
import playground.ivt.kticompatibility.KtiLikeScoringConfigGroup;

/**
 * 
 * Mostly a copy-paste from RunZurichScenario, with added strategies for schedule adaptation.
 * 
 */
public class ZurichScenarioControler {

	public static void main(final String[] args) {

		final String configFile = args[ 0 ];

		// This allows to get a log file containing the log messages happening
		// before controler init.
		OutputDirectoryLogging.catchLogEntries();

		// This is the location choice MultiNodeDijkstra.
		// Suppress all log messages of level below error --- to avoid spaming the config
		// file with zillions of "not route found" messages.
		Logger.getLogger( org.matsim.core.router.MultiNodeDijkstra.class ).setLevel( Level.ERROR ); // this is location choice
		Logger.getLogger( org.matsim.pt.router.MultiNodeDijkstra.class ).setLevel( Level.ERROR );
		Logger.getLogger( "org.matsim.analysis.ModeStatsControlerListener" ).setLevel(Level.OFF);


		final Config config = ConfigUtils.loadConfig(
				configFile,
				// this adds a new config group, used by the specific scoring function
				// we use
				new KtiLikeScoringConfigGroup(), new DestinationChoiceConfigGroup(),
				new ActivityStrategiesConfigGroup(), 				new BJActivityScoringConfigGroup());

		// This is currently needed for location choice: initializing
		// the location choice writes K-values files to the output directory, which:
		// - fails if the directory does not exist
		// - makes the controler crash latter if the unsafe setOverwriteFiles( true )
		// is not called.
		// This ensures that we get safety with location choice working as expected,
		// before we sort this out and definitely kick out setOverwriteFiles.
		createEmptyDirectoryOrFailIfExists( config.controler().getOutputDirectory() );
		final Scenario scenario = ScenarioUtils.loadScenario( config );

		final Controler controler = new Controler( scenario );
		//connectFacilitiesWithNetwork(controler);
		controler.addOverridingModule(new BlackListedTimeAllocationMutatorStrategyModule());

		initializeActivityStrategies(scenario, controler);
	
		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				        
				bindScoringFunctionFactory().to(ZurichScoringFunctionFactory.class);	
			}
		});		
		
		
		controler.run();
	}
	
	private static void initializeActivityStrategies(Scenario sc, Controler controler){
		
		
		//we need a map of quad trees
		
		Map<String, QuadTreeRebuilder<ActivityFacility>> shoppingFacilities = new HashMap <>();
		Map<String, QuadTreeRebuilder<ActivityFacility>> leisureFacilities = new HashMap <>();

		for (ActivityFacility af : sc.getActivityFacilities().getFacilities().values()) {
			
			for (String activityType : af.getActivityOptions().keySet()) {
				
				if (activityType.startsWith("shopping")) {
					if (shoppingFacilities.containsKey(activityType)) {
						shoppingFacilities.get(activityType).put(af.getCoord(), af);
					}
					else {
						final QuadTreeRebuilder<ActivityFacility> shopFacilitiesQuadTree = new QuadTreeRebuilder<ActivityFacility>();
						shopFacilitiesQuadTree.put(af.getCoord(), af);
						shoppingFacilities.put(activityType, shopFacilitiesQuadTree);
					}
				}
				else if (activityType.startsWith("secondary"))  {
					if (leisureFacilities.containsKey(activityType)) {
						leisureFacilities.get(activityType).put(af.getCoord(), af);
					}
					else {
						final QuadTreeRebuilder<ActivityFacility> leisureFacilitiesQuadTree = new QuadTreeRebuilder<ActivityFacility>();
						leisureFacilitiesQuadTree.put(af.getCoord(), af);
						leisureFacilities.put(activityType, leisureFacilitiesQuadTree);
					}
				}
			}
		}
		
		HashMap<String, QuadTree<ActivityFacility>> shoppingFacilitiesQuadTree = new HashMap <String, QuadTree<ActivityFacility>>();
		HashMap<String, QuadTree<ActivityFacility>> leisureFacilitiesQuadTree = new HashMap <String, QuadTree<ActivityFacility>>();
		
		for (String type : shoppingFacilities.keySet()) {
			shoppingFacilitiesQuadTree.put(type, shoppingFacilities.get(type).getQuadTree());
		}
		
		for (String type : leisureFacilities.keySet()) {
			leisureFacilitiesQuadTree.put(type, leisureFacilities.get(type).getQuadTree());
		}
			
		HashMap<String, Double> scoreChange = new HashMap<String, Double>();
		
		controler.addOverridingModule(new AbstractModule() {

			@Override
			public void install() {
				bind(HashMap.class)
				.annotatedWith(Names.named("shopQuadTree"))
				.toInstance(shoppingFacilitiesQuadTree);
				
				bind(HashMap.class)
				.annotatedWith(Names.named("leisureQuadTree"))
				.toInstance(leisureFacilitiesQuadTree);
				bind(HashMap.class)
				.annotatedWith(Names.named("scoreChangeMap"))
				.toInstance(scoreChange);
			}
			
		});		
		controler.addControlerListener(new ActivitiesAnalysisListener(sc, scoreChange));

		controler.addOverridingModule( new AbstractModule() {
			@Override
			public void install() {
			//	this.addPlanStrategyBinding("InsertRandomActivityWithLocationChoiceStrategy").to( InsertRandomActivityWithLocationChoiceStrategy.class ) ;

			//	this.addPlanStrategyBinding("RandomActivitiesSwaperStrategy").to( RandomActivitiesSwaperStrategy.class ) ;
				
			//	this.addPlanStrategyBinding("RemoveRandomActivityStrategy").to( RemoveRandomActivityStrategy.class ) ;
				addPlanStrategyBinding("ActivityChainModifierStrategy").toProvider(ActivityChainModifierStrategy.class);


			}
		});	
		
	}

	private static void connectFacilitiesWithNetwork(MatsimServices controler) {
        ActivityFacilities facilities = controler.getScenario().getActivityFacilities();
		//log.warn("number of facilities: " +facilities.getFacilities().size());
        Network network = (Network) controler.getScenario().getNetwork();
		//log.warn("number of links: " +network.getLinks().size());

		WorldConnectLocations wcl = new WorldConnectLocations(controler.getConfig());
		wcl.connectFacilitiesWithLinks(facilities, network);
	}

	private static void initializeLocationChoice( final MatsimServices controler ) {
		final Scenario scenario = controler.getScenario();
		final DestinationChoiceBestResponseContext lcContext =
			new DestinationChoiceBestResponseContext( scenario );
		lcContext.init();

		controler.addControlerListener(
				new DestinationChoiceInitializer(
					lcContext));
	}

	private static void createEmptyDirectoryOrFailIfExists(final String directory) {
		final File file = new File( directory +"/" );
		if ( file.exists() && file.list().length > 0 ) {
			throw new UncheckedIOException( "Directory "+directory+" exists and is not empty!" );
		}
		file.mkdirs();
	}

}
