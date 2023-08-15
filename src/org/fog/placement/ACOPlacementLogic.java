package org.fog.placement;

import isula.aco.*;
import isula.aco.algorithms.antsystem.OfflinePheromoneUpdate;
import isula.aco.algorithms.antsystem.PerformEvaporation;
import isula.aco.algorithms.antsystem.StartPheromoneMatrix;
import isula.aco.exception.InvalidInputException;
import org.fog.aco3.*;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.FogDeviceUtils;

import javax.naming.ConfigurationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ACOPlacementLogic extends ModulePlacement {
    private static Logger logger = Logger.getLogger(AntColony.class.getName());

    private List<Sensor> sensors;

    private List<Actuator> actuators;

    private ModuleMapping moduleMapping;

    private double[][] latencyMatrix;

    private LocationHandler locator;

    private Map<Integer, Map<String, Integer>> currentModuleInstanceNum = new HashMap<>();

    public ACOPlacementLogic(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators,
                             Application application, ModuleMapping moduleMapping, LocationHandler locator) throws InvalidInputException, ConfigurationException {
        setFogDevices(fogDevices);
        setApplication(application);
        setDeviceToModuleMap(new HashMap<>());
        this.moduleMapping = moduleMapping;
        this.locator = locator;
        this.sensors = sensors;
        this.actuators = actuators;
        mapModules();
    }

    @Override
    protected void mapModules() throws ConfigurationException, InvalidInputException {
        double[][] symbolicProblemRepresentation = new double[1][1];
        // devices.remove(0);
        List<FogDevice> devices = new ArrayList<>(getFogDevices());
        FogDevice mobile = devices.remove(0);

        FinalACOEnvironment environment = new FinalACOEnvironment(symbolicProblemRepresentation,
                devices,
                sensors,
                actuators,
                getApplication(),
                locator);
        FinalACOProblemConfiguration configuration = new FinalACOProblemConfiguration(environment);
        AntColony<FogDevice, FinalACOEnvironment> colony = getAntColony(configuration);
        FinalACOProblemSolver<FogDevice, FinalACOEnvironment> solver = new FinalACOProblemSolver<>();
        solver.initialize(environment, colony, configuration);
        solver.addDaemonActions(new FinalACOStartPheromoneMatrix<>(), new PerformEvaporation<>());
        solver.addDaemonActions(getPheromoneUpdatePolicy());
        solver.getAntColony().addAntPolicies(new FinalACONodeSelection());
        long startTime = System.currentTimeMillis();
        solver.solveProblem();
        long endTime = System.currentTimeMillis();
        System.out.println("Time consumed by ACO in milliseconds:" + (endTime - startTime));
        FogDevice[] bestSolution = solver.getBestSolution();
        // Algorithm done, start placing
        startPlacement(mobile, bestSolution);
    }

    private void startPlacement(FogDevice mobile, FogDevice[] bestSolution) {
        setModuleInstanceCountMap(getCurrentModuleInstanceNum(mobile, bestSolution, getApplication().getModuleNames()));
        setModulesOnDevice(mappedModules(bestSolution, mobile));
        Map<Integer, List<AppModule>> deviceToModuleMap = new HashMap<>();
        List<AppModule> temp = new ArrayList<>();
        temp.add(getApplication().getModules().get(0));
        deviceToModuleMap.put(mobile.getId(), temp);
        for (int i = 1; i < getApplication().getModules().size(); i++) {
            temp = new ArrayList<>();
            temp.add(getApplication().getModules().get(i));
            deviceToModuleMap.put(bestSolution[i - 1].getId(), temp);
        }
        setDeviceToModuleMap(deviceToModuleMap);
        Map<String, List<Integer>> moduleToDeviceMap = getModuleToDeviceMap(mobile, bestSolution);
        setModuleToDeviceMap(moduleToDeviceMap);
        createModuleInstanceOnDevice(getApplication().getModules().get(0), mobile);
        for (int i = 1; i < getApplication().getModuleNames().size(); i++) {
            createModuleInstanceOnDevice(getApplication().getModules().get(i), bestSolution[i - 1]);
        }
    }

    private Map<String, List<Integer>> getModuleToDeviceMap(FogDevice mobile, FogDevice[] bestSolution) {
        Map<String, List<Integer>> moduleToDeviceMap = new HashMap<>();
        List<Integer> tempDeviceList = new ArrayList<>();
        tempDeviceList.add(mobile.getId());
        moduleToDeviceMap.put(getApplication().getModuleNames().get(0), tempDeviceList);
        for (int i = 1; i < getApplication().getModuleNames().size(); i++) {
            tempDeviceList = new ArrayList<>();
            tempDeviceList.add(bestSolution[i - 1].getId());
            moduleToDeviceMap.put(getApplication().getModuleNames().get(i), tempDeviceList);
        }
        return moduleToDeviceMap;
    }

    private AntColony<FogDevice, FinalACOEnvironment> getAntColony(ConfigurationProvider configurationProvider) {
        return new AntColony<FogDevice, FinalACOEnvironment>(configurationProvider.getNumberOfAnts()) {
            @Override
            protected Ant<FogDevice, FinalACOEnvironment> createAnt(FinalACOEnvironment environment) {
                return new FinalACOAnt(environment.getFogDevices().size() - 1, getApplication().getModuleNames());
            }
        };
    }

    private DaemonAction<FogDevice, FinalACOEnvironment> getPheromoneUpdatePolicy() {
        return new OfflinePheromoneUpdate<FogDevice, FinalACOEnvironment>() {
            @Override
            protected double getPheromoneDeposit(Ant<FogDevice, FinalACOEnvironment> ant,
                                                 Integer positionInSolution,
                                                 FogDevice solutionComponent,
                                                 FinalACOEnvironment environment,
                                                 ConfigurationProvider configurationProvider) {
                return 1 / ant.getSolutionCost(environment);
            }
        };
    }

    public Map<Integer, Map<String, Integer>> getCurrentModuleInstanceNum(FogDevice client,
                                                                          FogDevice[] solution,
                                                                          List<String> modules) {
        Map<Integer, Map<String, Integer>> result = new HashMap<>();
        Map<String, Integer> temp = new HashMap<>();
        temp.put(modules.get(0), 1);
        result.put(client.getId(), temp);
        for (int i = 0; i < getApplication().getModuleNames().size() - 1; i++) {
            temp = new HashMap<>();
            temp.put(modules.get(i + 1), 1);
            result.put(solution[i].getId(), temp);
        }
        return result;
    }

    private Map<Integer, List<String>> mappedModules(FogDevice[] solution, FogDevice client) {
        Map<Integer, List<String>> currentModuleMap = new HashMap<>();
        ArrayList<String> module = new ArrayList<>();
        List<String> moduleNames = getApplication().getModuleNames();
        module.add(moduleNames.get(0));
        currentModuleMap.put(client.getId(), module);
        for (int i = 0; i < getApplication().getModuleNames().size() - 1; i++) {
            module = new ArrayList<>();
            module.add(moduleNames.get(i + 1));
            currentModuleMap.put(solution[i].getId(), module);
        }
        return currentModuleMap;
    }

}
