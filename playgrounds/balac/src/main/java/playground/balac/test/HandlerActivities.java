package playground.balac.test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.matsim.api.core.v01.events.ActivityEndEvent;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.events.PersonArrivalEvent;
import org.matsim.api.core.v01.events.PersonDepartureEvent;
import org.matsim.api.core.v01.events.handler.ActivityEndEventHandler;
import org.matsim.api.core.v01.events.handler.ActivityStartEventHandler;
import org.matsim.api.core.v01.events.handler.PersonArrivalEventHandler;
import org.matsim.api.core.v01.events.handler.PersonDepartureEventHandler;

public class HandlerActivities
		implements ActivityStartEventHandler, ActivityEndEventHandler, PersonArrivalEventHandler, PersonDepartureEventHandler {

	
	HashMap<String, ActivityData> currentActivity = new HashMap<>();
	HashMap<String, TravelData> currentTrip = new HashMap<>();

	Set<ActivityData> allActivities = new HashSet<>();
	Set<TravelData> allTrips = new HashSet<>();

	@Override
	public void reset(int iteration) {

	}

	@Override
	public void handleEvent(PersonArrivalEvent event) {

		ActivityData ad =  new ActivityData();
		
		ad.mode = event.getLegMode();
		ad.id = event.getPersonId().toString();
		currentActivity.put(event.getPersonId().toString(), ad);
		
		TravelData td = currentTrip.get(event.getPersonId().toString());
		if (td != null) {
			td.endTime = event.getTime();
			allTrips.add(td);
		}
		
	}

	@Override
	public void handleEvent(ActivityEndEvent event) {
		ActivityData ad = currentActivity.get(event.getPersonId().toString());
		if (ad != null) {
			ad.endTime = event.getTime();
			allActivities.add(ad);
		}
	}

	@Override
	public void handleEvent(ActivityStartEvent event) {
		ActivityData ad = currentActivity.get(event.getPersonId().toString());
		if (ad != null) {
			ad.startTime = event.getTime();
			ad.type = event.getActType();
		}
		
	}

	public Set<ActivityData> getAllActivities() {
		return allActivities;
	}
	
	public Set<TravelData> getAllTrips() {
		return allTrips;
	}

	@Override
	public void handleEvent(PersonDepartureEvent event) {

		TravelData td =  new TravelData();
		
		td.mode = event.getLegMode();
		td.id = event.getPersonId().toString();
		td.startTime = event.getTime();
		currentTrip.put(event.getPersonId().toString(), td);
	}

}
