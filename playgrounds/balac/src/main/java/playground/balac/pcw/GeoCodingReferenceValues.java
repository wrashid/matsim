package playground.balac.pcw;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import org.matsim.api.core.v01.Coord;

import org.matsim.core.utils.geometry.transformations.WGS84toCH1903LV03;
import org.matsim.core.utils.io.IOUtils;

import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;

public class GeoCodingReferenceValues {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		 BufferedReader readLink = IOUtils.getBufferedReader("C:\\LocalDocuments\\Projects\\MikrozensusEnrichment\\MikrozensusTrips.csv");
		 
		BufferedWriter outLink = IOUtils.getBufferedWriter("C:\\LocalDocuments\\Projects\\MikrozensusEnrichment\\MikrozensusTrips_geocoded_10000_20000.csv");
		outLink.write("HHNR;WEGNR;lat_start;lon_start;lat_end;lon_end;xcoord_start;ycoord_start;xcoord_end;ycoord_end;dist;sex;age;time");
		outLink.newLine();
		WGS84toCH1903LV03 transformation = new WGS84toCH1903LV03();
		
		 readLink.readLine();
		 String s = readLink.readLine();
		 int i = 1;
		 while(s != null) {
			
			 if (i >10000 && i <= 20000) {
			
				 String[] arr = s.split(";");
				 
				 
				String addressStart = arr[19];
				String streetNumberStart = arr[20];
				String codeStart = arr[17];
				String cityStart = arr[18];
				
				String addressEnd = arr[25];
				String streetNumberEnd = "";
				if (arr.length == 27)
					streetNumberEnd = arr[26];
				String codeEnd = arr[23];
				String cityEnd = arr[24];
	
				GeoApiContext context = new GeoApiContext().setApiKey(args[0]);
				GeocodingResult[] results =  GeocodingApi.geocode(context,
				    addressStart
				    + " " +	 streetNumberStart 
				    + ", " + cityStart+ ", " + codeStart ).await();
				
				GeocodingResult[] results1 =  GeocodingApi.geocode(context,
						addressEnd
					    + " " +	 streetNumberEnd 
					    + ", " + cityEnd+ ", " + codeEnd).await();
				if (results.length > 0 && results1.length > 0) {			
				
					Coord coordStartT = new Coord(results[0].geometry.location.lng, results[0].geometry.location.lat);
					
					Coord coordStart =  transformation.transform(coordStartT);
							
					Coord coordEndT = new Coord(results1[0].geometry.location.lng, results1[0].geometry.location.lat);
					
					Coord coordEnd =  transformation.transform(coordEndT);
					
					int Radius = 6371;
					
					double lat1 = Math.toRadians(results[0].geometry.location.lat);
			        double lat2 = Math.toRadians(results1[0].geometry.location.lat);
			        double lon1 = Math.toRadians(results[0].geometry.location.lng);
			        double lon2 = Math.toRadians(results1[0].geometry.location.lng);
					
					double x = Math.cos(lat1);
			        double y = Math.cos(lat2);
			        
			        double z = (lon2 - lon1) / 2.0;
			        
			        double k = (lat2 - lat1) / 2.0;
			        
			        double l = Math.sin(z) * Math.sin(z);
			        
					double distance = 2 * Radius * Math.asin(Math.sqrt(Math.sin((k) * Math.sin(k)) + x * y * l));
					
					outLink.write(arr[3] + ";" + arr[4] + ";" + results[0].geometry.location.lat + ";" + results[0].geometry.location.lng 
							+ ";" + results1[0].geometry.location.lat + ";" + results1[0].geometry.location.lng);
					
					outLink.write(";" + Double.toString(coordStart.getX()) + ";" + Double.toString(coordStart.getY()) 
							+ ";" + Double.toString(coordEnd.getX()) + ";" + Double.toString(coordEnd.getY()) + ";" + Double.toString(distance)
							
							 + ";" + arr[1]+ ";" + arr[0] + ";" + arr[7]);
					outLink.newLine();
				
				}
				else {
					
					outLink.write(arr[3] + ";" + arr[4] + ";" + "-99" + ";" + "-99" + ";" + "-99" + ";" + "-99" + ";" + "-99" + ";" + "-99" + ";" + "-99" + ";" + "-99" + ";" + "-99" + ";" + "-99" + ";" + "-99" + ";" + "-99");
					outLink.newLine();
				}
			 }
			 i++;
			s = readLink.readLine();
		 }
		 
		 outLink.flush();
		 outLink.close();
		 readLink.close();
		

	}

}
