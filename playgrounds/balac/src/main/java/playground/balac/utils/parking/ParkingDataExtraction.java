package playground.balac.utils.parking;

import java.io.BufferedReader;
import java.io.BufferedWriter;

import org.matsim.core.utils.io.IOUtils;

public class ParkingDataExtraction {

	public static void main(String[] args) throws Exception {
		
		BufferedReader readLink = IOUtils.getBufferedReader("C:\\Users\\balacm\\Desktop\\ParkingStudyData_enriched.csv");

		BufferedWriter outLink = IOUtils.getBufferedWriter("C:\\Users\\balacm\\Desktop\\ParkingStudyData_separated.csv");
			
		readLink.readLine();
		String s = readLink.readLine();
		int count = 0;
		while(s != null) {
			String[] arr = s.split(";");
			
			String plates = arr[5];
			String permits = arr[6];
			
			String[] platesArr = plates.split(",");
			String[] permitsArr = permits.split(",");
			
			if (platesArr.length != permitsArr.length) {
				count++;
				System.out.println(platesArr.length - permitsArr.length);
				System.out.println(s);
			}
			else {
				int i = 0;
				for (String plate : platesArr) {
					outLink.write(plate + ";" + permitsArr[i]);
					outLink.newLine();
					i++;

				}
				
			}
			s = readLink.readLine();
		}
		outLink.flush();
		outLink.close();
		readLink.close();
		System.out.println(count);
	}
}
