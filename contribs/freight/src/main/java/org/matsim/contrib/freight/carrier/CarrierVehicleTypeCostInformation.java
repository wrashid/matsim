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
  
package org.matsim.contrib.freight.carrier;
  
public class CarrierVehicleTypeCostInformation {

	public final double fix;

	public final double perDistanceUnit;
	@Deprecated
	public final double perTimeUnit;
	public final double perTransportTimeUnit;
    public final double perWaitingTimeUnit;
    public final double perServiceTimeUnit;
    

	/**
	 *  Costs perWaitingTimeUnit and perServiceTimeUnit were set to 0.0
	 * 
	 * @deprecated Use CarrierVehicleTypeCostInformation(double fix, double perDistanceUnit, double perTransportTimeUnit, double perWaitingTimeUnit, double perServiceTimeUnit) instead.
	 * In this "old" version there are only costsPerTimeUnit and no separation for the different values of time dependent costs. // Adaption to jsprit 1.7.x. KMT mar/18
	 * 
	 * @param fix
	 * @param perDistanceUnit
	 * @param perTimeUnit
	 */
    @Deprecated
	public CarrierVehicleTypeCostInformation(double fix, double perDistanceUnit, double perTimeUnit) {
		super();
		this.fix = fix;
		this.perDistanceUnit = perDistanceUnit;
		this.perTimeUnit = perTimeUnit;
		this.perTransportTimeUnit = perTimeUnit;
		this.perWaitingTimeUnit = 0.0;
		this.perServiceTimeUnit = 0.0;
	}
	
	
	/**
	 * The new (mar/18) Constructor including the new setting for the time dependent costs
	 * @param fix
	 * @param perDistanceUnit
	 * @param perTimeUnit
	 * @param perTransportTimeUnit
	 * @param perWaitingTimeUnit
	 * @param perServiceTimeUnit
	 */
	public CarrierVehicleTypeCostInformation(double fix, double perDistanceUnit, double perTransportTimeUnit, double perWaitingTimeUnit, double perServiceTimeUnit) {
		super();
		this.fix = fix;
		this.perDistanceUnit = perDistanceUnit;
		this.perTimeUnit = Double.NEGATIVE_INFINITY;  //TODO: in this version it should not been used any more --> Negative_Infinity or 0.0 ?? kmt mar/18
		this.perTransportTimeUnit = perTransportTimeUnit;
		this.perWaitingTimeUnit = perWaitingTimeUnit;
		this.perServiceTimeUnit = perServiceTimeUnit;
	}

}