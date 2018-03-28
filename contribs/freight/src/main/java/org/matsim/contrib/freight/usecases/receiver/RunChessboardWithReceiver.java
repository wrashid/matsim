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
import org.matsim.contrib.freight.carrier.CarrierPlanXmlReaderV2;
import org.matsim.contrib.freight.carrier.CarrierPlanXmlWriterV2;
import org.matsim.contrib.freight.carrier.CarrierService;
import org.matsim.contrib.freight.carrier.CarrierVehicle;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeLoader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeReader;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypeWriter;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.contrib.freight.carrier.Carriers;
import org.matsim.contrib.freight.carrier.TimeWindow;
import org.matsim.contrib.freight.controler.CarrierModule;
import org.matsim.contrib.freight.jsprit.MatsimJspritFactory;
import org.matsim.contrib.freight.jsprit.NetworkBasedTransportCosts;
import org.matsim.contrib.freight.jsprit.NetworkRouter;
import org.matsim.contrib.freight.receiver.Order;
import org.matsim.contrib.freight.receiver.ProductType;
import org.matsim.contrib.freight.receiver.ProductTypeImpl;
import org.matsim.contrib.freight.receiver.Receiver;
import org.matsim.contrib.freight.receiver.ReceiverCharacteristics;
import org.matsim.contrib.freight.receiver.ReceiverImpl;
import org.matsim.contrib.freight.receiver.ReceiverModule;
import org.matsim.contrib.freight.receiver.ReceiverOrder;
import org.matsim.contrib.freight.receiver.ReceiverOrderStrategyManagerFactory;
import org.matsim.contrib.freight.receiver.ReceiverProduct;
import org.matsim.contrib.freight.receiver.ReceiverProductType;
import org.matsim.contrib.freight.receiver.ReceiverScoringFunctionFactory;
import org.matsim.contrib.freight.receiver.Receivers;
import org.matsim.contrib.freight.replanning.CarrierPlanStrategyManagerFactory;
import org.matsim.contrib.freight.replanning.modules.ReRouteVehicles;
import org.matsim.contrib.freight.replanning.modules.TimeAllocationMutator;
import org.matsim.contrib.freight.scoring.CarrierScoringFunctionFactory;
import org.matsim.contrib.freight.usecases.analysis.CarrierScoreStats;
import org.matsim.contrib.freight.usecases.analysis.LegHistogram;
import org.matsim.contrib.freight.usecases.chessboard.TravelDisutilities;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.controler.events.IterationEndsEvent;
import org.matsim.core.controler.listener.IterationEndsListener;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.replanning.GenericPlanStrategyImpl;
import org.matsim.core.replanning.GenericStrategyManager;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.KeepSelected;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.misc.Time;
import org.matsim.vehicles.VehicleType;

import com.graphhopper.jsprit.core.algorithm.VehicleRoutingAlgorithm;
import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.solution.VehicleRoutingProblemSolution;
import com.graphhopper.jsprit.io.algorithm.VehicleRoutingAlgorithms;

/**
 * This is an instance where the chessboard network is used to illustrate how the receiver is included into MATSim.	This basic example only adds one receiver with two product types and one order for each type.

 * @author wlbean
 * 
 */

public class RunChessboardWithReceiver {
	private final static Logger LOG = Logger.getLogger(RunChessboardWithReceiver.class);
	
	/**
	 * 	 @param args
	 */
	
	public static void main(String[] args) {
		LOG.info("===== Start =====");
		RunChessboardWithReceiver.run(args);
		LOG.info("===== Done ======");
	}
	
	public static void run(String[] args){
		
		/*
		 * Input parameters to be included here.
		 */
		
		
		final int runs = Integer.parseInt(args[0]);
		long seed = Long.parseLong(args[1]);
		final String directory = args[2];
						
		
		for(int n=0;n<runs;n++) {
			final int run = n;
		
			
			/*
			 * Config stuff.
			 */
			
			Config config = ConfigUtils.createConfig();
			Scenario scReceiver = ScenarioUtils.createScenario(config);
			config.controler().setFirstIteration(0);
			config.controler().setLastIteration(20);
			config.controler().setMobsim("qsim");
			config.controler().setWriteSnapshotsInterval(1);
			config.global().setRandomSeed(seed);
			config.network().setInputFile(directory + "Input/grid9x9.xml");
										
			/*
			 * Sets the output directory.
			 */
			
			final String outputDir = String.format(directory + "Output/test.output.%03d.run/", run);
			createOutputDir(outputDir);
	
			config.controler().setOutputDirectory(outputDir);
			config.controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.overwriteExistingFiles);
			new MatsimNetworkReader(scReceiver.getNetwork()).readFile(directory + "Input/grid9x9.xml");
			
			Controler controler = new Controler(config);
					
			/*
			 * Create the receiver containter.
			 */
			
			Receivers receivers = new Receivers();
				
			
			/*
			 * Create new receivers.
			 */
				//Receiver 1
			Id<Link> receiverLocation1 = selectRandomLink(scReceiver.getNetwork());
				ReceiverCharacteristics newreceiverChar1 = createReceiver(receiverLocation1, Time.parseTime("10:00"), Time.parseTime("14:00"));
				Receiver newreceiver1 = null;
				newreceiver1 = ReceiverImpl.newInstance(Id.create("Receiver", Receiver.class));
				newreceiver1.setReceiverCharacteristics(newreceiverChar1);
				System.out.println("Created receiver " + newreceiver1.getId() + " at location " + newreceiver1.getReceiverCharacteristics().getLocation() + " with a delivery time window start time " + Time.writeTime(newreceiver1.getReceiverCharacteristics().getTimeWindowStart()) + " and delivery time window end " + Time.writeTime(newreceiver1.getReceiverCharacteristics().getTimeWindowEnd()) + " .");
								
				
				//Receiver 2
				Id<Link> receiverLocation2 = selectRandomLink(scReceiver.getNetwork());
				ReceiverCharacteristics newreceiverChar2 = createReceiver(receiverLocation2, Time.parseTime("8:00"), Time.parseTime("12:00"));
				Receiver newreceiver2 = null;
				newreceiver2 = ReceiverImpl.newInstance(Id.create("Receiver", Receiver.class));
				newreceiver2.setReceiverCharacteristics(newreceiverChar2);
				System.out.println("Created receiver " + newreceiver2.getId() + " at location " + newreceiver2.getReceiverCharacteristics().getLocation() + " with a delivery time window start time " + Time.writeTime(newreceiver2.getReceiverCharacteristics().getTimeWindowStart()) + " and delivery time window end " + Time.writeTime(newreceiver2.getReceiverCharacteristics().getTimeWindowEnd()) + " .");
					

			
			/*
			 * Creating generic product types.
			 */

			ProductType productType1 = createProductType(Id.create("P1", ProductType.class), "Product 1", 5);
			ProductType productType2 = createProductType(Id.create("P2", ProductType.class), "Product 2", 10);
			
			/*
			 * Creating product types ordered by a specific receivers.
			 */

				ReceiverProductType rProductType1 = createReceiverProductType(productType1);
				ReceiverProductType rProductType2 = createReceiverProductType(productType2);
				
				
			/*
			 * Creating receiver specific products with allocated delivery locations and order policy parameters.
			 */
			
				//Receiver 1.
				ReceiverProduct r1product1 = createReceiverProduct(receiverLocation1, rProductType1, 1000, 200);
				ReceiverProduct r1product2 = createReceiverProduct(receiverLocation1, rProductType2, 2000, 800);
				
				//Receiver 2.
				ReceiverProduct r2product1 = createReceiverProduct(receiverLocation2, rProductType1, 800, 200);
				ReceiverProduct r2product2 = createReceiverProduct(receiverLocation2, rProductType2, 1500, 600);
			
			/*
			 * Creating orders for each receiver product.
			 */
			
				//Receiver 1.
				Order r1order1 = createProductOrder(Id.create("Order1",  Order.class), newreceiver1, r1product1, Time.parseTime("00:30:00"));
				Order r1order2 = createProductOrder(Id.create("Order2",  Order.class), newreceiver1, r1product2, Time.parseTime("00:30:00"));
				
				//Receiver 2.
				Order r2order1 = createProductOrder(Id.create("Order3",  Order.class), newreceiver2, r2product1, Time.parseTime("01:00:00"));
				Order r2order2 = createProductOrder(Id.create("Order4",  Order.class), newreceiver2, r2product2, Time.parseTime("01:00:00"));
			
			/*
			 * Creating a single collection of orders.
			 */
				
				//Receiver 1.
				Collection<Order> r1orders = new ArrayList<Order>();
				r1orders.add(r1order1);
				r1orders.add(r1order2);
				
				//Receiver 2.
				Collection<Order> r2orders = new ArrayList<Order>();
				r2orders.add(r2order1);
				r2orders.add(r2order2);
				
		
			/**
			 * Creates a carrier xml file with the receiver order.
			 */
			
				/*
				 * Create the carrier container.
				 */
				
				Carriers carriers = new Carriers();
	
				Id<Link> carrierLocation = selectRandomLink(scReceiver.getNetwork());
				
				/*
				 * Create a new carrier with a fleet.
				 */
				
				Carrier carrier = CarrierImpl.newInstance(Id.create("Carrier1",Carrier.class));
				Id<Carrier> index = carrier.getId();
				
				org.matsim.contrib.freight.carrier.CarrierCapabilities.Builder capBuilder = CarrierCapabilities.Builder.newInstance();
				
				CarrierCapabilities carrierCap = capBuilder.setFleetSize(FleetSize.INFINITE).build();
				
				carrier.setCarrierCapabilities(carrierCap);						
				System.out.println("Created a carrier with capabilities.");	
						
				/*
				 * Create the carrier vehicle types.
				 */
				
				
					/*
					 * Heavy vehicle.
					 */
					
					org.matsim.contrib.freight.carrier.CarrierVehicleType.Builder typeBuilderHeavy = CarrierVehicleType.Builder.newInstance(Id.create("heavy", VehicleType.class));
					
					CarrierVehicleType typeHeavy = typeBuilderHeavy.setCapacity(14000).setFixCost(2604).setCostPerDistanceUnit(7.34E-3).setCostPerTimeUnit(0.171).build();
					
					org.matsim.contrib.freight.carrier.CarrierVehicle.Builder carrierHVehicleBuilder = CarrierVehicle.Builder.newInstance(Id.createVehicleId("heavy"), carrierLocation);
								
					CarrierVehicle heavy = carrierHVehicleBuilder.setEarliestStart(6.00*3600).setLatestEnd(18.00*3600).setType(typeHeavy).setTypeId(typeHeavy.getId()).build();
					
					/*
					 * Light vehicle.
					 */
					
					org.matsim.contrib.freight.carrier.CarrierVehicleType.Builder typeBuilderLight = CarrierVehicleType.Builder.newInstance(Id.create("light", VehicleType.class));
					
					CarrierVehicleType typeLight = typeBuilderLight.setCapacity(3000).setFixCost(1168).setCostPerDistanceUnit(4.22E-3).setCostPerTimeUnit(0.089).build();
					
					org.matsim.contrib.freight.carrier.CarrierVehicle.Builder carrierLVehicleBuilder = CarrierVehicle.Builder.newInstance(Id.createVehicleId("light"), carrierLocation);
					
					CarrierVehicle light = carrierLVehicleBuilder.setEarliestStart(6.00*3600).setLatestEnd(18.00*3600).setType(typeLight).setTypeId(typeLight.getId()).build();
				
				/*
				 * Assign vehicles to carrier.
				 */
					
				carrier.getCarrierCapabilities().getCarrierVehicles().add(heavy);
				
				carrier.getCarrierCapabilities().getCarrierVehicles().add(light);	
				
				System.out.println("Added different vehicle types to the carrier.");	
				
				/*
				 * Creating a single order for each receiver.
				 */
				
					//Receiver 1.
					ReceiverOrder receiver1order = new ReceiverOrder(newreceiver1, r1orders, carrier);
					
					//Receiver 2.
					ReceiverOrder receiver2order = new ReceiverOrder(newreceiver2, r2orders, carrier);
						
					newreceiver1.setSelectedPlan(receiver1order);
					newreceiver2.setSelectedPlan(receiver2order);
										
				/*
				* Add new receivers to the container.
				*/
				receivers.addReceiver(newreceiver1);
				receivers.addReceiver(newreceiver2);
					
									
				/*
				 *  Create new carrier services based on the receiver's order. This must be revised to include more than one carrier and more than one receiver.
				 */ 
				
				//Receiver 1.
				for(Order order : receiver1order.getReceiverOrders()){
					System.out.println(order);
				
					org.matsim.contrib.freight.carrier.CarrierService.Builder serBuilder = CarrierService.Builder.newInstance(Id.create(order.GetId(),CarrierService.class), order.getOrderDeliveryLocation());
					CarrierService newService = serBuilder.setCapacityDemand(order.getOrderQuantity()).setServiceStartTimeWindow(TimeWindow.newInstance(order.getOrderTimeWindowStart(), order.getOrderTimeWindowEnd())).setServiceDuration(order.getServiceDuration()).build();
					receiver1order.getOrderCarrier().getServices().add(newService);		
					}	
				
				//Receiver 2.
				for(Order order : receiver2order.getReceiverOrders()){
					System.out.println(order);
				
					org.matsim.contrib.freight.carrier.CarrierService.Builder serBuilder = CarrierService.Builder.newInstance(Id.create(order.GetId(),CarrierService.class), order.getOrderDeliveryLocation());
					CarrierService newService = serBuilder.setCapacityDemand(order.getOrderQuantity()).setServiceStartTimeWindow(TimeWindow.newInstance(order.getOrderTimeWindowStart(), order.getOrderTimeWindowEnd())).setServiceDuration(order.getServiceDuration()).build();
					receiver2order.getOrderCarrier().getServices().add(newService);		
					}
				
					
				/*
				 * Add carrier to carriers container.
				 */
				
				carriers.addCarrier(carrier);
				
				/*
				 * Write carrier plan to a new *.xml file.
				 */
					
				new CarrierPlanXmlWriterV2(carriers).write(directory + "/input/carrierFile.xml");
				
				
				
			/**
			 * 	Generate an initial carrier plan to deliver receiver orders.			
			 */
				CarrierVehicleTypes types = CarrierVehicleTypes.getVehicleTypes(carriers);
				new CarrierVehicleTypeWriter(types).write(directory + "/input/vehicleTypes.xml");
				
				carrierPlanGenerator(directory, scReceiver);
				
			/**
			 * Read initial carrier plan and 
			 */
				
				/*
				 * Read initial carrier plan.
				 */
				
				final Carriers finalCarriers = new Carriers();							
				
				new CarrierPlanXmlReaderV2(finalCarriers).readFile(directory + "/input/carrierPlanned.xml");		
							
				final CarrierVehicleTypes types2 = new CarrierVehicleTypes();
				new CarrierVehicleTypeReader(types2).readFile(directory + "/input/vehicleTypes.xml");
				
				new CarrierVehicleTypeLoader(finalCarriers).loadVehicleTypes(types2);
				
				
				/*
				 * Create a new instance of a carrier scoring function factory.
				 */
				
				final CarrierScoringFunctionFactory cScorFuncFac = new MyCarrierScoringFunctionFactoryImpl(scReceiver.getNetwork());
				
				/*
				 * Create a new instance of a carrier plan strategy manager factory.
				 */
				final CarrierPlanStrategyManagerFactory cStratManFac = new MyCarrierPlanStrategyManagerFactoryImpl(types2, scReceiver.getNetwork(), controler);
										
				CarrierModule carrierControler = new CarrierModule(finalCarriers, cStratManFac, cScorFuncFac);
				carrierControler.setPhysicallyEnforceTimeWindowBeginnings(true);
				controler.addOverridingModule(carrierControler);
				
				/*
				 * Create final receiver orders with new planned carrier.
				 */
				
				receiver1order.setOrderCarrier(finalCarriers.getCarriers().get(index));
				receiver2order.setOrderCarrier(finalCarriers.getCarriers().get(index));
				
				final Receivers finalReceivers = new Receivers();
				finalReceivers.addReceiver(newreceiver1);
				finalReceivers.addReceiver(newreceiver2);
				
				
				/*
				 * Create a new instance of a receiver scoring function factory.
				 */
				
				
				final ReceiverScoringFunctionFactory rScorFuncFac = new MyReceiverScoringFunctionFactoryImpl();
				
				/*
				 * Create a new instance of a receiver plan strategy manager factory.
				 */
				
				final ReceiverOrderStrategyManagerFactory rStratManFac = new MyReceiverOrderStrategyManagerFactorImpl();
				
				ReceiverModule receiverControler = new ReceiverModule(finalReceivers, rScorFuncFac, rStratManFac);
				
				controler.addOverridingModule(receiverControler);

				prepareFreightOutputDataAndStats(controler, finalCarriers, outputDir, finalReceivers);
								
				controler.run();					
			
			seed *= 2;
		}
		
		}
	
	private static class MyCarrierPlanStrategyManagerFactoryImpl implements CarrierPlanStrategyManagerFactory {

		/*
		 * Adapted from RunChessboard.java by sschroeder and gliedtke.
		 */
		
		private Network network;
		private MatsimServices controler;
		private CarrierVehicleTypes types;

		public MyCarrierPlanStrategyManagerFactoryImpl(final CarrierVehicleTypes types, final Network network, final MatsimServices controler) {
			this.types = types;
			this.network = network;
			this.controler= controler;
		}

		@Override
		public GenericStrategyManager<CarrierPlan, Carrier> createStrategyManager() {
			TravelDisutility travelDis = TravelDisutilities.createBaseDisutility(types, controler.getLinkTravelTimes());
			final LeastCostPathCalculator router = controler.getLeastCostPathCalculatorFactory().createPathCalculator(network,	travelDis, controler.getLinkTravelTimes());

			final GenericStrategyManager<CarrierPlan, Carrier> strategyManager = new GenericStrategyManager<>();
			strategyManager.setMaxPlansPerAgent(5);
			{
				GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>(new ExpBetaPlanChanger<CarrierPlan, Carrier>(1.));
				strategyManager.addStrategy(strategy, null, 1.0);

			}

			{
				GenericPlanStrategyImpl<CarrierPlan, Carrier> strategy = new GenericPlanStrategyImpl<>(new KeepSelected<CarrierPlan, Carrier>());
				strategy.addStrategyModule(new TimeAllocationMutator());
				ReRouteVehicles reRouteModule = new ReRouteVehicles(router, network, controler.getLinkTravelTimes(), 1.);
				strategy.addStrategyModule(reRouteModule);
				strategyManager.addStrategy(strategy, null, 0.5);
			}

			return strategyManager;
		}
	}
	
	

	private static void carrierPlanGenerator(String directory, Scenario scReceiver) {
		
		final Carriers carriers = new Carriers();
		new CarrierPlanXmlReaderV2(carriers).readFile(directory + "/input/carrierFile.xml");
		
		// read vehicle types.
		
		final CarrierVehicleTypes types = new CarrierVehicleTypes();

		new CarrierVehicleTypeReader(types).readFile(directory + "/input/vehicleTypes.xml");
		
		//assign them to their corresponding vehicles (defined in carrier.xml)

		new CarrierVehicleTypeLoader(carriers).loadVehicleTypes(types);	
				
		//get the carrier.
		Carrier carrier = carriers.getCarriers().get(Id.create("Carrier1", Carrier.class)); 

		/*
		 * Build the routing problem.
		 */
		
		VehicleRoutingProblem.Builder vrpBuilder = MatsimJspritFactory.createRoutingProblemBuilder(carrier, scReceiver.getNetwork());
		
		NetworkBasedTransportCosts netBasedCosts = NetworkBasedTransportCosts.Builder.newInstance(scReceiver.getNetwork(), types.getVehicleTypes().values()).build();

		VehicleRoutingProblem vrp = vrpBuilder.setRoutingCost(netBasedCosts).build();

		//read and create a pre-configured algorithms to solve the vrp
		VehicleRoutingAlgorithm vra = VehicleRoutingAlgorithms.readAndCreateAlgorithm(vrp, directory + "/input/initialPlanAlgorithm.xml");

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
		new CarrierPlanXmlWriterV2(carriers).write(directory + "/input/carrierPlanned.xml");
		
	}

	/**
	 * Creates a receiver with characteristics.
	 * @param start
	 * @param end
	 * @return
	 */

	private static ReceiverCharacteristics createReceiver(Id<Link> location, double start, double end) {
		ReceiverCharacteristics.Builder builder = ReceiverCharacteristics.Builder.newInstance();
		ReceiverCharacteristics receiverChar = builder.setLocation(location).setTimeWindowStart(start).setTimeWindowEnd(end).build();
		return receiverChar;
	}
	
	/**
	 * Creates a new general product type with a description and the product weight (in tonne per item).
	 * @param name
	 * @param requiredCapacity
	 * @return
	 */
	

	private static ProductType createProductType(Id<ProductType> Id, String name, double requiredCapacity) {
		String descr = name;
		Id<ProductType> typeId = Id;
		double reqCap = requiredCapacity;
		ProductType prodType = ProductTypeImpl.newInstance(typeId);
		prodType.setProductDescription(descr);
		prodType.setRequiredCapacity(reqCap);
		System.out.println("Created product type " + prodType.getDescription() + " with weight of " + prodType.getRequiredCapacity() + " kg.");
		return prodType;		
	}
	
	/**
	 * Creates a receiver product type from the generic product types, i.e. allocates products to receivers.
	 * @param prodtype
	 * @return
	 */
	
	private static ReceiverProductType createReceiverProductType(ProductType prodType) {
		ReceiverProductType.Builder builder = ReceiverProductType.Builder.newInstance(prodType.getId());
		ReceiverProductType rProdType = builder.setProductDescription(prodType.getDescription()).setRequiredCapacity(prodType.getRequiredCapacity()).build();
		System.out.println("Created a specific receiver product of type " + rProdType.getDescription() + " with weight of " + rProdType.getRequiredCapacity() + " tonnes.");
		return rProdType;
	}
	
	/**
	 * Assigns a receiver product type to a receiver, delivery location, and reordering policy parameters -- i.e. min and max inventory levels (in units).
	 * @param location
	 * @param prodType
	 * @param maxLevel
	 * @param minLevel
	 * @return
	 */
	
	private static ReceiverProduct createReceiverProduct(Id<Link> location, ReceiverProductType prodType, int maxLevel, int minLevel) {
		ReceiverProduct.Builder builder = ReceiverProduct.Builder.newInstance();
		ReceiverProduct rProd = builder.setMaxLevel(maxLevel).setMinLevel(minLevel).setProductType(prodType).build();
		System.out.println("Created a receiver product of type " + prodType.getDescription() + " with min inventory level of " + minLevel + " units, and max inventory level of " + maxLevel + " units.");
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
		Order.Builder builder = Order.Builder.newInstance(number, receiver, receiverProduct, serviceTime);
		Order order = builder.setServiceTime(serviceTime).build();
		System.out.println("Created an order of type " + order.getOrderName() + " for receiver " + order.getReceiver().getId() + " with order quantity of " + order.getOrderQuantity() + " tonnes, to " + order.getOrderDeliveryLocation() + ", and service duaration of " + Time.writeTime(order.getServiceDuration()) + ".");
		return order;
	}

	/**
	 * Create the output directory if it doesn't exist.
	 * @param outputDir
	 */

	private static void createOutputDir(String outputDir) {
		File dir = new File(outputDir);
		// if the directory does not exist, create it
		if (!dir.exists()){
			System.out.println("creating directory "+ outputDir);
			boolean result = dir.mkdirs();
			if(result) System.out.println(outputDir +" created");
		}
		
	}
	
	/**
	 * Selects a random link in the network.
	 * @param network
	 * @return
	 */
	
	private static Id<Link> selectRandomLink(Network network){
		Object[] linkIds = network.getLinks().keySet().toArray();
		int sample = MatsimRandom.getRandom().nextInt(linkIds.length);
		Object o = linkIds[sample];
		//System.out.println("random link "+ o);
		if(o instanceof Id<?>){
			return (Id<Link>) o;
		} else{
			throw new RuntimeException("Oops, cannot find a correct link Id.");
		}
		
	}
	
    private static void prepareFreightOutputDataAndStats(MatsimServices controler, final Carriers carriers, String outputDir, final Receivers receivers) {
    	/*
    	 * Adapted from RunChessboard.java by sshroeder and gliedtke.
    	 */
      final int statInterval = 1;
      final LegHistogram freightOnly = new LegHistogram(20);
      
     // freightOnly.setPopulation(controler.getScenario().getPopulation());
      //freightOnly.setInclPop(false);
     
      CarrierScoreStats scoreStats = new CarrierScoreStats(carriers, outputDir + "/carrier_scores", true);
      ReceiverScoreStats rScoreStats = new ReceiverScoreStats(receivers, outputDir + "/receiver_scores", true);
      
		controler.getEvents().addHandler(freightOnly);
		controler.addControlerListener(scoreStats);
		controler.addControlerListener(rScoreStats);
		controler.addControlerListener(new IterationEndsListener() {

			@Override
			public void notifyIterationEnds(IterationEndsEvent event) {
				if(event.getIteration() % statInterval != 0) return;
				//write plans
				String dir = event.getServices().getControlerIO().getIterationPath(event.getIteration());
				new CarrierPlanXmlWriterV2(carriers).write(dir + "/" + event.getIteration() + ".carrierPlans.xml");

				//write stats
				freightOnly.writeGraphic(dir + "/" + event.getIteration() + ".legHistogram_freight.png");
				freightOnly.reset(event.getIteration());

		}
		});		
    }
    
    
}
