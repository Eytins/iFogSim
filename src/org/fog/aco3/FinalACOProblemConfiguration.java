package org.fog.aco3;

import isula.aco.ConfigurationProvider;

import java.util.List;

public class FinalACOProblemConfiguration implements ConfigurationProvider {
    public FinalACOProblemConfiguration(FinalACOEnvironment environment) {

    }

    @Override
    public int getNumberOfAnts() {
        return 50;
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
        return 0;
    }

    @Override
    public double getHeuristicImportance() {
        return 1.0;
    }

    @Override
    public double getPheromoneImportance() {
        return 2.5;
    }
}
