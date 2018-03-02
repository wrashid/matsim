package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.io.CarrierVehicleTypeReader;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.vehicles.VehicleType;

/**
 * Tests for the new version-specific CarrierVehicleTypes-reader
 * Here: Using carrierVehicleType_v1.dtd
 * TODO: Move dtd to matsim.org/files/dtd and refactor path in vehicleTypes.xml
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
		assertEquals(2, types.getVehicleTypes().size());
	}
	
	public void test_whenReadingTypeMedium_itReadsDescriptionCorrectly(){
		CarrierVehicleType medium = types.getVehicleTypes().get(Id.create("medium", VehicleType.class));
		assertEquals("Medium Vehicle", medium.getDescription());
	}

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
		assertEquals(0.02, medium.getEngineInformation().getGasConsumption(),0.01);
		assertEquals("gasoline", medium.getEngineInformation().getFuelType().toString());
	}
}
