package org.matsim.contrib.freight.carrier;

import org.matsim.api.core.v01.Id;
import org.matsim.vehicles.EngineInformation;
import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.VehicleTypeImpl;

/**
 * The carrier vehicle type.
 * 
 * I decided to put vehicle cost information into the type (which is indeed not a physical attribute of the type). Thus physical and
 * non physical attributes are used. This is likely to be changed in future.
 * 
 * @author sschroeder
 *
 */
public class CarrierVehicleType extends ForwardingVehicleType {

	/**
	 * A builder building the type.
	 * 
	 * @author sschroeder
	 *
	 */
	public static class Builder {
		
		/**
		 * Returns a new instance of builder initialized with the typeId.
		 * 
		 * The defaults are [fix=0.0][perDistanceUnit=1.0][perTransportTimeUnit=0.0][perWaitingTimeUnit=0.0][perServiceTimeUnit=0.0].
		 * 
		 * @param typeId
		 * @return a type builder
		 */
		public static Builder newInstance(Id<VehicleType> typeId){
			return new Builder(typeId);
		}
		
		private Id<VehicleType> typeId;
		private double fix = 0.0;
		private double perDistanceUnit = 1.0;
		@Deprecated
		private double perTimeUnit = 0.0;
		private double perTransportTimeUnit = 0.0;
        private double perWaitingTimeUnit = 0.0;
        private double perServiceTimeUnit = 0.0;
		private String description;
		private EngineInformation engineInfo;
		private int capacity = 0;
		private double maxVeloInMeterPerSeconds = Double.MAX_VALUE;
		
		
		private Builder(Id<VehicleType> typeId){
			this.typeId = typeId;
		}
		
		/**
		 * Sets fixed costs of vehicle.
		 * 
		 * <p>By default it is 0.
		 * @param fix
		 * @return
		 */
		public Builder setFixCost(double fix){
			this.fix = fix;
			return this;
		}
		
		/**
		 * Sets costs per distance-unit.
		 * 
		 * <p>By default it is 1.
		 * 
		 * @param perDistanceUnit
		 * @return
		 */
		public Builder setCostPerDistanceUnit(double perDistanceUnit){
			this.perDistanceUnit = perDistanceUnit;
			return this;
		}
		
		/**
		 * Sets costs per time-unit.
		 * 
		 * <p>By default it is 0.
		 * @deprecated Use setCostsPerTransportTimeUnit and if wanted setCostsperWaitingTimeUnit and setCostsperServiceTimeUnit instead
		 * 
		 * @param perTimeUnit
		 * @return
		 */
		@Deprecated
		public Builder setCostPerTimeUnit(double perTimeUnit){
			this.perTimeUnit = perTimeUnit;
			return this;
		}
		
		/**
		 * Sets costs per time-unit during transport.
		 * 
		 * <p>By default it is 0.
		 * 
		 * @param perTransportTimeUnit
		 * @return
		 */
		public Builder setCostPerTransportTimeUnit(double perTransportTimeUnit){
			this.perTransportTimeUnit = perTransportTimeUnit;
			return this;
		}
		
		/**
		 * Sets costs per time-unit for the time the vehicle waits until performing a service activity.
		 * 
		 * <p>By default it is 0.
		 * 
		 * @param perWaitingTimeUnit)
		 * @return
		 */
		public Builder setCostPerWaitingTimeUnit(double perWaitingTimeUnit){
			this.perWaitingTimeUnit = perWaitingTimeUnit;
			return this;
		}
		
		/**
		 * Sets costs per time-unit for the time performing a service.
		 * 
		 * <p>By default it is 0.
		 * 
		 * @param perServiceTimeUnit
		 * @return
		 */
		public Builder setCostPerServiceTimeUnit(double perServiceTimeUnit){
			this.perServiceTimeUnit = perServiceTimeUnit;
			return this;
		}
		
		/**
		 * Sets description.
		 * 
		 * @param description
		 * @return this builder
		 */
		public Builder setDescription(String description){
			this.description = description;
			return this;
		}
		
		/**
		 * Sets the capacity of vehicle-type.
		 * 
		 * <p>By defaul the capacity is 0.
		 * 
		 * @param capacity
		 * @return this builder
		 */
		public Builder setCapacity(int capacity){
			this.capacity = capacity;
			return this;
		}
		
		/**
		 * Builds the type.
		 * 
		 * @return {@link CarrierVehicleType}
		 */
		public CarrierVehicleType build(){
			return new CarrierVehicleType(this);
		}

		/**
		 * Sets {@link VehicleCostInformation}
		 * 
		 * <p>The defaults are [fix=0.0][perDistanceUnit=1.0][perTransportTimeUnit=0.0][perWaitingTimeUnit=0.0][perServiceTimeUnit=0.0].
		 * 
		 * @param info
		 * @return this builder
		 */
		public Builder setVehicleCostInformation(VehicleCostInformation info) {
			fix = info.fix;
			perDistanceUnit = info.perDistanceUnit;
			perTimeUnit = info.perTimeUnit;
			perTransportTimeUnit = info.perTransportTimeUnit;
			perWaitingTimeUnit = info.perWaitingTimeUnit;
			perServiceTimeUnit = info.perServiceTimeUnit;
			return this;
		}

		/**
		 * Sets {@link EngineInformation}
		 * 
		 * @param engineInfo
		 * @return this builder
		 */
		public Builder setEngineInformation(EngineInformation engineInfo) {
			this.engineInfo = engineInfo;
			return this;
		}

		public Builder setMaxVelocity(double veloInMeterPerSeconds) {
			this.maxVeloInMeterPerSeconds  = veloInMeterPerSeconds;
			return this;
		}
	}
	

	public static class VehicleCostInformation {

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
		 * @deprecated Use VehicleCostInformation(double fix, double perDistanceUnit, double perTransportTimeUnit, double perWaitingTimeUnit, double perServiceTimeUnit) instead.
		 * In this "old" version there are only costsPerTimeUnit and no separation for the different values of time dependent costs. // Adaption to jsprit 1.7.x. KMT mar/18
		 * 
		 * @param fix
		 * @param perDistanceUnit
		 * @param perTimeUnit
		 */
        @Deprecated
		public VehicleCostInformation(double fix, double perDistanceUnit, double perTimeUnit) {
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
		public VehicleCostInformation(double fix, double perDistanceUnit, double perTransportTimeUnit, double perWaitingTimeUnit, double perServiceTimeUnit) {
			super();
			this.fix = fix;
			this.perDistanceUnit = perDistanceUnit;
			this.perTimeUnit = Double.NEGATIVE_INFINITY;  //TODO: in this version it should not been used any more --> Negative_Infinity or 0.0 ?? kmt mar/18
			this.perTransportTimeUnit = perTransportTimeUnit;
			this.perWaitingTimeUnit = perWaitingTimeUnit;
			this.perServiceTimeUnit = perServiceTimeUnit;
		}

	}

	private VehicleCostInformation vehicleCostInformation;

	private int capacity;
	
	private CarrierVehicleType(Builder builder){
		super(new VehicleTypeImpl(builder.typeId));
		this.vehicleCostInformation = new VehicleCostInformation(builder.fix, builder.perDistanceUnit, builder.perTransportTimeUnit, builder.perWaitingTimeUnit, builder.perServiceTimeUnit);
		if(builder.engineInfo != null) super.setEngineInformation(builder.engineInfo);
		if(builder.description != null) super.setDescription(builder.description);
		capacity = builder.capacity;
		super.setMaximumVelocity(builder.maxVeloInMeterPerSeconds);
	}

	/**
	 * Returns the cost values for this vehicleType.
	 * 
	 * If cost values are not explicitly set, the defaults are [fix=0.0][perDistanceUnit=1.0][perTransportTimeUnit=0.0][perWaitingTimeUnit=0.0][perServiceTimeUnit=0.0].
	 * 
	 * @return vehicleCostInformation
	 */
	public VehicleCostInformation getVehicleCostInformation() {
		return vehicleCostInformation;
	}
	
	/**
	 * Returns the capacity of carrierVehicleType.
	 * 
	 * <p>This might be replaced in future by a more complex concept of capacity (considering volume and different units).
	 * 
	 * @return integer
	 */
	public int getCarrierVehicleCapacity(){
		return capacity;
	}
	
}
