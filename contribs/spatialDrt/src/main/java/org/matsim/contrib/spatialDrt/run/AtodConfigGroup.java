/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package org.matsim.contrib.spatialDrt.run;

import org.matsim.contrib.drt.optimizer.insertion.ParallelPathDataProvider;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.spatialDrt.parkingStrategy.ParkingStrategy;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import java.net.URL;
import java.util.Map;

public class AtodConfigGroup extends ReflectiveConfigGroup {
	
	public static final String GROUP_NAME = "atod";

	public AtodConfigGroup() {
		super(GROUP_NAME);
	}


	@SuppressWarnings("deprecation")
	public static AtodConfigGroup get(Config config) {
		return (AtodConfigGroup)config.getModule(GROUP_NAME);
	}

	public static final String PARKING_STRATEGY = "parkingStrategy";
	static final String PARKING = "Paring strategies, alwaysRoaming, parkingOntheRoad, parkingInDepot, MixedParking";

	public static final String DEPOT_FILE = "depotFile";
	static final String DEPOT_FILE_EXP = "An XML file specifying the location of depots. The file format according to depot.dtd";


	public static final String DOOR_2_DOOR_STOP = "door2DoorStop";
	static final String DOOR_2_DOOR_STOP_EXP = "The bay length of the door-to-door AV, infinity means no bay length restriction, linkLength means length equals to the link length";

	public static final String MIN_BAY_SIZE = "minBaySize";
	static final String MIN_BAY_SIZE_EXP = "The minimum bay size for transit stop, 0.0 by default.";

	public static final String CHARGE_FILE ="chargingFile";
	static final String CHARGE_FILE_EXP = "The location of the charging points. The file format according to charger.dtd";

	public static final String MIN_BATTERY = "minBattery";
	static final String MIN_BATTERY_EXP = "When reaches the min battery value, the vehicle will go to charge";

	public static final String MIN_REQUEST_ACCEPT = "minRequestAccept";
	static final String MIN_REQUEST_ACCEPT_EXP ="If the request is too far, reject it";

	public static final String STOP_IN_AREA = "stopInArea";
	static final String STOP_IN_AREA_EXP = "Stops with in service area of drt";

	@NotNull
	private ParkingStrategy.Strategies parkingStrategy = ParkingStrategy.Strategies.NoParkingStrategy;

	@NotNull
	private String depotFile = null;


	@NotNull
	private Door2DoorStop door2DoorStop= Door2DoorStop.infinity;

	private double minBaySize = 0.0;


	public enum Door2DoorStop{
		infinity, linkLength
	}

	private String chargeFile = null;
	private double minBattery = 0.0;
	private double minRequestAccept = 0.0;
	private String stopInArea =null;


	@Override
	public Map<String, String> getComments() {
		Map<String, String> map = super.getComments();
		map.put(PARKING_STRATEGY, PARKING);
		map.put(DEPOT_FILE, DEPOT_FILE_EXP);
		map.put(DOOR_2_DOOR_STOP, DOOR_2_DOOR_STOP_EXP);
		map.put(MIN_BAY_SIZE, MIN_BAY_SIZE_EXP);
		map.put(CHARGE_FILE, CHARGE_FILE_EXP);
		map.put(MIN_BATTERY, MIN_BATTERY_EXP);
		map.put(MIN_REQUEST_ACCEPT, MIN_REQUEST_ACCEPT_EXP);
		map.put(STOP_IN_AREA, STOP_IN_AREA_EXP);
		return map;
	}

		/**
	 *
	 * @return -- {@value #DEPOT_FILE_EXP}
	 */
	@StringGetter(DEPOT_FILE)
	public String getDepotFile() {
		return depotFile;
	}
	/**
	 * 
	 * @param depotFile -- {@value #DEPOT_FILE_EXP}
	 */
	@StringSetter(DEPOT_FILE)
	public void setDepotFile(String depotFile) {
		this.depotFile = depotFile;
	}
	/**
	 * 
	 * @return -- {@value #DEPOT_FILE_EXP}
	 */
	public URL getDepotFileUrl(URL context) {
		return ConfigGroup.getInputFileURL(context, this.depotFile);
	}


	/**
	 * 
	 * @return -- {@value #PARKING}
	 */
	@StringGetter(PARKING_STRATEGY)
	public ParkingStrategy.Strategies getParkingStrategy() {
		return parkingStrategy;
	}

	/**
	 * 
	 * @param parkingStrategy -- {@value #PARKING}
	 */
	@StringSetter(PARKING_STRATEGY)
	public void setParkingStrategy(String parkingStrategy) {

		this.parkingStrategy = ParkingStrategy.Strategies.valueOf(parkingStrategy);
	}


	@StringGetter(DOOR_2_DOOR_STOP)
	public Door2DoorStop getDoor2DoorStop() {
		return door2DoorStop;
	}

	@StringSetter(DOOR_2_DOOR_STOP)
	public void setDoor2DoorStop(String door2doorStop) {
		this.door2DoorStop = Door2DoorStop.valueOf(door2doorStop);
	}

	@StringGetter(MIN_BAY_SIZE)
	public double getMinBaySize(){
		return minBaySize;
	}
	@StringSetter(MIN_BAY_SIZE)
	public void setMinBaySize(double minBaySize){
		this.minBaySize = minBaySize;
	}


	public URL getChargeFileURL(URL context){
		return ConfigGroup.getInputFileURL(context, chargeFile);
	}
	@StringGetter(CHARGE_FILE)
	public String getChargeFile(){
		return this.chargeFile;
	}
	@StringSetter(CHARGE_FILE)
	public void setChargeFile(String chargeFile){
		this.chargeFile = chargeFile;
	}

	@StringGetter(MIN_BATTERY)
	public double getMinBattery() {
		return minBattery;
	}
	@StringSetter(MIN_BATTERY)
	public void setMinBattery(double minBattery) {
		this.minBattery = minBattery;
	}

	@StringGetter(MIN_REQUEST_ACCEPT)
	public double getMinRequestAccept() {
		return minRequestAccept;
	}
	@StringSetter(MIN_REQUEST_ACCEPT)

	public void setMinRequestAccept(double minRequestAccept) {
		this.minRequestAccept = minRequestAccept;
	}

	public URL getStopInAreaURL(URL context){
		return ConfigGroup.getInputFileURL(context, stopInArea);
	}

	@StringGetter(STOP_IN_AREA)
	public String getStopInArea(){
		return this.stopInArea;
	}

	@StringSetter(STOP_IN_AREA)
	public void setStopInArea(String stopInArea){
		this.stopInArea = stopInArea;
	}

}
