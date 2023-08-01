package org.fog.placement;

import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;

import java.util.List;

public class ACOPlacementLogic extends ModulePlacement {
    public ACOPlacementLogic(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators,
                             Application application, ModuleMapping moduleMapping) {

    }

    @Override
    protected void mapModules() {
        // 1. Create latency matrix



        // createModuleInstanceOnDevice
        // setModuleInstanceCountMap
    }
}
