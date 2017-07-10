package playground.balac.utils.personattributes;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.internal.MatsimReader;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.MutableScenario;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

public class AddAttributeForInducedDemand {

	public static void main(String[] args) {

		MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimReader populationReader = new PopulationReader(scenario);
		MatsimNetworkReader networkReader = new MatsimNetworkReader(scenario.getNetwork());
		networkReader.readFile(args[1]);
		populationReader.readFile(args[2]);
		
		ObjectAttributes bla = new ObjectAttributes();
		
		new ObjectAttributesXmlReader(bla).readFile(args[0]);
		
		
		//TODO: add additional activity to test the rescheduling algorithm
		//TODO: add the scoring for the additional activity
		for(Person p : scenario.getPopulation().getPersons().values()) {
			
			String act = "";
			int count = 1;
			for (PlanElement pe : p.getSelectedPlan().getPlanElements()) {
				
				if (pe instanceof Activity) {
					if (act.equals(""))
						act = ((Activity) pe).getType();
					else
						act = act + "," + ((Activity)pe).getType(); 

					if (((Activity) pe).getType().startsWith("shop"))
						count++;
				}
			}		
		
			if (MatsimRandom.getRandom().nextDouble() > 0.7) {
				
				act += "," + "shop_" + count;
				bla.putAttribute(p.getId().toString(), "earliestEndTime_" + "shop_" + count, 0.0);
				bla.putAttribute(p.getId().toString(), "latestStartTime_" + "shop_" + count, 86400.0);
				bla.putAttribute(p.getId().toString(), "minimalDuration_" + "shop_" + count, 0.0);
				bla.putAttribute(p.getId().toString(), "typicalDuration_" + "shop_" + count, MatsimRandom.getRandom().nextDouble() * 3600.0 + 1800.0);

				
			}
			bla.putAttribute(p.getId().toString(), "activities", act);
	
			
		}
		
		ObjectAttributesXmlWriter betaWriter = new ObjectAttributesXmlWriter(bla);
		betaWriter.writeFile(args[3]);		
		
		
	}

}
