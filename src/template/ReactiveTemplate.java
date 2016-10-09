package template;

import java.util.HashMap;
import java.util.Random;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private static final double EPSILON = 0.000001;

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;
	private Double discount;

	private int numCities;
	private int costPerKm;
	private TaskDistribution td;
	private Topology topology;
	private VehicleAction[] actions;
	private VehicleState[] states;

	// private double[][] rewards;
	HashMap<Pair<Integer, Integer>, Double> rewards;
	private double[][][] transitions;

	private VehicleAction[] bestActions;
	private double[] V;
	HashMap<Pair<Integer, Integer>, Double> Q;

	// private double[][] Q;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		this.discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;

		this.costPerKm = agent.vehicles().get(0).costPerKm();

		this.numCities = topology.size();
		this.td = td;
		this.topology = topology;

		states = new VehicleState[numCities * numCities];
		actions = new VehicleAction[2 * numCities];

		initStates();
		initActions();

		// rewards = new double[states.length][actions.length];
		rewards = new HashMap<Pair<Integer, Integer>, Double>();
		transitions = new double[states.length][actions.length][states.length];

		bestActions = new VehicleAction[states.length];
		V = new double[states.length];
		// Q = new double[states.length][actions.length];
		Q = new HashMap<Pair<Integer, Integer>, Double>();

		initRewards();
		initTransitions();

		System.out.println("Will start value iterate.");

		valueIterate();

		System.out.println("Value iterate done.");

		// printTaskDistribution();

		// printQValues();

	}

	private void printQValues() {
		for (int i = 0; i < states.length; i++) {
			for (int j = 0; j < actions.length; j++) {
				System.out.println("Q for when in "
						+ states[i].getCurrentCity() + " with task for "
						+ states[i].getTaskDestinationCity()
						+ ", taking action: take = " + actions[j].getTake()
						+ " or moving to " + actions[j].getDestinationCity()
						+ " : reward = "
						+ Q.get(new Pair<Integer, Integer>(i, j)));
			}
		}

	}

	private void printTaskDistribution() {
		for (City city1 : topology.cities()) {
			for (City city2 : topology.cities()) {
				System.out.println("From " + city1.name + " to " + city2.name
						+ " : ");
				System.out.println("Probability of task: "
						+ td.probability(city1, city2));
				System.out.println("Expected reward: "
						+ td.reward(city1, city2));
				System.out.println("----------------------");
			}
		}

	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {

		Action action;

		VehicleAction vAction = getActionForState(vehicle.getCurrentCity(),
				availableTask == null ? null : availableTask.deliveryCity);

		// Random for exploration (needed?)
		if (availableTask == null || !vAction.getTake()) {
			if (availableTask != null) {
				System.out.println("[MISS " + vehicle.name()
						+ "] with reward = " + availableTask.reward);
			}

			// TODO: Need to check arg here
			if (vAction.getDestinationCity() == null
					|| !vehicle.getCurrentCity().hasNeighbor(
							vAction.getDestinationCity())) {
				System.out.println("Destination city was null!!!!!!!");
				action = new Move(vehicle.getCurrentCity().randomNeighbor(
						random));
			}

			else {
				action = new Move(vAction.getDestinationCity());
			}
		} else {
			action = new Pickup(availableTask);
		}

		if (numActions >= 1) {
			System.out.println("[" + vehicle.name()
					+ "]The total profit after " + numActions + " actions is "
					+ myAgent.getTotalProfit() + " (average profit: "
					+ (myAgent.getTotalProfit() / (double) numActions) + ")");
		}
		numActions++;

		return action;
	}

	private VehicleAction getActionForState(City curCity, City destCity) {
		for (int i = 0; i < states.length; i++) {
			if (destCity == null && states[i].getTaskDestinationCity() == null
					&& states[i].getCurrentCity().id == curCity.id) {
				return bestActions[i];
			}
		}

		for (int i = 0; i < states.length; i++) {
			if (states[i].getCurrentCity().id == curCity.id
					&& states[i].getTaskDestinationCity() != null
					&& states[i].getTaskDestinationCity().id == destCity.id) {
				return bestActions[i];
			}
		}
		return null;
	}

	private void valueIterate() {
		double totalDiff = Double.MAX_VALUE;
		int iter = 1;
		while (totalDiff > EPSILON) {
			System.out.println("Iteration no: " + iter + " with totalDiff = "
					+ totalDiff);
			totalDiff = 0;
			for (int i = 0; i < states.length; i++) {
				for (int j = 0; j < actions.length; j++) {
					Pair<Integer, Integer> inter = new Pair<Integer, Integer>(
							i, j);
					if (rewards.containsKey(inter)) {
						Q.put(inter, rewards.get(inter) + discount
								* sumTransitions(i, j));
					}
				}
				double maxQValue = (Double) maxQValue(i).getLeft();
				VehicleAction maxQAction = (VehicleAction) maxQValue(i).getRight();

				totalDiff += Math.abs(maxQValue - V[i]);

				V[i] = maxQValue;
				bestActions[i] = maxQAction;
			}
			iter++;
		}
	}

	private Pair<Double, VehicleAction> maxQValue(int state) {
		double max = 0d;
		boolean first = true;
		VehicleAction best = null;
		for (int i = 0; i < actions.length; i++) {
			Pair<Integer, Integer> inter = new Pair<Integer, Integer>(state, i);
			if (Q.containsKey(inter)) {
				if (first || Q.get(inter) > max) {
					max = Q.get(inter);
					best = actions[i];
					first = false;
				}
			}
		}
		return new Pair<Double, VehicleAction>(max, best);
	}

	private double sumTransitions(int state, int action) {
		double total = 0d;
		for (int i = 0; i < states.length; i++) {
			total += transitions[state][action][i] * V[i];
		}
		return total;
	}

	private void initStates() {
		for (int i = 0; i < numCities; i++) {
			for (int j = 0; j < numCities; j++) {
				if (i == j) {
					states[i * numCities + j] = new VehicleState(topology
							.cities().get(i));
				} else {
					states[i * numCities + j] = new VehicleState(topology
							.cities().get(i), topology.cities().get(j));
				}
			}
		}
	}

	private void initActions() {
		for (int i = 0; i < numCities; i++) {
			actions[i] = new VehicleAction(false, topology.cities().get(i));
			actions[i + numCities] = new VehicleAction(true, null);
		}
	}

	private void initRewards() {
		for (int i = 0; i < states.length; i++) {
			for (int j = 0; j < actions.length; j++) {
				giveReward(states[i], i, actions[j], j);
			}
		}
	}

	// TODO: Should we also use something with the distance????
	private void giveReward(VehicleState s, int posS, VehicleAction a, int posA) {
		if (a.getTake() && s.getTaskDestinationCity() != null
				&& s.getCurrentCity().id != s.getTaskDestinationCity().id) {
			/*
			rewards.put(
					new Pair<Integer, Integer>(posS, posA),
					td.reward(s.getCurrentCity(), s.getTaskDestinationCity())
							- s.getCurrentCity().distanceTo(
									s.getTaskDestinationCity())
							* costPerKm
							+ computeAveragePossibleRewardWithDistance(s
									.getTaskDestinationCity()));
			*/
			rewards.put(
					new Pair<Integer, Integer>(posS, posA),
					td.reward(s.getCurrentCity(), s.getTaskDestinationCity())
							- s.getCurrentCity().distanceTo(
									s.getTaskDestinationCity())
							* costPerKm);

		}

		else {
			if (s.getCurrentCity().hasNeighbor(a.getDestinationCity())) {
				double distance;
				if (s.getTaskDestinationCity() == null) {
					distance = 0;
				} else {
					distance = s.getCurrentCity().distanceTo(
							s.getTaskDestinationCity());
				}
				/*
				rewards.put(
						new Pair<Integer, Integer>(posS, posA),
						computeAveragePossibleRewardWithDistance(a
								.getDestinationCity()) - distance * costPerKm);
				*/
				rewards.put(
						new Pair<Integer, Integer>(posS, posA),
						-distance * costPerKm);

			}
		}
	}

	/*
	 * private double computeAveragePossibleReward(City city) { double
	 * totalPossibleReward = 0; for (int i = 0; i < numCities; i++) { double
	 * tmpReward = td.reward(city, topology.cities().get(i)) *
	 * td.probability(city, topology.cities().get(i)); totalPossibleReward +=
	 * tmpReward; }
	 *
	 * return totalPossibleReward; }
	 */
	private double computeAveragePossibleRewardWithDistance(City city) {
		double totalPossibleReward = 0;
		for (int i = 0; i < numCities; i++) {
			double tmpReward = (td.reward(city, topology.cities().get(i)) - city
					.distanceTo(topology.cities().get(i)) * costPerKm)
					* td.probability(city, topology.cities().get(i));
			totalPossibleReward += tmpReward;
		}

		return totalPossibleReward;
	}

	private void initTransitions() {
		for (int i = 0; i < states.length; i++) {
			for (int j = 0; j < actions.length; j++) {
				for (int k = 0; k < states.length; k++) {
					transitions[i][j][k] = giveProbTransition(states[i],
							actions[j], states[k]);
				}
			}
		}
	}

	private double giveProbTransition(VehicleState s, VehicleAction a,
			VehicleState nextState) {
		if (a.getTake()) {
			// TODO: Check first condition of 'if' (should be impossible to
			// happen)
			if (s.getTaskDestinationCity() != null
					&& s.getTaskDestinationCity().id == nextState
							.getCurrentCity().id) {
				// Just have to check the proba of the task for this specific
				// location to spawn
				return td.probability(nextState.getCurrentCity(),
						nextState.getTaskDestinationCity());
			} else
				return 0d;
		} else {
			if (a.getDestinationCity().id == nextState.getCurrentCity().id) {
				// Just have to check the proba of the task for this specific
				// location to spawn
				return td.probability(nextState.getCurrentCity(),
						nextState.getTaskDestinationCity());
			} else
				return 0d;
		}
	}
}
