package template;

import logist.topology.Topology.City;


public class VehicleState {

	private static int counter = 0;

	private City curCity;
	private City taskDestCity;
	private int id;

	public VehicleState(City curCity, City taskDestCity) {
		this.curCity = curCity;
		this.taskDestCity = taskDestCity;
		this.id = counter++;
	}

	public VehicleState(City curCity) {
		this.curCity = curCity;
		this.taskDestCity = null;
		this.id = counter++;
	}

	public City getCurrentCity() {
		return curCity;
	}

	public City getTaskDestinationCity() {
		return taskDestCity;
	}

	public int getId() {
		return id;
	}

}
