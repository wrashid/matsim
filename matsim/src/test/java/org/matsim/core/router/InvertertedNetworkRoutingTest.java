/* *********************************************************************** *
 * project: org.matsim.*
 * InvertertedNetworkLegRouterTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.core.router;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.algorithms.NetworkTurnInfoBuilder;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.costcalculators.LinkToLinkRandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.LinkToLinkTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LinkToLinkLeastCostPathCalculatorFactory;
import org.matsim.core.router.util.LinkToLinkTravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.Facility;


/**
 * Tests the routing under consideration of different link to link travel times (turning moves)
 * @author dgrether
 *
 */
public class InvertertedNetworkRoutingTest {

	@Test
	public void testInvertedNetworkLegRouter() {
		Fixture f = new Fixture();
		LinkToLinkTravelTimeStub tt = new LinkToLinkTravelTimeStub();
		TravelDisutilityFactory tc = new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car, f.s.getConfig().planCalcScore() );
		LeastCostPathCalculatorFactory lcpFactory = new DijkstraFactory();

		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
        Facility<?> fromFacility = new LinkWrapperFacility(//
                f.s.getNetwork().getLinks().get(Id.create("12", Link.class)));
        Facility<?> toFacility = new LinkWrapperFacility(//
                f.s.getNetwork().getLinks().get(Id.create("78", Link.class)));

		LinkToLinkRoutingModule router =
				new LinkToLinkRoutingModule(
						"mode",
						f.s.getPopulation().getFactory(),
						f.s.getNetwork(), lcpFactory,tc, tt, new NetworkTurnInfoBuilder(f.s));
		//test 1
		tt.setTurningMoveCosts(0.0, 100.0, 50.0);
		
		NetworkRoute route = calcRoute(router, fromFacility, toFacility, person);
		Assert.assertNotNull(route);
		Assert.assertEquals(Id.create("12", Link.class), route.getStartLinkId());
		Assert.assertEquals(Id.create("78", Link.class), route.getEndLinkId());
		Assert.assertEquals(3, route.getLinkIds().size());
		Assert.assertEquals(Id.create("23", Link.class), route.getLinkIds().get(0));
		Assert.assertEquals(Id.create("34", Link.class), route.getLinkIds().get(1));
		Assert.assertEquals(Id.create("47", Link.class), route.getLinkIds().get(2));
		
		//test 2
		tt.setTurningMoveCosts(100.0, 0.0, 50.0);
        route = calcRoute(router, fromFacility, toFacility, person);
		Assert.assertNotNull(route);
		Assert.assertEquals(Id.create("12", Link.class), route.getStartLinkId());
		Assert.assertEquals(Id.create("78", Link.class), route.getEndLinkId());
		Assert.assertEquals(3, route.getLinkIds().size());
		Assert.assertEquals(Id.create("23", Link.class), route.getLinkIds().get(0));
		Assert.assertEquals(Id.create("35", Link.class), route.getLinkIds().get(1));
		Assert.assertEquals(Id.create("57", Link.class), route.getLinkIds().get(2));
		
		//test 3
		tt.setTurningMoveCosts(50.0, 100.0, 0.0);
		
        route = calcRoute(router, fromFacility, toFacility, person);
		Assert.assertNotNull(route);
		Assert.assertEquals(Id.create("12", Link.class), route.getStartLinkId());
		Assert.assertEquals(Id.create("78", Link.class), route.getEndLinkId());
		Assert.assertEquals(3, route.getLinkIds().size());
		Assert.assertEquals(Id.create("23", Link.class), route.getLinkIds().get(0));
		Assert.assertEquals(Id.create("36", Link.class), route.getLinkIds().get(1));
		Assert.assertEquals(Id.create("67", Link.class), route.getLinkIds().get(2));
	}

	@Test
	public void testLinkToLinkRouter() {
		Fixture f = new Fixture();
		final LinkToLinkTravelTimeStub tt = new LinkToLinkTravelTimeStub();
		final LinkToLinkTravelDisutilityFactory tc = new LinkToLinkRandomizingTimeDistanceTravelDisutilityFactory(TransportMode.car, f.s.getConfig().planCalcScore());
		
		List<LinkToLinkLeastCostPathCalculatorFactory> algos = new ArrayList<>();
		algos.add(new LinkToLinkFastDijkstraFactory());
		algos.add(new LinkToLinkFastDijkstraFactory(true));
		algos.add(new LinkToLinkFastAStarEuclideanFactory());
		algos.add(new LinkToLinkFastAStarLandmarksFactory());

		for (LinkToLinkLeastCostPathCalculatorFactory algo : algos) {
			System.out.println("testing " + algo.getClass());
			
			LinkToLinkRoutingModuleV2 router = new LinkToLinkRoutingModuleV2("car", f.s.getPopulation().getFactory(), f.s.getNetwork(), f.s.getLanes(), algo, tc, tt);
			
			Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
			Facility<?> fromFacility = new LinkWrapperFacility(//
					f.s.getNetwork().getLinks().get(Id.create("12", Link.class)));
			Facility<?> toFacility = new LinkWrapperFacility(//
					f.s.getNetwork().getLinks().get(Id.create("78", Link.class)));
			
			//test 1
			tt.setTurningMoveCosts(0.0, 100.0, 50.0);
			
			NetworkRoute route = calcLinkToLinkRoute(router, fromFacility, toFacility, person);
			Assert.assertNotNull(route);
			Assert.assertEquals(Id.create("12", Link.class), route.getStartLinkId());
			Assert.assertEquals(Id.create("78", Link.class), route.getEndLinkId());
			Assert.assertEquals(3, route.getLinkIds().size());
			Assert.assertEquals(Id.create("23", Link.class), route.getLinkIds().get(0));
			Assert.assertEquals(Id.create("34", Link.class), route.getLinkIds().get(1));
			Assert.assertEquals(Id.create("47", Link.class), route.getLinkIds().get(2));
			
			//test 2
			tt.setTurningMoveCosts(100.0, 0.0, 50.0);
			route = calcLinkToLinkRoute(router, fromFacility, toFacility, person);
			Assert.assertNotNull(route);
			Assert.assertEquals(Id.create("12", Link.class), route.getStartLinkId());
			Assert.assertEquals(Id.create("78", Link.class), route.getEndLinkId());
			Assert.assertEquals(3, route.getLinkIds().size());
			Assert.assertEquals(Id.create("23", Link.class), route.getLinkIds().get(0));
			Assert.assertEquals(Id.create("35", Link.class), route.getLinkIds().get(1));
			Assert.assertEquals(Id.create("57", Link.class), route.getLinkIds().get(2));
			
			//test 3
			tt.setTurningMoveCosts(50.0, 100.0, 0.0);
			
			route = calcLinkToLinkRoute(router, fromFacility, toFacility, person);
			Assert.assertNotNull(route);
			Assert.assertEquals(Id.create("12", Link.class), route.getStartLinkId());
			Assert.assertEquals(Id.create("78", Link.class), route.getEndLinkId());
			Assert.assertEquals(3, route.getLinkIds().size());
			Assert.assertEquals(Id.create("23", Link.class), route.getLinkIds().get(0));
			Assert.assertEquals(Id.create("36", Link.class), route.getLinkIds().get(1));
			Assert.assertEquals(Id.create("67", Link.class), route.getLinkIds().get(2));
		}	
	}
	
	private NetworkRoute calcRoute(LinkToLinkRoutingModule router, final Facility<?> fromFacility,
            final Facility<?> toFacility, final Person person) {
        Leg leg = (Leg)router.calcRoute(fromFacility, toFacility, 0.0, person).get(0);
        return (NetworkRoute) leg.getRoute();
	}
	
	private NetworkRoute calcLinkToLinkRoute(LinkToLinkRoutingModuleV2 router, final Facility<?> fromFacility,
            final Facility<?> toFacility, final Person person) {
        Leg leg = (Leg)router.calcRoute(fromFacility, toFacility, 0.0, person).get(0);
        return (NetworkRoute) leg.getRoute();
	}
	
	private static class LinkToLinkTravelTimeStub implements LinkToLinkTravelTime {

		private double turningMoveCosts34;
		private double turningMoveCosts35;
		private double turningMoveCosts36;

		public void setTurningMoveCosts(double link34, double link35, double link36) {
			this.turningMoveCosts34 = link34;
			this.turningMoveCosts35 = link35;
			this.turningMoveCosts36 = link36;
		}

		@Override
		public double getLinkToLinkTravelTime(Link fromLink, Link toLink, double time) {
			double tt = fromLink.getLength() / fromLink.getFreespeed(time);
			if (Id.create("34", Link.class).equals(toLink.getId())){
				tt = tt + this.turningMoveCosts34;
			}
			else if (Id.create("35", Link.class).equals(toLink.getId())){
				tt = tt + this.turningMoveCosts35;
			}
			else if (Id.create("36", Link.class).equals(toLink.getId())){
				tt = tt + this.turningMoveCosts36;
			}
			return tt;
		}

	}
	
	private static class Fixture {
		public final Scenario s = ScenarioUtils.createScenario(ConfigUtils.createConfig());

		public Fixture() {
			Network net = this.s.getNetwork();
			NetworkFactory nf = net.getFactory();
			Node n1 = nf.createNode(Id.createNodeId("1"), new Coord(0, 0));
			Node n2 = nf.createNode(Id.createNodeId("2"), new Coord(0, 1000));
			Node n3 = nf.createNode(Id.createNodeId("3"), new Coord(0, 2000));
			Node n4 = nf.createNode(Id.createNodeId("4"), new Coord(500, 3000));
			Node n5 = nf.createNode(Id.createNodeId("5"), new Coord(0, 3000));
			Node n6 = nf.createNode(Id.createNodeId("6"), new Coord(-500, 3000));
			Node n7 = nf.createNode(Id.createNodeId("7"), new Coord(0, 4000));
			Node n8 = nf.createNode(Id.createNodeId("8"), new Coord(0, 5000));
			net.addNode(n1);
			net.addNode(n2);
			net.addNode(n3);
			net.addNode(n4);
			net.addNode(n5);
			net.addNode(n6);
			net.addNode(n7);
			net.addNode(n8);
			Link l12 = nf.createLink(Id.createLinkId("12"), n1, n2);
			Link l23 = nf.createLink(Id.createLinkId("23"), n2, n3);
			Link l34 = nf.createLink(Id.createLinkId("34"), n3, n4);
			Link l35 = nf.createLink(Id.createLinkId("35"), n3, n5);
			Link l36 = nf.createLink(Id.createLinkId("36"), n3, n6);
			Link l47 = nf.createLink(Id.createLinkId("47"), n4, n7);
			Link l57 = nf.createLink(Id.createLinkId("57"), n5, n7);
			Link l67 = nf.createLink(Id.createLinkId("67"), n6, n7);
			Link l78 = nf.createLink(Id.createLinkId("78"), n7, n8);
			l12.setFreespeed(10.0);
			l12.setLength(1000.0);
			l23.setFreespeed(10.0);
			l23.setLength(1000.0);
			l34.setFreespeed(10.0);
			l34.setLength(1000.0);
			l35.setFreespeed(10.0);
			l35.setLength(1000.0);
			l36.setFreespeed(10.0);
			l36.setLength(1000.0);
			l47.setFreespeed(10.0);
			l47.setLength(1000.0);
			l57.setFreespeed(10.0);
			l57.setLength(1000.0);
			l67.setFreespeed(10.0);
			l67.setLength(1000.0);
			l78.setFreespeed(10.0);
			l78.setLength(1000.0);
			net.addLink(l12);
			net.addLink(l23);
			net.addLink(l34);
			net.addLink(l35);
			net.addLink(l36);
			net.addLink(l47);
			net.addLink(l57);
			net.addLink(l67);
			net.addLink(l78);
		}
	}
}