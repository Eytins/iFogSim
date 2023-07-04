package org.fog.aco;

import isula.aco.Environment;
import isula.aco.exception.InvalidInputException;
import org.fog.entities.FogDevice;
import org.fog.utils.FogDeviceUtils;

import java.util.List;
import java.util.Map;

public class ACOEnvironment extends Environment {

    List<FogDevice> fogDevices;

    private final int idOfStartNode;

    private final int idOfEndNode;

    private final int numOfDevices;

    private final Map<Integer, Map<Integer, Double>> latencyMatrix;

    private static final int NUM_OF_FOG_DEVICES = 15;

    public ACOEnvironment(double[][] problemRepresentation,
                          List<FogDevice> fogDevices,
                          int idOfStartNode, int idOfEndNode,
                          int numOfDevices) throws InvalidInputException {
        super(problemRepresentation);
        this.fogDevices = fogDevices;
        this.idOfStartNode = idOfStartNode;
        this.idOfEndNode = idOfEndNode;
        this.numOfDevices = numOfDevices;
        this.latencyMatrix = FogDeviceUtils.createLatencyMatrix(fogDevices);
    }

    public int getNumOfFogDevices() {
        return fogDevices.size();
    }

    public int getIdOfStartNode() {
        return idOfStartNode;
    }

    public int getIdOfEndNode() {
        return idOfEndNode;
    }

    public int getNumOfServices() {
        return numOfDevices;
    }

    public Map<Integer, Map<Integer, Double>> getLatencyMatrix() {
        return latencyMatrix;
    }

    /**
     * The pheromone matrix in the TSP problem stores a pheromone value per node and per position of this node on
     * the route. That explains the dimensions selected for the pheromone matrix.
     *
     * @return Pheromone matrix instance.
     */
    @Override
    protected double[][] createPheromoneMatrix() {
        int maxId = 0;
        for (FogDevice device : fogDevices) {
            maxId = Math.max(maxId, device.getId());
        }

        if (this.getProblemRepresentation() != null) {
            // Based on observation, maxId is greater than the length of FogDevices.
            return new double[maxId + 1][maxId + 1];
        }

        return null;
    }
}


