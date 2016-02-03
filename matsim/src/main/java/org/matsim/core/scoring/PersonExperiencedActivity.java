package org.matsim.core.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;

public class PersonExperiencedActivity extends Event implements HasPersonId {
	private final Id<Person> agentId;
	private final Activity activity;

	public PersonExperiencedActivity(Id<Person> agentId, Activity activity) {
		super(activity.getEndTime());
		this.agentId = agentId;
		this.activity = activity;
	}

	@Override
	public Id<Person> getPersonId() {
		return agentId;
	}

	public Activity getActivity() {
		return activity;
	}

	@Override
	public String getEventType() {
		return this.getClass().getName();
	}
}
