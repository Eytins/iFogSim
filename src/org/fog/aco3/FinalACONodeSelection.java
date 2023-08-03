package org.fog.aco3;

import isula.aco.AntPolicy;
import isula.aco.AntPolicyType;
import isula.aco.ConfigurationProvider;
import isula.aco.exception.ConfigurationException;
import isula.aco.exception.SolutionConstructionException;
import org.fog.entities.FogDevice;
import org.fog.utils.Config;
import org.fog.utils.FogDeviceUtils;

import java.util.*;

public class FinalACONodeSelection extends AntPolicy<FogDevice, FinalACOEnvironment> {
    public FinalACONodeSelection() {
        super(AntPolicyType.NODE_SELECTION);
    }

    @Override
    public boolean applyPolicy(FinalACOEnvironment environment, ConfigurationProvider configurationProvider) {
        if (getAnt().getCurrentIndex() == 0) {
            getAnt().visitNode(environment.getInitialDevice());
            return true;
        }
        Random random = new Random();
        HashMap<FogDevice, Double> componentsWithProbabilities = this
                .getComponentsWithProbabilities(environment, configurationProvider);

        if (componentsWithProbabilities.isEmpty()) {
            doIfNoComponentsFound(environment);
            return false;
        }

        LinkedHashMap<FogDevice, Double> sortedMap = FogDeviceUtils.sortMapByValueDescending(componentsWithProbabilities);
        if (random.nextDouble() > Config.EXPLORE_PROMISE_RATIO) {
            for (Map.Entry<FogDevice, Double> fogDeviceDoubleEntry : sortedMap.entrySet()) {
                getAnt().visitNode(fogDeviceDoubleEntry.getKey());
                return true;
            }
        } else {
            FogDevice fogDevice = FogDeviceUtils.getRandomKeyFromMap(componentsWithProbabilities);
            getAnt().visitNode(fogDevice);
            return true;
        }

        return false;
    }

    public HashMap<FogDevice, Double> getComponentsWithProbabilities(FinalACOEnvironment environment,
                                                                     ConfigurationProvider configurationProvider) {
        HashMap<FogDevice, Double> componentsWithProbabilities = new HashMap<>();

        double denominator = Double.MIN_VALUE;
        List<FogDevice> neighbourhood = getAnt().getNeighbourhood(environment);
        if (neighbourhood == null) {
            throw new SolutionConstructionException("The ant's neighbourhood is null. There are no candidate " +
                    "components to add.");
        }
        for (FogDevice possibleMove : getAnt().getNeighbourhood(environment)) {
            if (!getAnt().isNodeVisited(possibleMove)
                    && getAnt().isNodeValid(possibleMove)) {
                // If it is leaf node and not end, then break

                Double heuristicTimesPheromone = getHeuristicTimesPheromone(
                        environment, configurationProvider, possibleMove);

                denominator += heuristicTimesPheromone;
                componentsWithProbabilities.put(possibleMove, 0.0);
            }
        }
        double totalProbability = 0.0;
        for (Map.Entry<FogDevice, Double> componentWithProbability : componentsWithProbabilities
                .entrySet()) {
            FogDevice component = componentWithProbability.getKey();

            Double numerator = getHeuristicTimesPheromone(environment,
                    configurationProvider, component);
            double probability = numerator / denominator;
            totalProbability += probability;

            componentWithProbability.setValue(probability);
        }
        if (componentsWithProbabilities.isEmpty()) {
            return doIfNoComponentsFound(environment);
        }
        double delta = 0.001;
        if (Math.abs(totalProbability - 1.0) > delta) {
            throw new ConfigurationException("The sum of probabilities for the possible components is " +
                    totalProbability + ". We expect this value to be closer to 1.");
        }
        return componentsWithProbabilities;
    }

    private Double getHeuristicTimesPheromone(FinalACOEnvironment environment,
                                              ConfigurationProvider configurationProvider, FogDevice possibleMove) {
        Double heuristicValue = getAnt().getHeuristicValue(possibleMove,
                getAnt().getCurrentIndex(), environment);
        Double pheromoneTrailValue = getAnt().getPheromoneTrailValue(possibleMove,
                getAnt().getCurrentIndex(), environment);

        if (heuristicValue == null || pheromoneTrailValue == null) {
            throw new SolutionConstructionException("The current ant is not producing valid pheromone/heuristic values" +
                    " for the solution component: " + possibleMove + " .Heuristic value " + heuristicValue +
                    " Pheromone value: " + pheromoneTrailValue);
        }

        return Math.pow(heuristicValue,
                configurationProvider.getHeuristicImportance())
                * Math.pow(pheromoneTrailValue,
                configurationProvider.getPheromoneImportance());
    }

    protected HashMap<FogDevice, Double> doIfNoComponentsFound(FinalACOEnvironment environment) {
        throw new SolutionConstructionException(
                "We have no suitable components to add to the solution from current position."
                        + "\n Previous Component: "
                        + getAnt().getSolution()[getAnt().getCurrentIndex() - 1]
                        + " at position " + (getAnt().getCurrentIndex() - 1)
                        + "\n Environment: " + environment.toString()
                        + "\nPartial solution : " + getAnt().getSolutionAsString());
    }
}
