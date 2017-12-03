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

/**
 * 
 */
package org.matsim.contrib.av.intermodal.router.config;

import java.net.URL;
import java.util.Collection;

import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * @author  jbischoff
 * @author	vsp-gleich
 */
/**
 *
 */
public class VariableAccessConfigGroup extends ReflectiveConfigGroup {

	public static final String GROUPNAME = "variableAccess";
	private static final String STYLE = "VariableAccessStyle";
	private static final String SCHEDULE = "VariableAccessTransitScheduleFile";
	private static final String MODE = "mode";
	private static final String AREA_FILE = "VariableAccessAreaShpFile";
	private static final String AREA_KEY = "VariableAccessAreaShpKey";
	private static final String COORDS2TIME_SURCHARGE_FILE = "coords2TimeSurchargeFile";
	private static final String MAX_DISTANCE_ONLY_TRANSIT_WALK_AVAILABLE = "maxDistanceOnlyTransitWalkAvailable";
	private String style = "fixed";
	private String mode = "pt";
	private double maxDistanceOnlyTransitWalkAvailable = 0.;
	private String transitScheduleFile = null;
	private String variableAccessAreaShpFile = null;
	private String variableAccessAreaShpKey = null;
	private String coords2TimeSurchargeFile = null;
	
	public static final String MODEGROUPNAME = "variableAccessMode";

	/**
	 * @param name
	 */
	public VariableAccessConfigGroup() {
		super(GROUPNAME);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @return the mode
	 */
	@StringGetter(STYLE)
	public String getStyle() {
		return style;
	}
	@StringGetter(MODE)
	public String getMode() {
		return mode;
	}

	/**
	 * @return the transitScheduleFile
	 */
	@StringGetter(SCHEDULE)
	public String getTransitScheduleFile() {
		return transitScheduleFile;
	}
	
	public URL getTransitScheduleFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, getTransitScheduleFile() ) ;
	}	
	/**
	 * @param transitScheduleFile the transitScheduleFile to set
	 */
	@StringSetter(SCHEDULE)
	public void setTransitScheduleFile(String transitScheduleFile) {
		this.transitScheduleFile = transitScheduleFile;
	}
	
	/**
	 * @return the variableAccessAreaShpFile containing the geometry elements
	 * which describe the area(s) wherein variable access is
	 * used for all routes originating or ending there. Outside this area variable
	 * access is not used. Without any file given the default is to use variable
	 * access for all routes.
	 */
	@StringGetter(AREA_FILE)
	public String getVariableAccessAreaShpFile() {
		return variableAccessAreaShpFile;
	}
	
	public URL getVariableAccessAreaShpFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, getVariableAccessAreaShpFile() ) ;
	}	
	/**
	 * @param variableAccessAreaShpFile the variableAccessAreaShpFile to set 
	 * containing the geometry elements
	 * which describe the area(s) wherein variable access is
	 * used for all routes originating or ending there. Outside this area variable
	 * access is not used. Without any file given the default is to use variable
	 * access for all routes.
	 * Path relative to config file
	 */
	@StringSetter(AREA_FILE)
	public void setVariableAccessAreaShpFile(String variableAccessAreaShpFile) {
		this.variableAccessAreaShpFile = variableAccessAreaShpFile;
	}
	
	
	/**
	 * @return the variableAccessAreaShpKey
	 */
	@StringGetter(AREA_KEY)
	public String getVariableAccessAreaShpKey() {
		return variableAccessAreaShpKey;
	}

	/**
	 * @param variableAccessAreaShpKey the variableAccessAreaShpKey to set
	 */
	@StringSetter(AREA_KEY)
	public void setVariableAccessAreaShpKey(String variableAccessAreaShpKey) {
		this.variableAccessAreaShpKey = variableAccessAreaShpKey;
	}
	
	/**
	 * @return the coords2TimeSurchargeFile containing the coords and time surcharges
	 * applied to them. Can be used for example to make some TransitStops less attractive
	 * than others. Is only applied to routes starting or ending in the variable access area
	 * Without any file given the default is to use variable access for all routes.
	 * Path relative to config file
	 */
	@StringGetter(COORDS2TIME_SURCHARGE_FILE)
	public String getCoords2TimeSurchargeFile() {
		return coords2TimeSurchargeFile;
	}
	
	public URL getCoords2TimeSurchargeFileURL(URL context) {
		return ConfigGroup.getInputFileURL(context, getCoords2TimeSurchargeFile() ) ;
	}	
	/**
	 * @param coords2TimeSurchargeFile path to the the coords2TimeSurchargeFile to set 
	 * containing the coords and time surcharges
	 * applied to them. Can be used for example to make some TransitStops less attractive
	 * than others. Is only applied to routes starting or ending in the variable access area
	 * Without any file given the default is to use variable access for all routes.
	 */
	@StringSetter(COORDS2TIME_SURCHARGE_FILE)
	public void setCoords2TimeSurchargeFile(String coords2TimeSurchargeFile) {
		this.coords2TimeSurchargeFile = coords2TimeSurchargeFile;
	}
	
	/**
	 * @param maxDistanceOnlyTransitWalkAvailable
	 * 			the maxDistanceOnlyTransitWalkAvailable to set
	 */
	@StringSetter(MAX_DISTANCE_ONLY_TRANSIT_WALK_AVAILABLE)
	public void setMaxDistanceOnlyTransitWalkAvailable(double maxDistanceOnlyTransitWalkAvailable) {
		this.maxDistanceOnlyTransitWalkAvailable = maxDistanceOnlyTransitWalkAvailable;
	}
	
	/**
	 * @return the maxDistanceOnlyTransitWalkAvailable
	 */
	@StringGetter(MAX_DISTANCE_ONLY_TRANSIT_WALK_AVAILABLE)
	public double getMaxDistanceOnlyTransitWalkAvailable() {
		return maxDistanceOnlyTransitWalkAvailable;
	}
	
	/**
	 * @param mode
	 *            the mode to set
	 */
	@StringSetter(STYLE)
	public void setStyle(String style) {
		this.style = style;
	}
	
	@StringSetter(MODE)
	public void setMode(String mode) {
		this.mode = mode;
	}

	public Collection<ConfigGroup> getVariableAccessModeConfigGroups() {
		return (Collection<ConfigGroup>) getParameterSets(MODEGROUPNAME);
	}

	public void setAccessModeGroup(ConfigGroup modeConfig) {
		addParameterSet(modeConfig);
	}

	@Override
	public ConfigGroup createParameterSet(final String type) {
		switch (type) {

		case MODEGROUPNAME:
			return new VariableAccessModeConfigGroup();
		default:
			throw new IllegalArgumentException(type);
		}
	}

}
