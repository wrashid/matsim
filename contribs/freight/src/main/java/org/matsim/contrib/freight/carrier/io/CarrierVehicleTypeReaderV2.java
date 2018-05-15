package org.matsim.contrib.freight.carrier.io;

import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.freight.carrier.CarrierVehicleType;
import org.matsim.contrib.freight.carrier.CarrierVehicleType.VehicleTypeCostInformation;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.EngineInformation.FuelType;
import org.matsim.vehicles.EngineInformationImpl;
import org.matsim.vehicles.VehicleType;
import org.xml.sax.Attributes;

/**
 * Reader reading carrierVehicleTypes from an xml-file according to <code>network_v2.dtd</code>.
 * 
 * TODO: Neue Kosten einlesen. 
 * TODO: v2.dtd erstellen und online stellen.
 * TODO: Tests schreiben.	 
 * 
 * @author kturner, based on sschroeder
 *
 */
public class CarrierVehicleTypeReaderV2 extends MatsimXmlParser {
	
	private static Logger logger = Logger.getLogger(CarrierVehicleTypeReaderV2.class);
	
	private CarrierVehicleTypes carrierVehicleTypes;

	private Id<VehicleType> currentTypeId;

	private String currentDescription;

	

//	private Integer currentCap;

	private VehicleTypeCostInformation currentVehicleCosts;

	private EngineInformation currentEngineInfo;

	private String currentCapacity;
	
	private String maxVelo;

	public CarrierVehicleTypeReaderV2(CarrierVehicleTypes carrierVehicleTypes) {
		super();
		this.carrierVehicleTypes = carrierVehicleTypes;
	}
	

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		if(name.equals("vehicleType")){
			this.currentTypeId = Id.create(atts.getValue("id"), VehicleType.class);
		}
		if(name.equals("allowableWeight")){
			String weight = atts.getValue("weight");
			parseDouble(weight);
		}
		if(name.equals("engineInformation")){
			String fuelType = atts.getValue("fuelType");
			String gasConsumption = atts.getValue("gasConsumption");
			EngineInformation engineInfo = new EngineInformationImpl(parseFuelType(fuelType), parseDouble(gasConsumption));
			this.currentEngineInfo = engineInfo;
		}
		
		if(name.equals("costInformation")){
			String fix = atts.getValue("fix");
			String perMeter = atts.getValue("perMeter");
			
			@Deprecated // not longer used in this version. TODO: can be removed after modification finished.
			String perSecond = atts.getValue("perSecond");
			
			String perTransportTimeUnit = atts.getValue("perTransportTimeUnit");
			String perWaitingTimeUnit = atts.getValue("perWaitingTimeUnit");
			String perServiceTimeUnit = atts.getValue("perServiceTimeUnit ");
			
			if(fix == null || perMeter == null || perTransportTimeUnit  == null) throw new IllegalStateException("cannot read costInformation correctly. probably the paramName was written wrongly");
			//TODO: Muss hier noch ein Fall rein, welche Werte da sind und dann ggf verschiedene VehicleCostInformationsConstrucoren aufrufen? Ich vermute ja mal ja... kmt april/18 
			VehicleTypeCostInformation vehicleCosts = new VehicleTypeCostInformation(parseDouble(fix), parseDouble(perMeter), parseDouble(perTransportTimeUnit), parseDouble(perWaitingTimeUnit), parseDouble(perServiceTimeUnit));
			this.currentVehicleCosts = vehicleCosts;
		}
	}

	private FuelType parseFuelType(String fuelType) {
		if(fuelType.equals(FuelType.diesel.toString())){
			return FuelType.diesel;
		}
		else if(fuelType.equals(FuelType.electricity.toString())){
			return FuelType.electricity;
		}
		else if(fuelType.equals(FuelType.gasoline.toString())){
			return FuelType.gasoline;
		}
		throw new IllegalStateException("fuelType " + fuelType + " is not supported");
	}

	private double parseDouble(String weight) {
		return Double.parseDouble(weight);
	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		if(name.equals("description")){
			this.currentDescription = content;
		}
		if(name.equals("capacity")){
			this.currentCapacity = content;
		}
		if(name.equals("maxVelocity")){
			this.maxVelo = content;
		}
		if(name.equals("vehicleType")){
			CarrierVehicleType.Builder typeBuilder = CarrierVehicleType.Builder.newInstance(currentTypeId);
			if(currentDescription != null) typeBuilder.setDescription(currentDescription);
//			if(currentWeight != null) vehType.setAllowableTotalWeight(currentWeight);
//			if(currentCap != null) vehType.setFreightCapacity(currentCap);
			if(currentVehicleCosts != null) typeBuilder.setVehicleCostInformation(currentVehicleCosts);
			if(currentEngineInfo != null) typeBuilder.setEngineInformation(currentEngineInfo);
			if(currentCapacity != null) typeBuilder.setCapacity(Integer.parseInt(currentCapacity));
			if(maxVelo != null) typeBuilder.setMaxVelocity(Double.parseDouble(maxVelo));
			CarrierVehicleType vehType = typeBuilder.build();
			carrierVehicleTypes.getVehicleTypes().put(vehType.getId(), vehType);
			reset();
		}
		
	}

	private void reset() {
		currentTypeId = null;
		currentDescription = null;
		currentVehicleCosts = null;
		currentEngineInfo = null;
	}

}
