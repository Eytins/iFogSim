package org.fog.aco3;

import isula.aco.Ant;
import org.fog.application.AppModule;
import org.fog.entities.FogDevice;
import org.fog.mobilitydata.Location;
import org.fog.placement.LocationHandler;
import org.fog.utils.Config;
import org.fog.utils.FogDeviceUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinalACOAnt extends Ant<FogDevice, FinalACOEnvironment> {
    private final int numberOfDevices;
    private List<String> modulesPlaced = new ArrayList<>();
    private List<String> modulesToBePlaced;
    private final List<String> modulesToBePlacedOrg;
    private Map<Integer, Double> currentCpuLoadMap = new HashMap<>();
    private Map<Integer, String> modulesOnDevice = new HashMap<>();

    public FinalACOAnt(int numberOfNodes, List<String> modulesToBePlaced) {
        this.numberOfDevices = numberOfNodes;
        this.setSolution(new FogDevice[numberOfNodes]);
        this.modulesToBePlaced = modulesToBePlaced;
        this.modulesToBePlaced.remove(0);
        modulesToBePlacedOrg = new ArrayList<>(modulesToBePlaced);
    }

    public Map<Integer, String> getModulesOnDevice() {
        return modulesOnDevice;
    }

    public int getNumberOfDevices() {
        return numberOfDevices;
    }

    public List<String> getModulesPlaced() {
        return modulesPlaced;
    }

    public List<String> getModulesToBePlaced() {
        return modulesToBePlaced;
    }

    public Map<Integer, Double> getCurrentCpuLoadMap() {
        return currentCpuLoadMap;
    }

    @Override
    public void clear() {
        super.clear();
        modulesToBePlaced = new ArrayList<>(modulesToBePlacedOrg);
        modulesPlaced = new ArrayList<>();
        modulesOnDevice = new HashMap<>();
    }

    @Override
    public void visitNode(FogDevice visitedNode) {
        super.visitNode(visitedNode);
        String placedModule = modulesToBePlaced.remove(0);
        modulesPlaced.add(placedModule);
        modulesOnDevice.put(visitedNode.getId(), placedModule);
    }

    @Override
    public boolean isSolutionReady(FinalACOEnvironment environment) {
        return getModulesToBePlaced().isEmpty();
    }

    @Override
    public double getSolutionCost(FinalACOEnvironment environment) {
        double totalCost = 0.0;
        for (int i = 1; i < getCurrentIndex(); i++) {
            FogDevice preNode = getSolution()[i - 1];
            FogDevice curNode = getSolution()[i];
            totalCost += environment.getLatencyMatrix()[preNode.getId()][curNode.getId()];
            totalCost += getSolution()[i].getPower();
        }
        return totalCost;
    }

    @Override
    public Double getHeuristicValue(FogDevice solutionComponent, Integer positionInSolution, FinalACOEnvironment environment) {
        return 1 / getDistanceBetweenCurrentDeviceAndUser(solutionComponent, environment);
    }

    @Override
    public List<FogDevice> getNeighbourhood(FinalACOEnvironment environment) {
        return environment.getModulesAvailableDevices().get(modulesToBePlaced.get(0));
    }

    @Override
    public Double getPheromoneTrailValue(FogDevice solutionComponent, Integer positionInSolution, FinalACOEnvironment environment) {
        if (solutionComponent == null) {
            return 0.0;
        }
        FogDevice previousComponent;
        if (positionInSolution > 0) {
            previousComponent = getSolution()[positionInSolution - 1];
        } else {
            previousComponent = environment.getInitialDevice();
        }
        double[][] pheromoneMatrix = environment.getPheromoneMatrix();
        return pheromoneMatrix[previousComponent.getId()][solutionComponent.getId()];
    }

    @Override
    public void setPheromoneTrailValue(FogDevice solutionComponent, Integer positionInSolution, FinalACOEnvironment environment, Double value) {
        if (solutionComponent == null) {
            return;
        }
        FogDevice previousComponent;
        if (positionInSolution > 0) {
            previousComponent = getSolution()[positionInSolution - 1];
        } else {
            previousComponent = environment.getInitialDevice();
        }
        double[][] pheromoneMatrix = environment.getPheromoneMatrix();
        pheromoneMatrix[previousComponent.getId()][solutionComponent.getId()] = value;
    }

    public double getDistanceBetweenCurrentDeviceAndUser(FogDevice solutionComponent, FinalACOEnvironment environment) {
        LocationHandler locator = environment.getLocator();
        double devicePositionX = locator.dataObject.resourceLocationData.get(locator.instanceToDataId.get(solutionComponent.getId())).latitude;
        double devicePositionY = locator.dataObject.resourceLocationData.get(locator.instanceToDataId.get(solutionComponent.getId())).longitude;
        double timeOfUser = getClosetTimeOfUser(locator, getCurrentTime(environment));
        Location userLocation = locator.dataObject.usersLocation.get(Config.USER_NAME).get(timeOfUser);
        return FogDeviceUtils.calculateDistance(devicePositionX, devicePositionY, userLocation.latitude, userLocation.longitude);
    }

    public double getCurrentTime(FinalACOEnvironment environment) {
        double result = 0.0;
        // Add module running time
        for (int i = 0; i < modulesPlaced.size(); i++) {
            AppModule module = environment.getApplication().getModuleByName(modulesPlaced.get(i));
            // TODO: Adjustment needed
            result += module.getRam() / ((double) getSolution()[i].getHost().getBw() / (2 * 8000));
        }
        // Add Delay Between Devices
        if (getCurrentIndex() == 1) {
            return result;
        } else {
            for (int i = 1; i < getCurrentIndex(); i++) {
                result += environment.getLatencyMatrix()[getSolution()[i - 1].getId()][getSolution()[i].getId()];
            }
        }
        return result;
    }

    public double getClosetTimeOfUser(LocationHandler locator, double time) {
        Map<Double, Location> map = locator.dataObject.usersLocation.get(Config.USER_NAME);
        double dif = Double.MAX_VALUE;
        double result = 0;
        for (Double key : map.keySet()) {
            if (Math.abs(key - time) < dif) {
                dif = Math.abs(key - time);
                result = key;
            }
        }
        return result;
    }
}
