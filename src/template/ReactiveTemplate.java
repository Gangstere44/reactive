package template;

import java.util.Random;

import logist.simulation.Vehicle;
import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

public class ReactiveTemplate implements ReactiveBehavior {

	private Random random;
	private double pPickup;
	private int numActions;
	private Agent myAgent;

	private int numCities;
	private VehicleAction[] actions;
	private VehicleState[] states;

	private long[][] rewards;
	private int[][] transitions;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
		this.pPickup = discount;
		this.numActions = 0;
		this.myAgent = agent;

		this.numCities = topology.size();
		states = new VehicleState[numCities * numCities];

		actions = new VehicleAction[numCities + 1];

		initStates(topology);
		initActions(topology);

		rewards = new long[states.length][actions.length];
		transitions = new int[states.length][actions.length];

		initRewards();
		initTransitions();



	}

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

		if (availableTask == null || random.nextDouble() > pPickup) {
			City currentCity = vehicle.getCurrentCity();
			action = new Move(currentCity.randomNeighbor(random));
		} else {
			action = new Pickup(availableTask);
		}

		if (numActions >= 1) {
			System.out.println("The total profit after " + numActions
					+ " actions is " + myAgent.getTotalProfit()
					+ " (average profit: "
					+ (myAgent.getTotalProfit() / (double) numActions) + ")");
		}
		numActions++;

		return action;
	}

	private void initStates(Topology topology) {

		for (int i = 0; i < numCities; i++) {

			for (int j = 0; j < numCities; j++) {

				if (i == j) {

					states[i * j + j] = new VehicleState(topology.cities().get(
							i));

				} else {

					states[i * j + j] = new VehicleState(topology.cities().get(
							i), topology.cities().get(j));
				}

			}
		}

	}

	private void initActions(Topology topology) {

		for (int i = 0; i < numCities; i++) {

			actions[i] = new VehicleAction(false, topology.cities().get(i));

		}

		actions[numCities] = new VehicleAction(true, null);
	}

	private void initRewards() {

		for (int i = 0; i < states.length; i++) {

			for (int j = 0; j < actions.length; j++) {

				rewards[i][j] = giveReward(states[i], actions[j]);

			}
		}
	}

	private long giveReward(VehicleState s, VehicleAction a) {

	}

	private void initTransitions() {

		for (int i = 0; i < states.length; i++) {

			for (int j = 0; j < actions.length; j++) {

				transitions[i][j] =

			}
		}

	}
}
