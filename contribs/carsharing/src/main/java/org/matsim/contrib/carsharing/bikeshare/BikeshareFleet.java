package org.matsim.contrib.carsharing.bikeshare;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.vehicles.Vehicle;

public class BikeshareFleet {
	private QuadTree<Id<Vehicle>> availableBikesLocationQuadTree;
	private Map<Id<Vehicle>, Coord> bikeCoordMap = new ConcurrentHashMap<>();
	private Map<Id<Vehicle>, Coord> initialBikeCoordMap;
	private Map<Id<Person>, Id<Vehicle>> rentedBikes = new ConcurrentHashMap<>();
	private ArrayList<QuadTree<Double>> rentals;

	public BikeshareFleet(QuadTree<Id<Vehicle>> availableBikesLocationQuadTree,
			Map<Id<Vehicle>, Coord> vehicleCoordMap) {
		this.availableBikesLocationQuadTree = availableBikesLocationQuadTree;
		rentals = new ArrayList<>();
				//QuadTree<Double>(availableBikesLocationQuadTree.getMinEasting(),
				//availableBikesLocationQuadTree.getMinNorthing(), availableBikesLocationQuadTree.getMaxEasting(),
				//a/vailableBikesLocationQuadTree.getMaxNorthing());
		this.initialBikeCoordMap = vehicleCoordMap;
		for (Id<Vehicle> vehicleId : vehicleCoordMap.keySet()) {
			this.bikeCoordMap.put(vehicleId, vehicleCoordMap.get(vehicleId));
		}
	}

	public Id<Vehicle> getAndRemoveClosest(Coord coord, Id<Person> person) {
		synchronized (this.availableBikesLocationQuadTree) {
			Id<Vehicle> closestBike = this.availableBikesLocationQuadTree.getClosest(coord.getX(), coord.getY());
			if (closestBike != null) {
				Coord coordLoc = this.bikeCoordMap.get(closestBike);
				this.availableBikesLocationQuadTree.remove(coordLoc.getX(), coordLoc.getY(), closestBike);
				this.rentedBikes.put(person, closestBike);
			}
			return closestBike;
		}

	}

	public void addBike(Coord coord, Id<Vehicle> bikeId) {
		synchronized (this.availableBikesLocationQuadTree) {

			this.availableBikesLocationQuadTree.put(coord.getX(), coord.getY(), bikeId);
			this.bikeCoordMap.put(bikeId, coord);
		}
	}

	public Map<Id<Vehicle>, Coord> getBikeCoordMap() {
		return bikeCoordMap;
	}

	public Map<Id<Person>, Id<Vehicle>> getRentedBikes() {
		return rentedBikes;
	}

	public QuadTree<Id<Vehicle>> getAvailableBikesLocationQuadTree() {
		return availableBikesLocationQuadTree;
	}

	public void reset() {

		this.rentedBikes.clear();
		this.availableBikesLocationQuadTree.clear();
		for (Id<Vehicle> vehicleId : this.initialBikeCoordMap.keySet()) {
			Coord coord = this.initialBikeCoordMap.get(vehicleId);
			this.bikeCoordMap.put(vehicleId, coord);
			this.availableBikesLocationQuadTree.put(coord.getX(), coord.getY(), vehicleId);
		}
		this.rentals.clear();
	}

	public void addRental(Coord coord, double now) {
		this.rentals.get(this.rentals.size() - 1).put(coord.getX(), coord.getY(), now);
	}

	public ArrayList<QuadTree<Double>> getRentals() {
		return rentals;
	}
	
	public void addRentalQuadTree() {
		this.rentals.add(new QuadTree<Double> (availableBikesLocationQuadTree.getMinEasting(),
				availableBikesLocationQuadTree.getMinNorthing(), availableBikesLocationQuadTree.getMaxEasting(),
				availableBikesLocationQuadTree.getMaxNorthing()));
	}

}
