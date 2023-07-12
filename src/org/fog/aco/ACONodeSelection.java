package org.fog.aco;

import isula.aco.AntPolicy;
import isula.aco.AntPolicyType;
import isula.aco.ConfigurationProvider;
import isula.aco.Environment;

import java.util.Random;

public class ACONodeSelection <C, E extends Environment> extends AntPolicy<C, E> {
    public ACONodeSelection() {
        super(AntPolicyType.NODE_SELECTION);
    }

    @Override
    public boolean applyPolicy(E environment, ConfigurationProvider configurationProvider) {
        Random random = new Random();
        C nextNode = null;
        return false;
    }
}
