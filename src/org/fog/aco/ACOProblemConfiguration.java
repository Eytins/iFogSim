package org.fog.aco;

import isula.aco.ConfigurationProvider;

import java.util.*;

public class ACOProblemConfiguration implements ConfigurationProvider {

    private final double initialPheromoneValue;

    public ACOProblemConfiguration(ACOEnvironment environment) {
        // Create a random solution which can reach the goal
        List<Double> randomSolutionLatencies = new ArrayList<>();
        Random random = new Random();
        int solutionRange = environment.getNumOfServices();

        List<Double> latencies = new ArrayList<>();
        Map<Integer, Map<Integer, Double>> latencyMatrix = environment.getLatencyMatrix();
        for (Map<Integer, Double> value : latencyMatrix.values()) {
            latencies.addAll(value.values());
        }

        Collections.shuffle(latencies);

        for (int latencyIndex = 0; latencyIndex < solutionRange; latencyIndex += 1) {
            randomSolutionLatencies.add(latencies.get(latencyIndex));
        }
        double costSum = randomSolutionLatencies.stream().mapToDouble(Double::doubleValue).sum();
        this.initialPheromoneValue = solutionRange / costSum;
    }

    @Override
    public int getNumberOfAnts() {
        return 100;
    }

    @Override
    public double getEvaporationRatio() {
        return 1 - 0.6;
    }

    @Override
    public int getNumberOfIterations() {
        return 100;
    }

    @Override
    public double getInitialPheromoneValue() {
        return this.initialPheromoneValue;
    }

    @Override
    public double getHeuristicImportance() {
        return 2.5;
    }

    @Override
    public double getPheromoneImportance() {
        return 1.0;
    }
}
