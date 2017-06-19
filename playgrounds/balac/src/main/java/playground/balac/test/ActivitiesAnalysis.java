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

		final BufferedWriter outLink = IOUtils.getBufferedWriter("C:\\Users\\balacm\\Desktop\\TRB\\cn_run2.txt");
		ObjectAttributes bla = new ObjectAttributes();
		
		new ObjectAttributesXmlReader(bla).readFile(args[0]);
		
		String inputFile = "C:\\Users\\balacm\\Desktop\\run.1000.events_cn2.xml.gz";

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
				outLink.write(ad.toString() + ";" + bla.getAttribute(ad.id, "typicalDuration_" + ad.type));
				outLink.newLine();
			}
		}
		
		
		outLink.flush();
		outLink.close();
				
		System.out.println("Events file read!");
		
		
	}
}
