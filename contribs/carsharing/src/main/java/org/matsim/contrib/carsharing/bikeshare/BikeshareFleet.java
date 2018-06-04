package org.matsim.contrib.carsharing.bikeshare;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.vehicles.Vehicle;

public class BikeshareFleet {
	private QuadTree<Id<Vehicle>> availableBikesLocationQuadTree;
	private Map<Id<Vehicle>, Coord> bikeCoordMap = new HashMap<>();
	private Map<Id<Vehicle>, Coord> initialBikeCoordMap;
	private Map<Id<Person>, Id<Vehicle>> rentedBikes = new HashMap<>();

	public BikeshareFleet(QuadTree<Id<Vehicle>> availableBikesLocationQuadTree,
			Map<Id<Vehicle>, Coord> vehicleCoordMap) {
		this.availableBikesLocationQuadTree = availableBikesLocationQuadTree;
		this.initialBikeCoordMap = vehicleCoordMap;
		for (Id<Vehicle> vehicleId : vehicleCoordMap.keySet()) {
			this.bikeCoordMap.put(vehicleId, vehicleCoordMap.get(vehicleId));
		}
	}

	public Id<Vehicle> getAndRemoveClosest(Coord coord, Id<Person> person) {

		Id<Vehicle> closestBike = this.availableBikesLocationQuadTree.getClosest(coord.getX(), coord.getY());
		if (closestBike != null) {
			Coord coordLoc = this.bikeCoordMap.get(closestBike);
			this.availableBikesLocationQuadTree.remove(coordLoc.getX(), coordLoc.getY(), closestBike);
			this.rentedBikes.put(person, closestBike);
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

	public Map<Id<Person>, Id<Vehicle>> getRentedBikes() {
		return rentedBikes;
	}

	public void reset() {

		this.rentedBikes = new HashMap<>();
		this.availableBikesLocationQuadTree.clear();
		for (Id<Vehicle> vehicleId : this.initialBikeCoordMap.keySet()) {
			Coord coord = this.initialBikeCoordMap.get(vehicleId);
			this.bikeCoordMap.put(vehicleId, coord);
			this.availableBikesLocationQuadTree.put(coord.getX(), coord.getY(), vehicleId);			
		}
	}

}
