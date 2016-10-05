package template;

import logist.topology.Topology.City;

public class VehicleAction {

	private static int counter = 0;

	private boolean take; // if true take package for destCity
						  // else move to destCity
	private City destCity;
	private int id;

	public VehicleAction(boolean take, City destCity) {
		this.take = take;
		this.destCity = destCity;
		this.id = counter++;
	}

	public boolean getTake() {
		return take;
	}

	public City getDestinationCity() {
		return destCity;
	}

	public int getId() {
		return id;
	}
}
