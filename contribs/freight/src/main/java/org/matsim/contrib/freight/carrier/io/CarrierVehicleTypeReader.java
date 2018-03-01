/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.freight.carrier.io;

import java.util.Stack;

import org.apache.log4j.Logger;
import org.matsim.contrib.freight.carrier.CarrierVehicleTypes;
import org.matsim.core.utils.io.MatsimXmlParser;
import org.xml.sax.Attributes;

/**
 * GOAL:
 * A CarrierVehicleTypes reader that reads the MATSim format. This reader recognizes the format of the CarrierVehicleTypes-file and uses
 * the correct reader for the specific CarrierVehicleTypes-version, without manual setting.
 * 
 * Current status Feb/18: Calls the "old" reader.
 * 
 * TODO: define .dtd-files, create a v2-reader, adapt CarrierVehicleTypes and other stuff...
 * 
 * @author kturner
 *
 */
public final class CarrierVehicleTypeReader extends MatsimXmlParser{

	private final static String CARRIER_VEHICLE_TYPE_V1 = "carrierVehicleType_v1.dtd";	
	//	private final static String CARRIER_VEHICLE_TYPE_V2 = "carrierVehicleType_v2.dtd";		//TODO: Create v1.dtd

	private MatsimXmlParser delegate = null;
	
	private CarrierVehicleTypes carrierVehicleTypes;
	private String carrierVehicleTypesFile;

	private static final Logger log = Logger.getLogger(CarrierVehicleTypeReader.class);
	

	public CarrierVehicleTypeReader(CarrierVehicleTypes carrierVehicleTypes, String carrierVehicleTypesFile) {
		super();
		this.carrierVehicleTypes = carrierVehicleTypes;
		this.carrierVehicleTypesFile = carrierVehicleTypesFile;
	}

	@Override
	public void startTag(String name, Attributes atts, Stack<String> context) {
		this.delegate.startTag(name, atts, context);

	}

	@Override
	public void endTag(String name, String content, Stack<String> context) {
		this.delegate.endTag(name, content, context);
	}

	@Override
	protected void setDoctype(final String doctype) {
		super.setDoctype(doctype);
		switch ( doctype ) {	
		//				
		//				//TODO: V2 does not exist yet. kmt feb/18
		//				case CARRIER_VEHICLE_TYPE_V2 :
		//					this.delegate =
		//							new CarrierVehicleTypeReaderV2(this.carrierVehicleTypes);
		//					log.info("using carrierVehicleTypeV2-reader.");
		//					break;
		case CARRIER_VEHICLE_TYPE_V1 :
			this.delegate =
				new CarrierVehicleTypeReaderV1(this.carrierVehicleTypes); 				//TODO: Übergabewert setzen, prüfen.
			log.info("using carrierVehicleTypeV1-reader.");
			break;
		default:
			this.delegate = new CarrierVehicleTypeReaderV1(carrierVehicleTypes);
			log.warn("No carrierVehicleType reader available for doctype \"" + doctype + "\" ... using carrierVehicleTypeV1-reader.");
		}

	}

}
