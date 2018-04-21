/* *********************************************************************** *
 * project: org.matsim.*
 * ReceiverUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package org.matsim.contrib.freight.usecases.receiver;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.freight.carrier.Carrier;
import org.matsim.contrib.freight.carrier.CarrierCapabilities;
import org.matsim.contrib.freight.carrier.CarrierCapabilities.FleetSize;
import org.matsim.contrib.freight.carrier.CarrierImpl;
import org.matsim.contrib.freight.carrier.CarrierPlan;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeWriter;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.io.ReceiversWriter;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.receiver.Order;
import org.matsim.contrib.freight.receiver.ProductType;
import org.matsim.contrib.freight.receiver.Receiver;
import org.matsim.contrib.freight.receiver.ReceiverImpl;
import org.matsim.contrib.freight.receiver.ReceiverOrder;
import org.matsim.contrib.freight.receiver.ReceiverProduct;
import org.matsim.contrib.freight.receiver.Receivers;
import org.matsim.contrib.freight.receiver.reorderPolicy.SSReorderPolicy;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.VehicleType;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.io.algorithm.VehicleRoutingAlgorithms;

/**
 * Various utilities for building receiver scenarios (for now).
 * 
 * @author jwjoubert, wlbean
 */
public class ReceiverChessboardScenario {
	private final static Logger LOG = Logger.getLogger(ReceiverChessboardScenario.class);

	/**
	 * Build the entire chessboard example.
	 */
	public static void createChessboardScenario(long seed, int run) {
		Scenario sc = setupChessboardScenario(seed, run);
		Receivers receivers = createChessboardReceivers(sc);
		Carriers carriers = createChessboardCarriers(sc);
		createReceiverOrders(receivers, carriers);

		//		new CarrierPlanXmlWriterV2(carriers).write("/Users/jwjoubert/Downloads/carrierFile.xml");

		/* Let jsprit do its magic and route the given receiver orders. */
		generateCarrierPlan(carriers, sc.getNetwork());

		/* Write the necessary bits to file. */
		String outputFolder = sc.getConfig().controler().getOutputDirectory();
		outputFolder += outputFolder.endsWith("/") ? "" : "/";
		new File(outputFolder).mkdirs();

		new ConfigWriter(sc.getConfig()).write(outputFolder + "config.xml");
		new CarrierPlanXmlWriterV2(carriers).write(outputFolder + "carriers.xml");
		new ReceiversWriter(receivers).write(outputFolder + "receivers.xml");
	}

	/**
	 * Route the services that are allocated to the carrier.
	 * 
	 * @param carriers
	 * @param network
	 */
	public static void generateCarrierPlan(Carriers carriers, Network network) {
		Carrier carrier = carriers.getCarriers().get(Id.create("Carrier1", Carrier.class)); 

		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, network);

		NetworkBasedTransportCosts netBasedCosts = NetworkBasedTransportCosts.Builder.newInstance(network, carrier.getCarrierCapabilities().getVehicleTypes()).build();
		VehicleRoutingProblem vrp = vrpBuilder.setRoutingCost(netBasedCosts).build();

		//read and create a pre-configured algorithms to solve the vrp
		VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, "./input/usecases/chessboard/vrpalgo/initialPlanAlgorithm.xml");

		//solve the problem
		Collection<VehicleRoutingProblemSolution> solutions = vra.searchSolutions();

		//get best (here, there is only one)
		VehicleRoutingProblemSolution solution = null;

		Iterator<VehicleRoutingProblemSolution> iterator = solutions.iterator();

		while(iterator.hasNext()){
			solution = iterator.next();
		}

		//create a carrierPlan from the solution 
		CarrierPlan plan = MatsimJspritFactory.createPlan(carrier, solution);

		//route plan 
		NetworkRouter.routePlan(plan, netBasedCosts);


		//assign this plan now to the carrier and make it the selected carrier plan
		carrier.setSelectedPlan(plan);

		//write out the carrierPlan to an xml-file
		//		new CarrierPlanXmlWriterV2(carriers).write(directory + "/input/carrierPlanned.xml");
	}


	public static void createReceiverOrders(Receivers receivers, Carriers carriers) {
		Carrier carrierOne = carriers.getCarriers().get(Id.create("Carrier1", Carrier.class));

		/* Create generic product types */
		ProductType productTypeOne = receivers.createAndAddProductType(Id.create("P1", ProductType.class));
		productTypeOne.setProductDescription("Product 1");
		productTypeOne.setRequiredCapacity(5.0);

		ProductType productTypeTwo = receivers.createAndAddProductType(Id.create("P2", ProductType.class));
		productTypeTwo.setProductDescription("Product 2");
		productTypeTwo.setRequiredCapacity(10.0);

		/* Create receiver-specific products */
		Receiver receiverOne = receivers.getReceivers().get(Id.create("1", Receiver.class));
		ReceiverProduct receiverOneProductOne = createReceiverProduct(receiverOne, productTypeOne, 200, 1000);
		ReceiverProduct receiverOneProductTwo = createReceiverProduct(receiverOne, productTypeTwo, 800, 2000);
		receiverOne.getProducts().add(receiverOneProductOne);
		receiverOne.getProducts().add(receiverOneProductTwo);

		Receiver receiverTwo = receivers.getReceivers().get(Id.create("2", Receiver.class));
		ReceiverProduct receiverTwoProductOne = createReceiverProduct(receiverTwo, productTypeOne, 200, 800);
		ReceiverProduct receiverTwoProductTwo = createReceiverProduct(receiverTwo, productTypeTwo, 600, 1500);
		receiverTwo.getProducts().add(receiverTwoProductOne);
		receiverTwo.getProducts().add(receiverTwoProductTwo);

		/* Generate and collate orders for the different receiver/order combination. */

		/* -----  Receiver 1. ----- */
		Order r1order1 = createProductOrder(Id.create("Order1",  Order.class), receiverOne, 
				receiverOneProductOne, Time.parseTime("00:30:00"));
		Order r1order2 = createProductOrder(Id.create("Order2",  Order.class), receiverOne, 
				receiverOneProductTwo, Time.parseTime("00:30:00"));
		Collection<Order> r1orders = new ArrayList<Order>();
		r1orders.add(r1order1);
		r1orders.add(r1order2);

		/* Combine product orders into single receiver order for a specific carrier. */
		ReceiverOrder receiver1order = new ReceiverOrder(receiverOne.getId(), r1orders, carrierOne.getId());
		receiverOne.setSelectedPlan(receiver1order);

		/* Convert receiver orders to initial carrier services. */
		for(Order order : receiver1order.getReceiverOrders()){
			org.matsim.contrib.freight.carrier.CarrierService.Builder serBuilder = CarrierService.
					Builder.newInstance(Id.create(order.getId(),CarrierService.class), order.getReceiver().getLinkId());
			
			if(receiverOne.getTimeWindows().size() > 1) {
				LOG.warn("Multiple time windows set. Only the first is used");
			}
			CarrierService newService = serBuilder.setCapacityDemand(order.getOrderQuantity()).
					setServiceStartTimeWindow(receiverOne.getTimeWindows().get(0)).
					setServiceDuration(order.getServiceDuration()).
					build();
			carriers.getCarriers().get(receiver1order.getCarrierId()).getServices().add(newService);		
		}	


		/* -----  Receiver 2. ----- */
		Order r2order1 = createProductOrder(Id.create("Order3",  Order.class), receiverTwo, 
				receiverTwoProductOne, Time.parseTime("01:00:00"));
		Order r2order2 = createProductOrder(Id.create("Order4",  Order.class), receiverTwo, 
				receiverTwoProductTwo, Time.parseTime("01:00:00"));
		Collection<Order> r2orders = new ArrayList<Order>();
		r2orders.add(r2order1);
		r2orders.add(r2order2);

		/* Combine product orders into single receiver order for a specific carrier. */
		ReceiverOrder receiver2order = new ReceiverOrder(receiverTwo.getId(), r2orders, carrierOne.getId());
		receiverTwo.setSelectedPlan(receiver2order);

		/* Convert receiver orders to initial carrier services. */
		for(Order order : receiver2order.getReceiverOrders()){
			org.matsim.contrib.freight.carrier.CarrierService.Builder serBuilder = CarrierService.
					Builder.newInstance(Id.create(order.getId(),CarrierService.class), order.getReceiver().getLinkId());
			
			if(receiverTwo.getTimeWindows().size() > 1) {
				LOG.warn("Multiple time windows set. Only the first is used");
			}
			CarrierService newService = serBuilder.
					setCapacityDemand(order.getOrderQuantity()).
					setServiceStartTimeWindow(receiverTwo.getTimeWindows().get(0)).
					setServiceDuration(order.getServiceDuration()).
					build();
			carriers.getCarriers().get(receiver2order.getCarrierId()).getServices().add(newService);		
		}
	}


	/**
	 * FIXME Need to complete this. 
	 * @return
	 */
	public static Scenario setupChessboardScenario(long seed, int run) {
		Config config = ConfigUtils.createConfig();
		config.controler().setFirstIteration(0);
		config.controler().setLastIteration(20);
		config.controler().setMobsim("qsim");
		config.controler().setWriteSnapshotsInterval(1);
		config.global().setRandomSeed(seed);
		config.network().setInputFile("./input/usecases/chessboard/network/grid9x9.xml");
		config.controler().setOutputDirectory(String.format("./output/run_%03d/", run));

		Scenario sc = ScenarioUtils.loadScenario(config);
		return sc;
	}


	public static Receivers createChessboardReceivers(Scenario sc) {
		Network network = sc.getNetwork();

		/* Create first receiver */
		Id<Link> receiverOneLocation = selectRandomLink(network);
		Receiver receiverOne = ReceiverImpl.newInstance(Id.create("1", Receiver.class))
				.setLinkId(receiverOneLocation)
				.addTimeWindow(TimeWindow.newInstance(Time.parseTime("10:00"), Time.parseTime("14:00")));
		/* FIXME Add a toString() method. */
		
		/* Create second receiver */
		Id<Link> receiverTwoLocation = selectRandomLink(network);
		Receiver receiverTwo = ReceiverImpl.newInstance(Id.create("2", Receiver.class))
				.setLinkId(receiverTwoLocation)
				.addTimeWindow(TimeWindow.newInstance(Time.parseTime("08:00"), Time.parseTime("12:00")));

		Receivers receivers = new Receivers();
		
		receivers.setDescription("Chessboard");
		/* Add a dummy example attribute */
		receivers.getAttributes().putAttribute("date", "2018/04/20");
		
		receivers.addReceiver(receiverOne);
		receivers.addReceiver(receiverTwo);
		return receivers;
	}

	public static Carriers createChessboardCarriers(Scenario sc) {
		Id<Carrier> carrierId = Id.create("Carrier1", Carrier.class);
		Carrier carrier = CarrierImpl.newInstance(carrierId);
		Id<Link> carrierLocation = selectRandomLink(sc.getNetwork());

		org.matsim.contrib.freight.carrier.CarrierCapabilities.Builder capBuilder = CarrierCapabilities.Builder.newInstance();
		CarrierCapabilities carrierCap = capBuilder.setFleetSize(FleetSize.INFINITE).build();
		carrier.setCarrierCapabilities(carrierCap);						
		LOG.info("Created a carrier with capabilities.");	

		/*
		 * Create the carrier vehicle types. 
		 * TODO This might, potentially, be read from XML file. 
		 */

		/* Heavy vehicle. */
		org.matsim.contrib.freight.carrier.CarrierVehicleType.Builder typeBuilderHeavy = CarrierVehicleType.Builder.newInstance(Id.create("heavy", VehicleType.class));
		CarrierVehicleType typeHeavy = typeBuilderHeavy.setCapacity(14000).setFixCost(2604).setCostPerDistanceUnit(7.34E-3).setCostPerTimeUnit(0.171).build();
		org.matsim.contrib.freight.carrier.CarrierVehicle.Builder carrierHVehicleBuilder = CarrierVehicle.Builder.newInstance(Id.createVehicleId("heavy"), carrierLocation);
		CarrierVehicle heavy = carrierHVehicleBuilder.setEarliestStart(Time.parseTime("06:00:00")).setLatestEnd(Time.parseTime("18:00:00")).setType(typeHeavy).setTypeId(typeHeavy.getId()).build();

		/* Light vehicle. */
		org.matsim.contrib.freight.carrier.CarrierVehicleType.Builder typeBuilderLight = CarrierVehicleType.Builder.newInstance(Id.create("light", VehicleType.class));
		CarrierVehicleType typeLight = typeBuilderLight
				.setCapacity(3000).setFixCost(1168)
				.setCostPerDistanceUnit(4.22E-3)
				.setCostPerTimeUnit(0.089)
				.build();
		org.matsim.contrib.freight.carrier.CarrierVehicle.Builder carrierLVehicleBuilder = CarrierVehicle.Builder.newInstance(Id.createVehicleId("light"), carrierLocation);
		CarrierVehicle light = carrierLVehicleBuilder
				.setEarliestStart(Time.parseTime("06:00:00"))
				.setLatestEnd(Time.parseTime("18:00:00"))
				.setType(typeLight)
				.setTypeId(typeLight.getId())
				.build();

		/* Assign vehicles to carrier. */
		carrier.getCarrierCapabilities().getCarrierVehicles().add(heavy);
		carrier.getCarrierCapabilities().getVehicleTypes().add(typeHeavy);
		carrier.getCarrierCapabilities().getCarrierVehicles().add(light);	
		carrier.getCarrierCapabilities().getVehicleTypes().add(typeLight);
		LOG.info("Added different vehicle types to the carrier.");

		CarrierVehicleTypes types = new CarrierVehicleTypes();
		types.getVehicleTypes().put(typeLight.getId(), typeLight);
		types.getVehicleTypes().put(typeHeavy.getId(), typeHeavy);
		String folder = sc.getConfig().controler().getOutputDirectory();
		folder += folder.endsWith("/") ? "" : "/";
		new CarrierVehicleTypeWriter(types).write(folder + "carrierVehicleTypes.xml");

		Carriers carriers = new Carriers();
		carriers.addCarrier(carrier);
		return carriers;
	}


	/**
	 * Selects a random link in the network.
	 * @param network
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static Id<Link> selectRandomLink(Network network){
		Object[] linkIds = network.getLinks().keySet().toArray();
		int sample = MatsimRandom.getRandom().nextInt(linkIds.length);
		Object o = linkIds[sample];
		//System.out.println("random link "+ o);
		Id<Link> linkId = null;
		if(o instanceof Id<?>){
			linkId = (Id<Link>) o;
			return linkId;
		} else{
			throw new RuntimeException("Oops, cannot find a correct link Id.");
		}
	}


	/**
	 * This method assigns a specific product type to a receiver, and allocates that receiver's order
	 * policy.
	 * 
	 * TODO This must be made more generic so that multiple policies can be considered. Currently (2018/04
	 * this is hard-coded to be a min-max (s,S) reordering policy.
	 *  
	 * @param receiver
	 * @param productType
	 * @param minLevel
	 * @param maxLevel
	 * @return 
	 */
	private static ReceiverProduct createReceiverProduct(Receiver receiver, ProductType productType, int minLevel, int maxLevel) {
		ReceiverProduct.Builder builder = ReceiverProduct.Builder.newInstance();
		ReceiverProduct rProd = builder.setReorderingPolicy(new SSReorderPolicy(minLevel, maxLevel)).setProductType(productType).build();
		return rProd;
	}

	/**
	 * Create a receiver order for different products.
	 * @param number
	 * @param receiver
	 * @param receiverProduct
	 * @param serviceTime
	 * @return
	 */
	private static Order createProductOrder(Id<Order> number, Receiver receiver, ReceiverProduct receiverProduct, double serviceTime) {
		Order.Builder builder = Order.Builder.newInstance(number, receiver, receiverProduct);
		Order order = builder
				.calculateOrderQuantity()
				.setServiceTime(serviceTime)
				.build();
//		LOG.info("Created an order of type " + order.getOrderName() 
//		+ " for receiver " + order.getReceiver().getId() + " with order quantity of " 
//		+ order.getOrderQuantity() + " tonnes, to " + order.getReceiver().getLinkId().toString() 
//		+ ", and service duaration of " + Time.writeTime(order.getServiceDuration()) + ".");
		return order;
	}


}
