package playground.balac.test;

public class ActivityData {
	
	String id;
	String type;
	double startTime = 0.0;
	double endTime = 0.0;
	double prefTime = 0.0;
	String mode;
	
	
	public String toString() {
		return id +  ";" + type +  ";" + startTime +  ";" + endTime +  ";" + mode;
		
		
	}

}
