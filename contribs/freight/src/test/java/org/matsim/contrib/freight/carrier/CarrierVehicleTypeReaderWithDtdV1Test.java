package org.matsim.contrib.freight.carrier;

import org.junit.Ignore;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.io.CarrierVehicleTypeReader;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.VehicleType;

/**
 * Tests for the new version-specific CarrierVehicleTypes-reader
 * Here: Using carrierVehicleType_v1.dtd
 * TODO: Move dtd to matsim.org/files/dtd and refactor path in vehicleTypes.xml
 * 
 * 
 * @author kturner
 *
 */
public class CarrierVehicleTypeReaderWithDtdV1Test extends MatsimTestCase{
	
	CarrierVehicleTypes types;
	
	@Override
	public void setUp() throws Exception{
		super.setUp();
		types = new CarrierVehicleTypes();
		CarrierVehicleTypeReader vehicleTypeReader = new CarrierVehicleTypeReader(types); //TODO: shorten it and use import after going investigations are done succesfully ;) kmt feb/18
		vehicleTypeReader.readFile(getClassInputDirectory() + "vehicleTypes.xml");
	}
	
	public void test_whenReadingTypes_nuOfTypesIsReadCorrectly(){
		assertEquals(2, types.getVehicleTypes().size());
	}
	
	public void test_whenReadingTypes_itReadyExactlyTheTypesFromFile(){
		assertTrue(types.getVehicleTypes().containsKey(Id.create("medium", VehicleType.class)));
		assertTrue(types.getVehicleTypes().containsKey(Id.create("light", VehicleType.class)));
	}
	
//	### Some tests if capabilities for type Medium are read correctly
	public void test_whenReadingTypeMedium_itReadsDescriptionCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals("Medium Vehicle", medium.getDescription());
	}

//	//--> allowableWeight is not implemented kmt mrz/18
//	@Ignore 
//	public void test_whenReadingTypeMedium_itAllowableWeightCorrectly(){
//		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
//		assertEquals(12, medium.);
//	}
	
	public void test_whenReadingTypeMedium_itReadsCapacityCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals(30, medium.getCarrierVehicleCapacity());
	}
	
	public void test_whenReadingTypeMedium_itReadsCostInfoCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals(50.0, medium.getVehicleCostInformation().fix,0.01);
		assertEquals(0.4, medium.getVehicleCostInformation().perDistanceUnit,0.01);
		assertEquals(30.0, medium.getVehicleCostInformation().perTimeUnit,0.01);
	}
	
	public void test_whenReadingTypeMedium_itReadsEngineInfoCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals(0.02, medium.getEngineInformation().getGasConsumption(),0.001);
		assertEquals(FuelType.gasoline, medium.getEngineInformation().getFuelType());
	}
	
	public void test_whenReadingTypeMedium_itReadsMaxVelocityCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals(9.5, medium.getMaximumVelocity());
	}
	
	
	
//	### And now the same for type Light ####
	public void test_whenReadingTypeLight_itReadsDescriptionCorrectly(){
		CarrierVehicleType light = types.getVehicleTypes().get(Id.create("light", VehicleType.class));
		assertEquals("Light Vehicle", light.getDescription());
	}
	
//	//--> allowableWeight is not implemented kmt mrz/18
//	@Ignore 
//	public void test_whenReadingTypeMedium_itAllowableWeightCorrectly(){
//		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("light", VehicleType.class));
//		assertEquals(8, light.);
//	}
	
	public void test_whenReadingTypeLight_itReadsCapacityCorrectly(){
		CarrierVehicleType Light = types.getVehicleTypes().get(Id.create("light", VehicleType.class));
		assertEquals(15, Light.getCarrierVehicleCapacity());
	}
	
	public void test_whenReadingTypeLight_itReadsCostInfoCorrectly(){
		CarrierVehicleType Light = types.getVehicleTypes().get(Id.create("light", VehicleType.class));
		assertEquals(20.0, Light.getVehicleCostInformation().fix,0.01);
		assertEquals(0.35, Light.getVehicleCostInformation().perDistanceUnit,0.01);
		assertEquals(25.0, Light.getVehicleCostInformation().perTimeUnit,0.01);
	}
	
	public void test_whenReadingTypeLight_itReadsEngineInfoCorrectly(){
		CarrierVehicleType Light = types.getVehicleTypes().get(Id.create("light", VehicleType.class));
		assertEquals(0.01, Light.getEngineInformation().getGasConsumption(),0.001);
		assertEquals(FuelType.diesel, Light.getEngineInformation().getFuelType());
	}
	
	public void test_whenReadingTypeLight_itReadsMaxVelocityCorrectly(){
		CarrierVehicleType light = types.getVehicleTypes().get(Id.create("light", VehicleType.class));
		assertEquals(10.0, light.getMaximumVelocity());
	}
	
}
