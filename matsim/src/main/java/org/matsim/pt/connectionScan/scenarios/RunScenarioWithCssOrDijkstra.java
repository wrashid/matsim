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

//        String configPath = "C:\\Users\\gthunig\\VSP\\matsim\\matsim\\matsim-berlin\\scenarios\\berlin-v5.1-1pct\\b5_1\\b5_1.run_config.xml";
        String configPath = "C:\\Users\\gthunig\\Desktop\\Vsp\\ConnectionScan\\Scenarien\\Cottbus\\cb04\\run_config.xml";
        boolean useCss = true;
//        String configPath = args[0];
//        boolean useCss = Boolean.parseBoolean(args[1]);


        Config config = ConfigUtils.loadConfig(configPath);
//        config.plans().setInputFile("./plansTest4.xml.gz");
        config.global().setNumberOfThreads(1);
        config.planCalcScore().setUtilityOfLineSwitch(0); //-- compareable

        Scenario scenario = ScenarioUtils.loadScenario(config);

        scenario.getConfig().controler().setLastIteration(0);
        if (useCss) {
            scenario.getConfig().controler().setOutputDirectory("./output/CottbusScenario/CSTest");
//            scenario.getConfig().controler().setOutputDirectory("./output/BerlinScenario/CS2");
        } else {
//            scenario.getConfig().controler().setOutputDirectory("./output/CottbusScenario/DTest");
            scenario.getConfig().controler().setOutputDirectory("./output/CottbusScenario/D");
        }
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
