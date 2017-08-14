package playground.balac.test;

import java.io.BufferedWriter;
import java.io.IOException;

import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.MatsimEventsReader;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;

public class ActivitiesAnalysis {

	public static void main(String[] args) throws IOException {

		final BufferedWriter outLink = IOUtils.getBufferedWriter("C:\\Users\\balacm\\Desktop\\bj_res4_act.txt");
		final BufferedWriter outLinkT = IOUtils.getBufferedWriter("C:\\Users\\balacm\\Desktop\\bj_res4_trips.txt");

		ObjectAttributes bla = new ObjectAttributes();
		
		new ObjectAttributesXmlReader(bla).readFile(args[0]);
		
		String inputFile = "C:\\Users\\balacm\\Desktop\\output_events.xml.gz";

		//create an event object
		EventsManager events = EventsUtils.createEventsManager();

		//create the handler and add it
		HandlerActivities handler1 = new HandlerActivities();
		events.addHandler(handler1);
		
        //create the reader and read the file
		MatsimEventsReader reader = new MatsimEventsReader(events);
		reader.readFile(inputFile);
		
		
		System.out.println("=========================================");
		outLink.write("id;type;start;end;mode;pref");
		outLink.newLine();
		for (ActivityData ad : handler1.allActivities) {
			if (!(ad.type.equals("pt interaction") || ad.type.startsWith("cb"))) {
				if (bla.getAttribute(ad.id, "subpopulation") == null || !(bla.getAttribute(ad.id, "subpopulation").equals("outAct") || bla.getAttribute(ad.id, "subpopulation").equals("diff_first_last"))) {
				outLink.write(ad.toString() + ";" + bla.getAttribute(ad.id, "typicalDuration_" + ad.type));
				outLink.newLine();
				}
			}
		}
		outLinkT.write("id;start;end;mode");
		outLinkT.newLine();
		for (TravelData td : handler1.allTrips) {
			if (bla.getAttribute(td.id, "subpopulation") == null || !(bla.getAttribute(td.id, "subpopulation").equals("outAct") || bla.getAttribute(td.id, "subpopulation").equals("diff_first_last"))) {
				outLinkT.write(td.toString());
				outLinkT.newLine();
			}
		}
		
		
		outLink.flush();
		outLink.close();
				
		System.out.println("Events file read!");
		
		
	}
}
