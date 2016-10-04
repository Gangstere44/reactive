package template;

import logist.topology.Topology.City;


public class VehicleState {

	private static int counter = 0;

	private City curCity;
	private City destCity;
	private int id;

	public VehicleState(City curCity, City destCity) {
		this.curCity = curCity;
		this.destCity = destCity;
		this.id = counter++;
	}

	public VehicleState(City curCity) {
		this.curCity = curCity;
		this.destCity = null;
		this.id = counter++;
	}

	public City getCurrentCity() {
		return curCity;
	}

	public City getDestinationCity() {
		return destCity;
	}

	public int getId() {
		return id;
	}

}
