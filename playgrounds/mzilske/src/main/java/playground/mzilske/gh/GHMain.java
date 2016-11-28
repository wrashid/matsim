package playground.mzilske.gh;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.StartupEvent;
import org.matsim.core.controler.listener.StartupListener;
import org.matsim.core.router.GraphHopperLeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;

import java.util.Map;

public class GHMain {

    public static void main(String[] args) {
        Config config = ConfigUtils.loadConfig("../../examples/scenarios/berlin/config.xml");
        config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
        config.controler().setLastIteration(5);
        Controler controler = new Controler(config);
        controler.addOverridingModule(new AbstractModule() {
            @Override
            public void install() {
                addControlerListenerBinding().toInstance(new StartupListener() {
                    @Override
                    public void notifyStartup(StartupEvent event) {
                        for (Map.Entry<Id<Person>, ? extends Person> entry : event.getServices().getScenario().getPopulation().getPersons().entrySet()) {
                            for (Plan plan : entry.getValue().getPlans()) {
                                for (PlanElement planElement : plan.getPlanElements()) {
                                    if (planElement instanceof Leg) {
                                        ((Leg) planElement).setRoute(null);
                                    }
                                }
                            }
                        }
                    }
                });
                bind(LeastCostPathCalculatorFactory.class).to(GraphHopperLeastCostPathCalculatorFactory.class);
            }
        });
        controler.run();
    }

}
