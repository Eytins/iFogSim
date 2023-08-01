package org.fog.aco2;

import isula.aco.Environment;
import isula.aco.exception.InvalidInputException;
import org.fog.entities.FogDevice;
import org.fog.placement.LocationHandler;
import org.fog.utils.FogDeviceUtils;

import java.util.List;
import java.util.Map;

public class ACOEnvironment extends Environment {

    private List<FogDevice> fogDevices;

    private Map<Integer, FogDevice> fogDevicesMap;

    private final int idOfStartNode;

    private final int indexOfStartNode;

    private final int idOfEndNode;

    private final int numOfServices;

    private final Map<Integer, Map<Integer, Double>> latencyMatrix;

    private final LocationHandler locator;

    public ACOEnvironment(double[][] problemRepresentation,
                          List<FogDevice> fogDevices,
                          int idOfStartNode, int indexOfStartNode, int idOfEndNode,
                          int numOfServices, LocationHandler locator) throws InvalidInputException {
        super(problemRepresentation);
        this.fogDevices = fogDevices;
        this.idOfStartNode = idOfStartNode;
        this.indexOfStartNode = indexOfStartNode;
        this.idOfEndNode = idOfEndNode;
        this.numOfServices = numOfServices;
        this.locator = locator;
        this.fogDevicesMap = FogDeviceUtils.fogDeviceListToMap(fogDevices);
        this.latencyMatrix = FogDeviceUtils.createLatencyMatrix(fogDevices);
        this.setPheromoneMatrix(this.createPheromoneMatrix());
    }

    public List<FogDevice> getFogDevices() {
        return fogDevices;
    }

    public Map<Integer, FogDevice> getFogDevicesMap() {
        return fogDevicesMap;
    }

    public int getIdOfStartNode() {
        return idOfStartNode;
    }

    public int getIdOfEndNode() {
        return idOfEndNode;
    }

    public int getIndexOfStartNode() {
        return indexOfStartNode;
    }

    public int getNumOfServices() {
        return numOfServices;
    }

    public LocationHandler getLocator() {
        return locator;
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
        if (fogDevices == null) {
            return null;
        }
        int maxID = FogDeviceUtils.getMaxFogDeviceID(fogDevices);
        if (this.getProblemRepresentation() != null) {
            // Based on observation, maxId is greater than the length of FogDevices.
            return new double[maxID + 1][maxID + 1];
        }

        return null;
    }
}


