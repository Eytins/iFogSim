package org.fog.utils;

import org.fog.entities.FogDevice;

import java.io.Serializable;
import java.util.List;

public class SerializableFogDevices implements Serializable {
    public SerializableFogDevices(List<FogDevice> fogDevices) {
        this.fogDevices = fogDevices;
    }

    List<FogDevice> fogDevices;

    public List<FogDevice> getFogDevices() {
        return fogDevices;
    }

    public void setFogDevices(List<FogDevice> fogDevices) {
        this.fogDevices = fogDevices;
    }
}
