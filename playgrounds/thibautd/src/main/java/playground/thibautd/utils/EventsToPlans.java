/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToPlans.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.thibautd.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scoring.*;
import org.matsim.core.scoring.EventsToActivities.ActivityHandler;
import org.matsim.core.scoring.EventsToLegs.LegHandler;
import org.matsim.core.utils.collections.MapUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author thibautd
 */
public class EventsToPlans implements ActivityHandler, LegHandler {

	private final IdFilter filter;

	private boolean locked = false;
	private final Map<Id, Plan> agentsPlans = new HashMap<Id, Plan>();

	public static interface IdFilter {
		public boolean accept(final Id id);
	}

	public EventsToPlans() {
		this( new IdFilter() {
			@Override
			public boolean accept(final Id id) {
				return true;
			}
		});
	}

	@Override
	public void handleActivity(final PersonExperiencedActivity event) {
		if ( !filter.accept( event.getPersonId()) ) return;
		final Plan plan =
				MapUtils.getArbitraryObject(
						event.getPersonId(),
						agentsPlans,
						new MapUtils.Factory<Plan>() {
							@Override
							public Plan create() {
								return new PlanImpl(PopulationUtils.createPerson(event.getPersonId()));
							}
						});
		plan.addActivity( event.getActivity());

	}

	@Override
	public void handleLeg(final PersonExperiencedLeg event) {
		if ( !filter.accept( event.getPersonId()) ) return;
		final Plan plan =
				MapUtils.getArbitraryObject(
						event.getPersonId(),
						agentsPlans,
						new MapUtils.Factory<Plan>() {
							@Override
							public Plan create() {
								return new PlanImpl(PopulationUtils.createPerson(event.getPersonId()));
							}
						});
		plan.addLeg( event.getLeg() );

	}
	public EventsToPlans(final IdFilter filter) {
		this.filter = filter;
	}

	public Map<Id, Plan> getPlans() {
		if ( !locked ) {
			locked = true;
		}
		return agentsPlans;
	}



}

