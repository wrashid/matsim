package org.matsim.contrib.carsharing.vehicles;
/** 
 * @author balac
 */
public class FFVehicleImpl implements CSVehicle{
	
	private String type;
	private String vehicleId;
	private String companyId;
	private String csType;
	
	public FFVehicleImpl(String type, String vehicleId, String companyId, String csType) {
		
		this.type = type;
		this.csType = csType;
		this.vehicleId = vehicleId;
		this.companyId = companyId;
	}

	@Override
	public String getType() {
		return type;
	}
	
	@Override
	public String getVehicleId() {
		return vehicleId;
	}
	@Override
	public String getCsType() {
		return csType;
	}
	@Override
	public String getCompanyId() {
		return companyId;
	}
	

	

}
