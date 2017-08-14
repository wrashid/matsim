package playground.balac.utils.routing;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.transformations.CH1903LV03PlustoWGS84;

public class RandomODCreator {

	public static void main(String[] args) throws IOException {

		double centerX = 2683217.0; 
		double centerY = 1247300.0;
		
		
		final BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\balacm\\Desktop\\od_pairs.csv"));
		CH1903LV03PlustoWGS84 transform = new CH1903LV03PlustoWGS84();
		int i = 2000;
		while (i > 0) {
			
			double x1 = (0.5 - MatsimRandom.getRandom().nextDouble()) * 30000 + centerX;
			
			double y1 = (0.5 - MatsimRandom.getRandom().nextDouble()) * 30000 + centerY;
			
			Coord coord1 = CoordUtils.createCoord(x1, y1);
			coord1 = transform.transform(coord1);
			double x2 = (0.5 - MatsimRandom.getRandom().nextDouble()) * 30000 + centerX;
			
			double y2 = (0.5 - MatsimRandom.getRandom().nextDouble()) * 30000 + centerY;
			
			Coord coord2 = CoordUtils.createCoord(x2, y2);
			coord2 = transform.transform(coord2);

			int time = MatsimRandom.getRandom().nextInt(18 * 60) + 6 * 60;
			writer.write(Integer.toString(i) + ";" + Double.toString(x1) + ";" + Double.toString(y1) + ";" +
					Double.toString(x2) + ";" + Double.toString(y2) + ";" + Double.toString(coord1.getX()) + ";" +
					Double.toString(coord1.getY()) + ";" +
					Double.toString(coord2.getX()) + ";" + Double.toString(coord2.getY()) + ";" +Integer.toString(time));
			writer.newLine();
			i--;
			
		}
		writer.flush();
		writer.close();
		
	}

}
