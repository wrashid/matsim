package org.matsim.contrib.carsharing.bikeshare;

import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.vehicles.Vehicle;

public class BikeshareFleet {
	private QuadTree<Id<Vehicle>> availableBikesLocationQuadTree;
	private Map<Id<Vehicle>, Coord> bikeCoordMap;

	public BikeshareFleet(QuadTree<Id<Vehicle>> availableBikesLocationQuadTree,
			Map<Id<Vehicle>, Coord> vehicleCoordMap) {
		this.availableBikesLocationQuadTree = availableBikesLocationQuadTree;
		this.bikeCoordMap = vehicleCoordMap;
	}

	public Id<Vehicle> getAndRemoveClosest(Coord coord) {

		Id<Vehicle> closestBike = this.availableBikesLocationQuadTree.getClosest(coord.getX(), coord.getY());
		if (closestBike != null) {
			Coord coordLoc = this.bikeCoordMap.get(closestBike);
			this.availableBikesLocationQuadTree.remove(coordLoc.getX(), coordLoc.getY(), closestBike);
		}
		
		return closestBike;
		
	}
	
	public void addBike(Coord coord, Id<Vehicle> bikeId) {
		
		this.availableBikesLocationQuadTree.put(coord.getX(), coord.getY(), bikeId);
		this.bikeCoordMap.put(bikeId, coord);
	}

	public Map<Id<Vehicle>, Coord> getBikeCoordMap() {
		return bikeCoordMap;
	}

}
