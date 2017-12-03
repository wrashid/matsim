package org.matsim.contrib.av.intermodal.router.fileReader;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.matsim.api.core.v01.Coord;

public class Coord2SurchargeFileReader {
	
	public static void readAndAddCoord2SurchargeFromFile(URL fileURL, Map<Coord, Double> discouragedCoord2TimeSurchargeFixed,
			Map<Coord, Double> discouragedCoord2TimeSurchargeOnOff){
		try {
			BufferedReader coords2SurchargeReader = new BufferedReader(new FileReader(fileURL.getFile()));
			String line;
			// check header
			if (coords2SurchargeReader.readLine().equals("coordX,coordY,timeSurcharge,type")) {
				while((line = coords2SurchargeReader.readLine()) != null){
					// ignore comments after "//"
					String[] lineSplits = line.split("//")[0].split(",");
					if(lineSplits.length == 4) { // else ignore
						Coord coord = new Coord(Double.parseDouble(lineSplits[0]), Double.parseDouble(lineSplits[1]));
						if(lineSplits[3].equals("fixed")){
							discouragedCoord2TimeSurchargeFixed.put(coord, Double.parseDouble(lineSplits[2]));
						} else if (lineSplits[3].equals("onOff")) {
							discouragedCoord2TimeSurchargeOnOff.put(coord, Double.parseDouble(lineSplits[2]));
						} else {
							throw new RuntimeException("unknown discouragedCoord2TimeSurcharge type: " + 
									lineSplits[3]);
						}
					}
				}
			} else {
				throw new RuntimeException("unknown header in Coord2SurchargeFile, should be: coordX,coordY,timeSurcharge,type");
			}
			coords2SurchargeReader.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
