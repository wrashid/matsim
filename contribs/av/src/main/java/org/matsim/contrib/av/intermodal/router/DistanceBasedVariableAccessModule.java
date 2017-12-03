/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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
package org.matsim.contrib.av.intermodal.router;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.av.intermodal.router.config.VariableAccessConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.geometry.geotools.MGC;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.prep.PreparedPolygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * TODO: checkCarAvail
 * @author  jbischoff
 * @author	vsp-gleich
 */
/**
 *
 */
public class DistanceBasedVariableAccessModule implements VariableAccessEgressTravelDisutility {

	
	private Map<String,Boolean> teleportedModes = new HashMap<>();
	private Map<Integer,String> distanceMode = new TreeMap<>();
	private Map<String, PreparedPolygon> geometriesVariableAccessArea = new HashMap<>();
	/* 
	 * Time surcharges are only applied to trips starting or ending in the variable access area.
	 * There are to types available: fixed and onOff time surcharges
	 * 
	 * It can be used to force the TransitRouter to search a route avoiding the TransitStop
	 * located at the coordinate, e.g. if that stop is inaccessible due to a river between the 
	 * variable access area and the TransitStop (-> fixed) or if the TransitStop is served very 
	 * infrequently leading to excessive pt wait time (-> onOff, so routes via this TransitStop 
	 * can be returned, but while rerouting at least one route avoiding it is returned as well).
	 * 
	 * If fromCoord and toCoord are subject to time surcharges these are added up. Also if the
	 * same coord is subject to both a fixed and an onOff time surcharge, both are added up.
	 */
	/** fixed time surcharge: full time surcharge always applied */
	private Map<Coord, Double> discouragedCoord2TimeSurchargeFixed = new HashMap<>();
	/** onOff time surcharge: alternate between no or full time surcharge applied */
	private Map<Coord, Double> discouragedCoord2TimeSurchargeOnOff = new HashMap<>();
	private final Random rand = MatsimRandom.getRandom();
	private final String variableAccessStyle;
	private final double maxDistanceOnlyTransitWalkAvailable;
	private boolean checkCarAvail = true;
	
	private final Network carnetwork;
	private final Config config;
	
	/**
	 * 
	 */
	public DistanceBasedVariableAccessModule(Network carnetwork, Config config) {
		this.config = config;
		this.carnetwork = carnetwork;
		VariableAccessConfigGroup vaconfig = (VariableAccessConfigGroup) config.getModules().get(VariableAccessConfigGroup.GROUPNAME);
		if (vaconfig.getVariableAccessAreaShpFile() != null && vaconfig.getVariableAccessAreaShpKey() != null) {
			geometriesVariableAccessArea = readShapeFileAndExtractGeometry(
					vaconfig.getVariableAccessAreaShpFileURL(config.getContext()), vaconfig.getVariableAccessAreaShpKey());
		}
		if (vaconfig.getCoords2TimeSurchargeFile() != null) {
			readCoord2SurchargeFile(vaconfig.getCoords2TimeSurchargeFileURL(config.getContext()));
		}
		variableAccessStyle = vaconfig.getStyle();
		if (!(variableAccessStyle.equals("fixed") || variableAccessStyle.equals("flexible"))) {
			throw new RuntimeException("Unsupported Style");
		}
		maxDistanceOnlyTransitWalkAvailable = vaconfig.getMaxDistanceOnlyTransitWalkAvailable();
		teleportedModes.put(TransportMode.transit_walk, true);
	}
	/**
	 * 
	 * @param mode the mode to register
	 * @param maximumAccessDistance maximum beeline Distance for using this mode
	 * @param isTeleported defines whether this is a teleported mode
	 * @param lcp for non teleported modes, some travel time assumption is required
	 */
	public void registerMode(String mode, int maximumAccessDistance, boolean isTeleported){
		if (this.distanceMode.containsKey(maximumAccessDistance)){
			throw new RuntimeException("Maximum distance of "+maximumAccessDistance+" is already registered to mode "+distanceMode.get(maximumAccessDistance)+" and cannot be re-registered to mode: "+mode);
		} else if (this.distanceMode.containsValue(mode)) {
			throw new RuntimeException("mode "+mode+"is already registered. Check your config four double entries..");
		}
		if (isTeleported){
			teleportedModes.put(mode, true);
			distanceMode.put(maximumAccessDistance, mode);
		} else {
			teleportedModes.put(mode, false);
			distanceMode.put(maximumAccessDistance, mode);
		}
	}
	
	
	/* (non-Javadoc)
	 * @see playground.jbischoff.pt.VariableAccessEgressTravelDisutility#getAccessEgressModeAndTraveltime(org.matsim.api.core.v01.population.Person, org.matsim.api.core.v01.Coord, org.matsim.api.core.v01.Coord)
	 */
	@Override
	public Leg getAccessEgressModeAndTraveltime(Person person, Coord coord, Coord toCoord, double time, boolean variableSurchargeOn) {
		double egressDistance = CoordUtils.calcEuclideanDistance(coord, toCoord);
		// return usual transit walk if the access / egress leg has neither origin nor destination in the area where variable access shall be used
		String mode = TransportMode.transit_walk;
		double discouragedCoordTimeSurcharge = 0;
		boolean isStartInVariableAccessArea = isInVariableAccessArea(coord);
		boolean isEndInVariableAccessArea = isInVariableAccessArea(toCoord);
		if (isStartInVariableAccessArea || isEndInVariableAccessArea) {
			if (discouragedCoord2TimeSurchargeFixed.containsKey(coord)) {
				discouragedCoordTimeSurcharge += discouragedCoord2TimeSurchargeFixed.get(coord);
			} 
			if (discouragedCoord2TimeSurchargeFixed.containsKey(toCoord)) {
				discouragedCoordTimeSurcharge += discouragedCoord2TimeSurchargeFixed.get(toCoord);
			}
			if (discouragedCoord2TimeSurchargeOnOff.containsKey(coord) && variableSurchargeOn) {
				discouragedCoordTimeSurcharge += discouragedCoord2TimeSurchargeOnOff.get(coord);
			}
			if (discouragedCoord2TimeSurchargeOnOff.containsKey(toCoord) && variableSurchargeOn) {
				discouragedCoordTimeSurcharge += discouragedCoord2TimeSurchargeOnOff.get(toCoord);
			}
		}
		if (isStartInVariableAccessArea && isEndInVariableAccessArea){
			if (variableAccessStyle.equals("fixed")) {
				mode = getModeForDistanceFixedStyle(egressDistance);				
			} else {
				mode = getModeForDistanceFlexibleStyle(egressDistance, person);		
			}
		}

		Link startLink = NetworkUtils.getNearestLink(carnetwork, coord);
		Link endLink = NetworkUtils.getNearestLink(carnetwork, toCoord);
		if (startLink.equals(endLink)) {
			mode = TransportMode.transit_walk;
		}
		Leg leg = PopulationUtils.createLeg(mode);
		Route route = RouteUtils.createGenericRouteImpl(startLink.getId(), endLink.getId());
		leg.setRoute(route);
		if (this.teleportedModes.get(mode)){
			double distf;
			double speed;
			// RoutingParams for transit_walk are not accessible here, but equal those for access_walk
			if(mode.equals(TransportMode.transit_walk)){
				distf = config.plansCalcRoute().getModeRoutingParams().get(TransportMode.access_walk).getBeelineDistanceFactor();
				speed = config.plansCalcRoute().getModeRoutingParams().get(TransportMode.access_walk).getTeleportedModeSpeed();
			} else {				
				distf = config.plansCalcRoute().getModeRoutingParams().get(mode).getBeelineDistanceFactor();
				speed = config.plansCalcRoute().getModeRoutingParams().get(mode).getTeleportedModeSpeed();
			}
			double distance = egressDistance*distf;
			double travelTime = distance / speed + discouragedCoordTimeSurcharge;
			leg.setTravelTime(travelTime);
			route.setDistance(distance);
			leg.setDepartureTime(time);
			
						
		} else {
			double distance = egressDistance*1.3;
			double travelTime = distance / 7.25 + discouragedCoordTimeSurcharge;
			leg.setTravelTime(travelTime);
			route.setDistance(distance);
			
//			too expensive
//			Path path = this.lcpPerNonTeleportedMode.get(mode).calcLeastCostPath(startLink.getFromNode(), endLink.getToNode(), 0, person, null);
//			route.setDistance(path.travelCost);
//			route.setTravelTime(path.travelTime);
		}
		return leg;
	}

	/**
	 * @param egressDistance
	 * @return
	 */
	private String getModeForDistanceFixedStyle(double egressDistance) {
		for (Entry<Integer, String> e : this.distanceMode.entrySet()){
			if (e.getKey()>=egressDistance){
//				System.out.println("Mode" + e.getValue()+" "+egressDistance);
				return e.getValue();
			}
		}
		throw new RuntimeException(egressDistance + " m is not covered by any egress / access mode.");
		
	}
	
	/**
	 * @param egressDistance
	 * @return
	 */
	private String getModeForDistanceFlexibleStyle(double egressDistance, Person p) {
		if (egressDistance <= maxDistanceOnlyTransitWalkAvailable) return TransportMode.transit_walk;
		//TODO: MAke Config switch
		List<String> possibleModes = new ArrayList<>();
		for (Entry<Integer,String> e : this.distanceMode.entrySet()){
			if (e.getKey()>=egressDistance){
				if (e.getValue().equals(TransportMode.car)){
					if ((checkCarAvail)&&(p!=null)&&(p.getCustomAttributes()!=null)){
						
						String carA  = PersonUtils.getCarAvail(p);
						if (carA!=null){
						if (carA.equals("always")||carA.equals("sometimes")){
							possibleModes.add(e.getValue());
						}
						}
						else {				
							possibleModes.add(e.getValue());
						}
					}
					else {
						possibleModes.add(e.getValue());
					}
				}
				else {
					possibleModes.add(e.getValue());
				}
			}
		}
		if (possibleModes.size()<1){
//			Logger.getLogger(getClass()).warn("Egress distance "+egressDistance+ " is not covered by any available mode mode for person "+p.getId()+". Assuming walk.");
			return (TransportMode.transit_walk);
		}
		return possibleModes.get(rand.nextInt(possibleModes.size()));
	}


	/* (non-Javadoc)
	 * @see playground.jbischoff.pt.VariableAccessEgressTravelDisutility#isTeleportedAccessEgressMode(java.lang.String)
	 */
	@Override
	public boolean isTeleportedAccessEgressMode(String mode) {
		return this.teleportedModes.get(mode);
	}
	
	public static Map<String, PreparedPolygon> readShapeFileAndExtractGeometry(URL fileURL, String key){
		Map<String,PreparedPolygon> geometry = new HashMap<>();	
		for (SimpleFeature ft : ShapeFileReader.getAllFeatures(fileURL.getFile())) {
			
				GeometryFactory geometryFactory= new GeometryFactory();
				WKTReader wktReader = new WKTReader(geometryFactory);

				try {
					Geometry geo = wktReader.read((ft.getAttribute("the_geom")).toString());
					MultiPolygon poly = (MultiPolygon) geo;
					PreparedPolygon preparedPoly = new PreparedPolygon(poly);
					String lor = ft.getAttribute(key).toString();
					geometry.put(lor, preparedPoly);
				} catch (ParseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			 
		}	
		return geometry;
	}
	
	private boolean isInVariableAccessArea(Coord coord){
		if(geometriesVariableAccessArea.size() > 0){
			for(String name: geometriesVariableAccessArea.keySet()){
				if(geometriesVariableAccessArea.get(name).contains(MGC.coord2Point(coord))){
					return true;
				}
			}
			return false;
		} else {			
			return true;
		}
	}
	
	private void readCoord2SurchargeFile(URL fileURL){
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
