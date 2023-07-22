package org.fog.aco;

import isula.aco.AntPolicy;
import isula.aco.AntPolicyType;
import isula.aco.ConfigurationProvider;
import isula.aco.Environment;
import isula.aco.exception.ConfigurationException;
import isula.aco.exception.SolutionConstructionException;
import org.fog.entities.FogDevice;

import java.util.*;

public class ACONodeSelection extends AntPolicy<FogDevice, ACOEnvironment> {
    public ACONodeSelection() {
        super(AntPolicyType.NODE_SELECTION);
    }

    @Override
    public boolean applyPolicy(ACOEnvironment environment, ConfigurationProvider configurationProvider) {
        if (getAnt().getCurrentIndex() == 0) {
            getAnt().visitNode(environment.getFogDevices().get(environment.getIndexOfStartNode()));
            return true;
        }

        Random random = new Random();
        FogDevice nextNode = null;

        double value = random.nextDouble();
        double total = 0;

        HashMap<FogDevice, Double> componentsWithProbabilities = this
                .getComponentsWithProbabilities(environment, configurationProvider);

        if (componentsWithProbabilities.size() < 1) {
            // Set this ant as invalid
            getAnt().setCurrentIndex(Integer.MIN_VALUE);
            return true;
        }

        Iterator<Map.Entry<FogDevice, Double>> componentWithProbabilitiesIterator = componentsWithProbabilities
                .entrySet().iterator();
        while (componentWithProbabilitiesIterator.hasNext()) {
            Map.Entry<FogDevice, Double> componentWithProbability = componentWithProbabilitiesIterator
                    .next();

            Double probability = componentWithProbability.getValue();
            if (probability.isNaN()) {
                throw new ConfigurationException("The probability for component " + componentWithProbability.getKey() +
                        " is not a number.");
            }

            total += probability;

            if (total >= value) {
                nextNode = componentWithProbability.getKey();
                getAnt().visitNode(nextNode);
                return true;
            }
        }

        return false;
    }

    /**
     * Gets a probabilities vector, containing probabilities to move to each node
     * according to pheromone matrix.
     *
     * @param environment           Environment that ants are traversing.
     * @param configurationProvider Configuration provider.
     * @return Probabilities for the adjacent nodes.
     */
    public HashMap<FogDevice, Double> getComponentsWithProbabilities(ACOEnvironment environment,
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

        Double totalProbability = 0.0;
        Iterator<Map.Entry<FogDevice, Double>> componentWithProbabilitiesIterator = componentsWithProbabilities
                .entrySet().iterator();
        while (componentWithProbabilitiesIterator.hasNext()) {
            Map.Entry<FogDevice, Double> componentWithProbability = componentWithProbabilitiesIterator
                    .next();
            FogDevice component = componentWithProbability.getKey();

            Double numerator = getHeuristicTimesPheromone(environment,
                    configurationProvider, component);
            Double probability = numerator / denominator;
            totalProbability += probability;

            componentWithProbability.setValue(probability);
        }

        FogDevice[] solution = getAnt().getSolution();
        Map<FogDevice, Boolean> visited = getAnt().getVisited();
        int currentIndex = getAnt().getCurrentIndex();


        if (componentsWithProbabilities.size() < 1) {
//            return doIfNoComponentsFound(environment, configurationProvider);
            return componentsWithProbabilities;
        }
        double delta = 0.001;
        if (Math.abs(totalProbability - 1.0) > delta) {
            throw new ConfigurationException("The sum of probabilities for the possible components is " +
                    totalProbability + ". We expect this value to be closer to 1.");
        }

        return componentsWithProbabilities;
    }


    protected HashMap<FogDevice, Double> doIfNoComponentsFound(ACOEnvironment environment,
                                                               ConfigurationProvider configurationProvider) {
        throw new SolutionConstructionException(
                "We have no suitable components to add to the solution from current position."
                        + "\n Previous Component: "
                        + getAnt().getSolution()[getAnt().getCurrentIndex() - 1]
                        + " at position " + (getAnt().getCurrentIndex() - 1)
                        + "\n Environment: " + environment.toString()
                        + "\nPartial solution : " + getAnt().getSolutionAsString());
    }

    private Double getHeuristicTimesPheromone(ACOEnvironment environment,
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
}
