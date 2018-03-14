package org.matsim.contrib.freight.carrier;

import org.matsim.testcases.MatsimTestCase;
import org.matsim.contrib.freight.carrier.io.CarrierVehicleTypeReader;

public class CarrierVehicleTypeWriterTest extends MatsimTestCase{
	
	
	public void testTypeWriter(){
		CarrierVehicleTypes types = new CarrierVehicleTypes();
		new CarrierVehicleTypeReader(types).readFile(getClassInputDirectory() + "vehicleTypes.xml");
		new CarrierVehicleTypeWriter(types).write(getClassInputDirectory() + "vehicleTypesWritten.xml");
		types.getVehicleTypes().clear();
		new CarrierVehicleTypeReader(types).readFile(getClassInputDirectory() + "vehicleTypesWritten.xml");
	}
}
