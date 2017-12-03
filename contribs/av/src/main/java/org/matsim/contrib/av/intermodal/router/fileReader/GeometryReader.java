package org.matsim.contrib.av.intermodal.router.fileReader;

import java.net.URL;
import java.util.Map;

import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.prep.PreparedPolygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class GeometryReader {
	
	public static void readShapeFileAndExtractGeometry(URL fileURL, String key, Map<String, PreparedPolygon> geometries){
		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(fileURL.getFile())) {
			
				GeometryFactory geometryFactory= new GeometryFactory();
				WKTReader wktReader = new WKTReader(geometryFactory);

				try {
					Geometry geo = wktReader.read((ft.getAttribute("the_geom")).toString());
					MultiPolygon poly = (MultiPolygon) geo;
					PreparedPolygon preparedPoly = new PreparedPolygon(poly);
					String lor = ft.getAttribute(key).toString();
					geometries.put(lor, preparedPoly);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			 
		}	
	}

}
