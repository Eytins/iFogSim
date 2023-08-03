package org.fog.aco3;

import isula.aco.ConfigurationProvider;
import isula.aco.DaemonAction;
import isula.aco.DaemonActionType;
import isula.aco.Environment;

import java.util.logging.Logger;

public class FinalACOStartPheromoneMatrix<C, FinalACOEnvironment extends Environment> extends DaemonAction<C, org.fog.aco3.FinalACOEnvironment> {
    private static Logger logger = Logger
            .getLogger(FinalACOStartPheromoneMatrix.class.getName());

    public FinalACOStartPheromoneMatrix() {
        super(DaemonActionType.INITIAL_CONFIGURATION);
    }

    @Override
    public void applyDaemonAction(ConfigurationProvider configurationProvider) {
        logger.fine("INITIALIZING PHEROMONE MATRIX");
        logger.fine("Initial pheromone matrix: created from environment");
        getEnvironment().setPheromoneMatrix(getEnvironment().createPheromoneMatrix());
    }
}
