package org.fog.aco3;

import isula.aco.Environment;
import isula.aco.exception.InvalidInputException;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.LocationHandler;
import org.fog.utils.FogDeviceUtils;

import java.util.List;

public class FinalACOEnvironment extends Environment {
    private List<FogDevice> fogDevices;
    private List<Sensor> sensors;
    private List<Actuator> actuators;
    private Application application;
    private LocationHandler locator;
    private final double[][] latencyMatrix;
    private FogDevice initialDevice;

    /**
     * Creates an Environment for the Ants to traverse.
     *
     * @param problemRepresentation Graph representation of the problem to be solved.
     * @param fogDevices fogDevices
     * @param sensors sensors
     * @param actuators actuators
     * @param application application
     * @param locator locator
     * @throws InvalidInputException When the problem graph is incorrectly formed.
     */
    public FinalACOEnvironment(double[][] problemRepresentation,
                               List<FogDevice> fogDevices,
                               List<Sensor> sensors,
                               List<Actuator> actuators,
                               Application application,
                               LocationHandler locator) throws InvalidInputException {
        super(problemRepresentation);
        this.fogDevices = fogDevices;
        this.sensors = sensors;
        this.actuators = actuators;
        this.application = application;
        this.locator = locator;
        latencyMatrix = FogDeviceUtils.createLatencyMatrixOfAllDevices(fogDevices);
        initialDevice = FogDeviceUtils.getInitialFogDevice(fogDevices, application, locator);
        setPheromoneMatrix(createPheromoneMatrix());
    }

    @Override
    protected double[][] createPheromoneMatrix() {
        if (fogDevices == null) {
            return null;
        }
        if (this.getProblemRepresentation() != null) {
            // Based on observation, maxId is greater than the length of FogDevices.
            return FogDeviceUtils.copy2DArray(latencyMatrix);
        }
        return null;
    }

    public FogDevice getInitialDevice() {
        return initialDevice;
    }

    public List<FogDevice> getFogDevices() {
        return fogDevices;
    }

    public List<Sensor> getSensors() {
        return sensors;
    }

    public List<Actuator> getActuators() {
        return actuators;
    }

    public Application getApplication() {
        return application;
    }

    public LocationHandler getLocator() {
        return locator;
    }

    public double[][] getLatencyMatrix() {
        return latencyMatrix;
    }
}
