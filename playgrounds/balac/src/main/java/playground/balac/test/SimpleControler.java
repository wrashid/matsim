package playground.balac.test;

import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;

import playground.balac.test.zurich.ZurichScoringFunctionFactory;


public class SimpleControler {

	public static void main(String[] args) {
		final String configFile = args[ 0 ];
		
		final Config config = ConfigUtils.loadConfig(
				configFile,
				new BJActivityScoringConfigGroup());
		
		Controler controler = new Controler(config);	
		controler.addOverridingModule(new BlackListedTimeAllocationMutatorStrategyModule());

		controler.addOverridingModule(new AbstractModule() {
			
			@Override
			public void install() {
				        
				bindScoringFunctionFactory().to(ZurichScoringFunctionFactory.class);	
		        addControlerListenerBinding().to(ScoreAnalysis.class);

			}
		});		
		
		controler.run();
	}
}
