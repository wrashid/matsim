package playground.balac.utils.parking;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import org.matsim.api.core.v01.Coord;

import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.IOUtils;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;

public class GeocodingParkingAddresses {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		 BufferedReader readLink = IOUtils.getBufferedReader("C:\\Users\\balacm\\Downloads\\ParkingStudyData (1).csv");
		 
		BufferedWriter outLink = IOUtils.getBufferedWriter("C:\\Users\\balacm\\Downloads\\ParkingStudyData_enriched (1).csv");
		outLink.write("personID;date;time;startAddress;endAddress;plates;permit;emptySpaces;lat_start;lon_start;lat_end;lon_end");
		outLink.newLine();
//		WGS84toCH1903LV03 transformation = new WGS84toCH1903LV03();
		
		 readLink.readLine();
		 String s = readLink.readLine();
		 while(s != null) {
			
			 
			
			 String[] arr = s.split(";");
			 
			 
			String addressStart = arr[3];
			String codeStart = "";
			String cityStart = "Zuerich";
			
			String addressEnd = arr[4];
			String streetNumberEnd = "";
			String codeEnd = "";
			String cityEnd = "Zuerich";

			GeoApiContext context = new GeoApiContext().setApiKey(args[0]);
			GeocodingResult[] results =  GeocodingApi.geocode(context,
			    addressStart
			    + " " 
			    + ", " + cityStart+ ", " + codeStart ).await();
			
			GeocodingResult[] results1 =  GeocodingApi.geocode(context,
					addressEnd
				    + " " +	 streetNumberEnd 
				    + ", " + cityEnd+ ", " + codeEnd).await();
			if (results.length > 0 && results1.length > 0) {				
						
				
				outLink.write(s + ";" +  results[0].geometry.location.lat + ";" + results[0].geometry.location.lng 
						+ ";" + results1[0].geometry.location.lat + ";" + results1[0].geometry.location.lng);
				
				outLink.newLine();
			
			}
			else {
				
				outLink.write(s + ";" + "-99" + ";" + "-99" + ";" + "-99" + ";" + "-99");
				outLink.newLine();
			}			 
			s = readLink.readLine();
		 }
		 
		 outLink.flush();
		 outLink.close();
		 readLink.close();
		

	}

}
