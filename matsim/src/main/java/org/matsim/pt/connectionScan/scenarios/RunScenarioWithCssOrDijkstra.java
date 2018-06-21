package org.matsim.pt.connectionScan.scenarios;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.connectionScan.ConnectionScanTransitRouterProvider;
import org.matsim.pt.router.TransitRouter;

import java.io.File;

public class RunScenarioWithCssOrDijkstra {

    public static void main(String[] args) {

        File file = new File("");
        System.out.println(file.getAbsolutePath());

        String configPath = "../..\\runs-svn\\open_berlin_scenario\\b5_22e1a\\b5_22e1a.run_config.xml";
        boolean useCss = true;
//        String configPath = args[0];
//        boolean useCss = Boolean.parseBoolean(args[1]);


        Config config = ConfigUtils.loadConfig(configPath);
//        config.plans().setInputFile("./b5_22e1a.1pct_plans_pt300.xml.gz"); // <-currently default
        config.plans().setInputFile("./b5_22e1a.1pct_plans_pt1.xml.gz");

        Scenario scenario = ScenarioUtils.loadScenario(config);

//        scenario.getConfig().controler().setLastIteration(0);
        scenario.getConfig().controler().setOutputDirectory("./output/CSCompare2");
        scenario.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);

        Controler controler = new Controler(scenario);

        if (useCss) {
            controler.addOverridingModule(new AbstractModule() {

                @Override
                public void install() {
                bind(TransitRouter.class).toProvider(ConnectionScanTransitRouterProvider.class); //.asEagerSingleton();
                }
            });
        }
        controler.run();
    }
}
