package playground.balac.test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;

import com.google.inject.Inject;

public class ScoreAnalysis implements IterationEndsListener {

	@Inject Scenario scenario;
	@Inject private MatsimServices controler;

	ArrayList<ArrayList<Double>> scores = new ArrayList<>();
	
	@Override
	public void notifyIterationEnds(IterationEndsEvent event) {

		int iterationNumber = event.getIteration();
		ArrayList<Double> itScores = new ArrayList<>();
		ObjectAttributes  pa = scenario.getPopulation().getPersonAttributes();
		for (Person person : scenario.getPopulation().getPersons().values()) {

			if (pa.getAttribute(person.getId().toString(), "subpopulation") == null) {
			
				person.getSelectedPlan().getScore();
				itScores.add(person.getSelectedPlan().getScore());
			}
		}
		
		scores.add(itScores);
		
		if (iterationNumber == ((ControlerConfigGroup)scenario.getConfig().getModules().get("controler")).getLastIteration()) {
			final BufferedWriter outLink = 
					IOUtils.getBufferedWriter(
							this.controler.getControlerIO().getIterationFilename(
									event.getIteration(), "scores.txt"));
			
			try {
				outLink.write("iteration,score");
				outLink.newLine();
				int i = 0;
				for (ArrayList<Double> al : this.scores) {
					
					double sum = 0.0;
					for (Double d : al) {
						sum += d;
					}
					outLink.write(Integer.toString(i) + "," + sum / al.size());
					outLink.newLine();
					i++;
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			

			
		}
	}

}
