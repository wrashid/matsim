/* *********************************************************************** *
 * project: org.matsim.*
 * JavaDEQSim.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.core.mobsim.jdeqsim;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.jdeqsim.util.Timer;

import javax.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The starting point of the whole micro-simulation.
 * @see <a href="http://www.matsim.org/docs/jdeqsim">http://www.matsim.org/docs/jdeqsim</a>
 * @author rashid_waraich
 */
public class JDEQSimulation implements Mobsim {

	private final static Logger log = Logger.getLogger(JDEQSimulation.class);

	protected Scenario scenario;
	protected final PlansConfigGroup.ActivityDurationInterpretation activityDurationInterpretation;

	private final JDEQSimConfigGroup config;
	private final EventsManager events;

    private final Map<Id<Link>, Scheduler> linkIdToScheduler;

	@Inject
	public JDEQSimulation(final JDEQSimConfigGroup config, final Scenario scenario, final EventsManager events) {
		Road.setConfig(config);
		Message.setEventsManager(events);
		this.config = config;
		this.scenario = scenario;
		this.events = events;
		activityDurationInterpretation = scenario.getConfig().plans().getActivityDurationInterpretation();
		this.linkIdToScheduler = null;
	}

	public JDEQSimulation(final JDEQSimConfigGroup config, final Scenario scenario, final EventsManager events,
						  Map<Id<Link>, Scheduler> linkIdToScheduler) {
		Road.setConfig(config);
		Message.setEventsManager(events);
		this.config = config;
		this.scenario = scenario;
		this.events = events;
		this.linkIdToScheduler = linkIdToScheduler;
		activityDurationInterpretation = scenario.getConfig().plans().getActivityDurationInterpretation();
	}

	@Override
	public void run() {
		events.initProcessing();
		Timer timer = new Timer();
		timer.startTimer();

		initializeRoads();
		initializeVehicles();

		linkIdToScheduler.values().stream().distinct().forEach(scheduler -> scheduler.startSimulation());

		timer.endTimer();
		log.info("Time needed for one iteration (only JDEQSimulation part): " + timer.getMeasuredTime() + "[ms]");
		// events.finishProcessing();
	}

	protected void initializeRoads() {
		HashMap<Id<Link>, Road> allRoads = new HashMap<>();
		for (Link link : scenario.getNetwork().getLinks().values()) {
			Scheduler scheduler = linkIdToScheduler.get(link.getId());
			allRoads.put(link.getId(), new Road(scheduler, link));
		}
		Road.setAllRoads(allRoads);
	}

	protected void initializeVehicles() {
		for (Person person : scenario.getPopulation().getPersons().values()) {
			Scheduler scheduler = null;
			// the vehicle registers itself to the scheduler
			List<PlanElement> planElements = person.getSelectedPlan().getPlanElements();
			if (planElements.size() >= 2) {
				Activity act = (Activity)(planElements.get(0));
				scheduler = linkIdToScheduler.get(act.getLinkId());
			}
			else {
				scheduler = linkIdToScheduler.values().iterator().next();
			}
			assert(scheduler != null);
			new Vehicle(scheduler, person, activityDurationInterpretation);
		}
	}

    protected Scheduler getScheduler() {
        return linkIdToScheduler.values().iterator().next();
    }

    public EventsManager getEvents() {
		return events;
	}

	public JDEQSimConfigGroup getConfig() {
		return config;
	}

}
