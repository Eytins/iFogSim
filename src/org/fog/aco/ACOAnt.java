package org.fog.aco;

import isula.aco.Ant;
import org.fog.entities.FogDevice;
import org.fog.utils.FogDeviceUtils;

import java.util.*;

/**
 * Node Routing Problem <p>
 * Some explanation: <p>
 * 1. A solution component refers to an element
 * or a part of a potential solution to a given problem.
 * <p>
 * 2. In ACO, the problem to be solved is typically represented
 * as a graph, where nodes represent possible solutions or decision
 * points, and edges represent the connections or transitions between
 * those solutions. Each solution component corresponds to a particular
 * decision or choice made by an ant as it moves through the graph.
 * <p>
 * 3. Problem Type:
 * Node Routing Problem" or "Node Constrained Shortest Path Problem
 */
public class ACOAnt extends Ant<FogDevice, ACOEnvironment> {
    private static final double DELTA = Float.MIN_VALUE;

    private int initialReference;

    private final int numberOfNodes;

    private final int indexOfFirstNode;

    public ACOAnt(int numberOfNodes, int indexOfFirstNode) {
        this.numberOfNodes = numberOfNodes;
        this.indexOfFirstNode = indexOfFirstNode;
        this.setSolution(new FogDevice[numberOfNodes]);
    }

    @Override
    public void clear() {
        super.clear();
        this.initialReference = indexOfFirstNode;
    }

    /**
     * Returns true when the solution built by the current ant is finished.
     * <p>
     * When the ant has passed greater than serviceNum of nodes and reached end point.
     * <p>
     * Define when the Ant must stop adding components to its solution.
     *
     * @param acoEnvironment Environment instance with problem information.
     * @return True if the solution is finished, false otherwise.
     */
    @Override
    public boolean isSolutionReady(ACOEnvironment acoEnvironment) {
        // Means this ant is invalid
        if (getCurrentIndex() == Integer.MIN_VALUE) {
            return true;
        }
        return (getCurrentIndex() >= acoEnvironment.getNumOfServices() - 1)
                && (getSolution()[getCurrentIndex() - 1].getId() == acoEnvironment.getIdOfEndNode());
    }

    /**
     * Calculates the cost associated to the solution build, which is needed
     * to determine the performance of the Ant.
     * <p>
     * Or: Calculates the total cost or distance associated with the path traversed
     * from the start point to the end point, while passing through the
     * required number of nodes or satisfying other constraints
     *
     * @param acoEnvironment Environment instance with problem information.
     * @return The cost of the solution built.
     */
    @Override
    public double getSolutionCost(ACOEnvironment acoEnvironment) {
        // Means this ant is invalid
        if (getCurrentIndex() == Integer.MIN_VALUE) {
            return Double.MAX_VALUE;
        }
        double totalCost = 0.0;
        for (int i = 1; i < getSolution().length; i++) {
            FogDevice preNode = getSolution()[i - 1];
            FogDevice curNode = getSolution()[i];
            // TODO: add cost with curNode's distance with user's route.
            double costBetweenTwoNodes = getCostBetweenTwoNodes(preNode, curNode, acoEnvironment);
            totalCost += costBetweenTwoNodes;
        }
        return totalCost;
    }

    /**
     * Calculates the heuristic contribution for the cost of the
     * solution by adding a component at a specific position.
     * <p>
     * calculates an estimate of the potential benefit or cost
     * associated with adding a specific component to the current solution
     * <p>
     * When the solution is empty we take a random node as a reference.
     *
     * @param fogDevice          Solution component
     * @param positionInSolution Position of this component in the solution.
     * @param acoEnvironment     Environment instance with problem information.
     * @return Heuristic contribution
     */
    @Override
    public Double getHeuristicValue(FogDevice fogDevice, Integer positionInSolution, ACOEnvironment acoEnvironment) {
        FogDevice lastComponent;
        if (getCurrentIndex() > 0) {
            lastComponent = this.getSolution()[getCurrentIndex() - 1];
        } else {
            lastComponent = acoEnvironment.getFogDevices().get(this.initialReference);
        }

//        double cost = getCostBetweenTwoNodes(lastComponent, fogDevice, acoEnvironment) + DELTA;
        double cost = FogDeviceUtils.calculateDistanceBetweenDevices(lastComponent, fogDevice, acoEnvironment.getLocator());
        return 1 / cost;
    }

    /**
     * The components that are available for selection while an Ant is constructing its solution.
     *
     * @param acoEnvironment Environment instance with problem information.
     * @return List of available components.
     */
    @Override
    public List<FogDevice> getNeighbourhood(ACOEnvironment acoEnvironment) {
        Map<Integer, Map<Integer, Double>> latencyMatrix = acoEnvironment.getLatencyMatrix();
        Set<Integer> ids;
        if (getCurrentIndex() == 0) {
            ids = latencyMatrix.get(acoEnvironment.getIdOfStartNode()).keySet();
        } else {
            ids = latencyMatrix.get(getSolution()[getCurrentIndex() - 1].getId()).keySet();
        }
        List<FogDevice> res = new ArrayList<>();
        for (FogDevice device : acoEnvironment.getFogDevices()) {
            if (ids.contains(device.getId())) {
                res.add(device);
            }
        }
        return res;
    }

    /**
     * Returns the pheromone value associated to a solution component at a specific position
     *
     * @param fogDevice          Solution component.
     * @param positionInSolution Position of this component in the solution.
     * @param acoEnvironment     Environment instance with problem information.
     * @return Pheromone value.
     */
    @Override
    public Double getPheromoneTrailValue(FogDevice fogDevice, Integer positionInSolution, ACOEnvironment acoEnvironment) {
        if (fogDevice == null) {
            return 0.0;
        }
        FogDevice previousComponent;
        if (positionInSolution > 0) {
            previousComponent = getSolution()[positionInSolution - 1];
        } else {
            previousComponent = acoEnvironment.getFogDevices().get(this.initialReference);
        }
        double[][] pheromoneMatrix = acoEnvironment.getPheromoneMatrix();
        return pheromoneMatrix[fogDevice.getId()][previousComponent.getId()];
    }

    /**
     * Updates the value of a cell on the pheromone matrix.
     *
     * @param fogDevice          Solution component.
     * @param positionInSolution Position of this component in the solution.
     * @param acoEnvironment     Environment instance with problem information.
     * @param value              New pheromone value.
     */
    @Override
    public void setPheromoneTrailValue(FogDevice fogDevice, Integer positionInSolution, ACOEnvironment acoEnvironment, Double value) {
        if (fogDevice == null) {
            return;
        }
        FogDevice previousComponent;
        if (positionInSolution > 0) {
            previousComponent = getSolution()[positionInSolution - 1];
        } else {
            previousComponent = acoEnvironment.getFogDevices().get(this.initialReference);
        }

        double[][] pheromoneMatrix = acoEnvironment.getPheromoneMatrix();
        pheromoneMatrix[fogDevice.getId()][previousComponent.getId()] = value;
        pheromoneMatrix[previousComponent.getId()][fogDevice.getId()] = value;
    }

    /**
     * Get the latency cost between two connecting nodes
     *
     * @param start       Send node
     * @param end         Receive node
     * @param environment Environment
     * @return The cost, Double.MAX_VALUE if two nodes are not connected directly.
     */
    private static double getCostBetweenTwoNodes(FogDevice start, FogDevice end, ACOEnvironment environment) {
        if (start == null || end == null) {
            return Double.MAX_VALUE;
        }
        Map<Integer, Double> map = environment.getLatencyMatrix().get(start.getId());
        return map.getOrDefault(end.getId(), Double.MAX_VALUE);
    }
}
