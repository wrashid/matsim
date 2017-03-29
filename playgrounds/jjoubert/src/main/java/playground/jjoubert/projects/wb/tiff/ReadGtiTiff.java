/* *********************************************************************** *
 * project: org.matsim.*
 * ReaGtidTif.java                                                                        *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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
/**
 * 
 */
package playground.jjoubert.projects.wb.tiff;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.spi.IIORegistry;
import javax.imageio.stream.ImageInputStream;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.Counter;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.github.jaiimageio.impl.plugins.tiff.TIFFImageMetadata;

import mil.nga.tiff.FieldTagType;
import mil.nga.tiff.FileDirectory;
import mil.nga.tiff.FileDirectoryEntry;
import mil.nga.tiff.ImageWindow;
import mil.nga.tiff.Rasters;
import mil.nga.tiff.TIFFImage;
import mil.nga.tiff.TiffReader;
import mil.nga.tiff.io.ByteReader;
import mil.nga.tiff.util.TiffConstants;
import playground.southafrica.utilities.Header;

/**
 * Class to read the TIF file provided by GeoTerraImage.
 * 
 * @author jwjoubert
 */
public class ReadGtiTiff {
	final private static Logger LOG = Logger.getLogger(ReadGtiTiff.class);

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Header.printHeader(ReadGtiTiff.class.toString(), args);
//		runTiff(args);
//		runGdal(args);
		runBrianExample(args[0], args[1]);
		
		Header.printFooter();
	}

	public static void runGdal(String[] args){
		String filename = args[0];

		Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("tif");
		ImageReader reader = (ImageReader)readers.next();
		LOG.info("Ignoring metadata: " + reader.isIgnoringMetadata());
		File file = new File(filename);
		try {
			ImageInputStream iis = ImageIO.createImageInputStream(file);
			reader.setInput(iis, true, false);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		/* Get the metadata. */
		IIOMetadata metadata = null;
		try {
			metadata = reader.getImageMetadata(0);
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		LOG.info("Metadata format names:");
		String[] sa = metadata.getMetadataFormatNames();
		for(String s : sa){
			LOG.info(s);
		}
		Node n = metadata.getAsTree(TIFFImageMetadata.nativeMetadataFormatName);
		displayMetadata(n);


		BufferedImage bi = null;
		try {
			bi = ImageIO.read(new File(filename));
		} catch (IOException e) {
			e.printStackTrace();
		}

		bi.flush();
	}
	
	

	public static void displayMetadata(Node root) {
		displayMetadata(root, 0);
	}

	static void indent(int level) {
		for (int i = 0; i < level; i++) {
			System.out.print("  ");
		}
	} 

	static void displayMetadata(Node node, int level) {
		indent(level); // emit open tag
		System.out.print("<" + node.getNodeName());
		NamedNodeMap map = node.getAttributes();
		if (map != null) { // print attribute values
			int length = map.getLength();
			for (int i = 0; i < length; i++) {
				Node attr = map.item(i);
				System.out.print(" " + attr.getNodeName() +
						"=\"" + attr.getNodeValue() + "\"");
			}
		}

		Node child = node.getFirstChild();
		if (child != null) {
			System.out.println(">"); // close current tag
			while (child != null) { // emit child tags recursively
				displayMetadata(child, level + 1);
				child = child.getNextSibling();
			}
			indent(level); // emit close tag
			System.out.println("</" + node.getNodeName() + ">");
		} else {
			System.out.println("/>");
		}
	}


	public static void runTiff(String[] args){
		String tiffFilename = args[0];
		LOG.info("Reading TIFF file from " + tiffFilename);

		File tiffFile = new File(tiffFilename);
		TIFFImage tiffImage = null;
		try {
			tiffImage = TiffReader.readTiff(tiffFile, true);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot read TIFF image " + tiffFile);
		}
		LOG.info("Done reading file.");
		
		for(FileDirectory fileDirectory : tiffImage.getFileDirectories()){
			FileDirectoryEntry entry = fileDirectory.get(FieldTagType.GeoKeyDirectory);
			if(entry != null){
				LOG.info("Found the GeoKeyDirectory");
				if(entry.getValues() instanceof ArrayList<?>){
					ArrayList<?> list = (ArrayList<?>)entry.getValues();
					Object o = list.get(0);
				}
			}
		}
		

		FileDirectory fd = tiffImage.getFileDirectories().get(0);
		ImageWindow iw = new ImageWindow(fd);
		LOG.info("Min x: " + iw.getMinX());
		LOG.info("Max x: " + iw.getMaxX());
		LOG.info("Min y: " + iw.getMinY());
		LOG.info("Max y: " + iw.getMaxY());
		LOG.info("Number of samples per pixel: " + fd.getSamplesPerPixel());
		Rasters rasters = fd.readRasters();
		fd.get(FieldTagType.ExifIFD);

		/* Figure out the geo coordinates of the image, or a pixel. */
		LOG.info("Resolution unit: " + fd.getResolutionUnit());		

		LOG.info("IsTiled(): " + fd.isTiled());
		Map<Short,Integer> map = new TreeMap<Short, Integer>();
		for(int x = 0; x < rasters.getWidth(); x++){
			for(int y = 0; y < rasters.getHeight(); y++){
				Number[] num = rasters.getPixel(x, y);
				if(num.length > 1){
					LOG.warn("Pixel value has length " + num.length);
				}
				if(num[0] instanceof Short){
					short s = (short)num[0];
					if(!map.containsKey(s)){
						map.put(s, 1);
					} else{
						int old = map.get(s);
						map.put(s, old+1);
					}
				} else{
					LOG.warn("Pixel does not have a value of type `short`, but " + num[0].getClass().toString());
				}

				/* Get the coordinates of the pixel. */
			}
		}
		LOG.info("Pixel values: value (number of instances)");
		for(short s : map.keySet()){
			LOG.info(String.format("  %3d (%d)", s, map.get(s)));
		}

	}
	
	private static void runBrianExample(String geotiffFile, String rFile){
		GeoTiffImage gti = new GeoTiffImage(new File(geotiffFile));
		gti.convertImageToRTwo(rFile);
	}
	
	
	
	private static class GeoTiffImage{
		private double originLongitude;
		private double originLatitude;
		private double incrementLongitude;
		private double incrementLatitude;
		private Map<Short, Color> colorMap;
		private TIFFImage image;
		
		public GeoTiffImage(File geoTiffFile) {
			this.image = null;
			try {
				this.image = TiffReader.readTiff(geoTiffFile);
			} catch (IOException e) {
				throw new RuntimeException("Cannot read GeoTIFF file " + geoTiffFile.getAbsolutePath());
			}  
			
			FileDirectoryEntry modelPixelScale = null;;
			FileDirectoryEntry modelTiePoint = null;
			FileDirectoryEntry geoDoubleParams;
			FileDirectoryEntry geoAsciiParams;
			FileDirectoryEntry colorMapEntry = null;
			FileDirectoryEntry photometric = null;
			for(FileDirectory fileDirectory: this.image.getFileDirectories()){ 
				FileDirectoryEntry geoKeyDirectory = fileDirectory.get(FieldTagType.GeoKeyDirectory); 
				if(geoKeyDirectory != null){ 
					// Parse the keys out of the values 
					// values[3] is the number of keys defined 
					// Similar to https://github.com/constantinius/geotiff.js/blob/master/src/geotiff.js parseGeoKeyDirectory method  
					// Parse those keys similar to https://github.com/constantinius/geotiff.js/blob/master/src/geotiffimage.js  
					// Parsing those keys would use information from these tags 
					modelPixelScale = fileDirectory.get(FieldTagType.ModelPixelScale); 
					modelTiePoint = fileDirectory.get(FieldTagType.ModelTiepoint); 
					geoDoubleParams = fileDirectory.get(FieldTagType.GeoDoubleParams); 
					geoAsciiParams = fileDirectory.get(FieldTagType.GeoAsciiParams); 
					colorMapEntry = fileDirectory.get(FieldTagType.ColorMap);
					photometric = fileDirectory.get(FieldTagType.PhotometricInterpretation);
				LOG.info("Done reading field tags");
				}
			}
			
			/* Get the origin. Ignoring elevation. */
			Object o = modelTiePoint.getValues();
			if(o instanceof ArrayList<?>){
				ArrayList<?> mtp = (ArrayList<?>) o;
				originLongitude = (double) mtp.get(3);
				originLatitude = (double) mtp.get(4);
			} else{
				throw new IllegalArgumentException("Model tie points object is not of type ArrayList<?>");
			}
			
			/* Get the pixel increments. */
			Object oo = modelPixelScale.getValues();
			if(oo instanceof ArrayList<?>){
				ArrayList<?> mtp = (ArrayList<?>) oo;
				incrementLongitude = (double) mtp.get(0);
				incrementLatitude = (double) mtp.get(1);
			} else{
				throw new IllegalArgumentException("Model pixel scales object is not of type ArrayList<?>");
			}
			
			/* Get the field colors from the colormap. */
			colorMap = new TreeMap<>();
			double intensity = Math.pow(2, 16);
			Object ooo = colorMapEntry.getValues();
			if(ooo instanceof ArrayList<?>){
				ArrayList<?> cma = (ArrayList<?>) ooo;
				for(int i = 0; i < cma.size()/3; i++){
					int r = (int) Math.round(( Double.valueOf(cma.get(i).toString()) / intensity)*255);
					int g = (int) Math.round(( Double.valueOf(cma.get(i+(1*256)).toString()) / intensity)*255);
					int b = (int) Math.round(( Double.valueOf(cma.get(i+(2*256)).toString()) / intensity)*255);
					
					Color c = new Color(r, g, b);
					short index = (short) i;
					if(!colorMap.containsKey(index)){
						colorMap.put(index, c);
					}
				}
			}
			
//			LOG.info("Done");
		}
		
		private void convertImageToR(String filename){
			Counter counter = new Counter("  pixels # ");
			
			BufferedWriter bw = IOUtils.getBufferedWriter(filename);
			try{
				bw.write("xMin,yMin,xMax,yMax,value");
				bw.newLine();
				
				/* Process each pixel. */
				Rasters rasters = this.image.getFileDirectory(0).readRasters();
				for(int x = 0; x < rasters.getWidth(); x++){
					for(int y = 0; y < rasters.getHeight(); y++){
						Number[] num = rasters.getPixel(x, y);
						if(num.length > 1){
							LOG.warn("Pixel value has length " + num.length);
						}
						short s = 0;
						if(num[0] instanceof Short){
							s = (short)num[0];
						} else{
							LOG.warn("Pixel does not have a value of type `short`, but " + num[0].getClass().toString());
						}
						
						/* Get the coordinates of the pixel. */
						double[] da = getCoordArray(x, y);
						bw.write(String.format("%.10f,%.10f,%.10f,%.10f,%d\n", 
								da[0], da[1], da[2], da[3], s ));
						counter.incCounter();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot write to " + filename);
			} finally{
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot close " + filename);
				}
			}
			counter.printCounter();
		}
		
		private void convertImageToRTwo(String filename){
			Counter counter = new Counter("  pixels # ");
			
			BufferedWriter bw = IOUtils.getBufferedWriter(filename);
			try{
				bw.write("poly,lon,lat,value,r,g,b");
				bw.newLine();
				
				/* Process each pixel. */
				int index = 0;
				Rasters rasters = this.image.getFileDirectory(0).readRasters();
				for(int x = 0; x < rasters.getWidth(); x++){
					for(int y = 0; y < rasters.getHeight(); y++){
						Number[] num = rasters.getPixel(x, y);
						if(num.length > 1){
							LOG.warn("Pixel value has length " + num.length);
						}
						short s = 0;
						if(num[0] instanceof Short){
							s = (short)num[0];
						} else{
							LOG.warn("Pixel does not have a value of type `short`, but " + num[0].getClass().toString());
						}
						
						/* Get the coordinates of the pixel. */
						double[] da = getCoordArray(x, y);
						Color c = this.colorMap.get(s);
						
						bw.write(String.format("%d,%.10f,%.10f,%d,%d,%d,%d\n", index, da[0], da[1], s, c.getRed(), c.getGreen(), c.getBlue()));
						bw.write(String.format("%d,%.10f,%.10f,%d,%d,%d,%d\n", index, da[2], da[1], s, c.getRed(), c.getGreen(), c.getBlue()));
						bw.write(String.format("%d,%.10f,%.10f,%d,%d,%d,%d\n", index, da[2], da[3], s, c.getRed(), c.getGreen(), c.getBlue()));
						bw.write(String.format("%d,%.10f,%.10f,%d,%d,%d,%d\n", index, da[0], da[3], s, c.getRed(), c.getGreen(), c.getBlue()));
						bw.write(String.format("%d,%.10f,%.10f,%d,%d,%d,%d\n", index, da[0], da[1], s, c.getRed(), c.getGreen(), c.getBlue()));
						
						counter.incCounter();
						index++;
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				throw new RuntimeException("Cannot write to " + filename);
			} finally{
				try {
					bw.close();
				} catch (IOException e) {
					e.printStackTrace();
					throw new RuntimeException("Cannot close " + filename);
				}
			}
			counter.printCounter();
		}
		
		/**
		 * Returns the coordinates of the bottom-left and top-right corners of 
		 * the pixel, in the format [xmin, ymin, xmax, ymax]. 
		 * @param x
		 * @param y
		 * @return
		 */
		public double[] getCoordArray(int x, int y){
			double[] da = {
					this.originLongitude + x*this.incrementLongitude,
					this.originLatitude - (y+1)*this.incrementLatitude,
					this.originLongitude + (x+1)*this.incrementLongitude,
					this.originLatitude - y*this.incrementLatitude,
			};
			return da;
		}
	}
	
	
	
	
	
	private class GeoKeys{
		private final short keydirectoryVersion;
		private final short keyRevision;
		private final short minorRevision;
		private final short numberOfKeys;
		
		public GeoKeys(short directoryVersion, short keyRevision, 
				short minorRevision, short numberOfKeys) {
			this.keydirectoryVersion = directoryVersion;
			this.keyRevision = keyRevision;
			this.minorRevision = minorRevision;
			this.numberOfKeys = numberOfKeys;
		}
	}
	

}
