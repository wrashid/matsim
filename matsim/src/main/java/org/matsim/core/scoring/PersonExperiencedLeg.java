package org.matsim.core.scoring;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.api.internal.HasPersonId;
import org.matsim.core.mobsim.framework.HasPerson;

public class PersonExperiencedLeg extends Event implements HasPersonId {
	private final Id<Person> agentId;
	private final Leg leg;

	public PersonExperiencedLeg(Id<Person> agentId, Leg leg) {
		super(leg.getDepartureTime() + leg.getTravelTime());
		this.agentId = agentId;
		this.leg = leg;
	}

	@Override
	public Id<Person> getPersonId() {
		return agentId;
	}

	public Leg getLeg() {
		return leg;
	}

	@Override
	public String getEventType() {
		return this.getClass().getName();
	}

}
