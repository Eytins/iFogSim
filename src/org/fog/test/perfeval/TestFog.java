package org.fog.test.perfeval;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.*;
import org.fog.placement.Controller;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

import java.util.*;

/**
 * Build a small simulation
 * Replace the algorithm to whatever another algorithm
 * Where to run?
 * How to run?
 * Make it work
 */
public class TestFog {
    static int numOfFogDevices = 10;

    static List<FogDevice> fogDevices = new ArrayList<>();

    static Map<String, Integer> getIdByName = new HashMap<>();

    private static void createFogDevices() {
        FogDevice cloud = createAFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16 * 103, 16 * 83.25);
        fogDevices.add(cloud);
        getIdByName.put(cloud.getName(), cloud.getId());
        for (int i = 0; i < numOfFogDevices; i++) {
            FogDevice device = createAFogDevice("FogDevice-" + i, getValue(12000, 15000), getValue(4000, 8000), getValue(200, 300), getValue(500, 1000), 1, 0.01, getValue(100, 120), getValue(70, 75));
            device.setParentId(cloud.getId());
            device.setUplinkLatency(10);
            fogDevices.add(device);
            getIdByName.put(device.getName(), device.getId());
        }
    }

    private static FogDevice createAFogDevice(String nodeName, long mips, int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
        List<Pe> peList = new ArrayList<>();
        // MIPS is "Millions of Instructions Per Second." It is a measure of the performance of a computer's processor or CPU
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));
        int hostId = FogUtils.generateEntityId();
        long storage = 1000000;
        int bw = 10000;
        // PowerHost class enables simulation of power-aware hosts.
        PowerHost host = new PowerHost(hostId, new RamProvisionerSimple(ram), new BwProvisionerOverbooking(bw), storage, peList, new StreamOperatorScheduler(peList), new FogLinearPowerModel(busyPower, idlePower));
        List<Host> hostList = new ArrayList<>();
        // hostList includes a power-aware host
        hostList.add(host);
        String arch = "x86";
        String os = "Linux";
        String vmm = "Xen";
        double time_zone = 10.0;
        double cost = 3.0;
        double costPerMem = 0.05;
        double costPerStorage = 0.001;
        double costPerBw = 0.0;
        LinkedList<Storage> storageList = new LinkedList<>();
        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(arch, os, vmm, host, time_zone, cost, costPerMem, costPerStorage, costPerBw);
        FogDevice fogdevice = null;
        try {
            fogdevice = new FogDevice(nodeName, characteristics, new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
        } catch (Exception e) {
            e.printStackTrace();
        }
        fogdevice.setLevel(level);
        return fogdevice;
    }

    /**
     * This function creates a Sequential Unidirectional dataflow application model
     * @param appId a
     * @param brokerId b
     * @return r
     */
    private static Application createApplication(String appId, int brokerId) {
        Application application = Application.createApplication(appId, brokerId);
        application.addAppModule("Module1", 10);
        application.addAppModule("Module2", 10);
        application.addAppModule("Module3", 10);
        application.addAppModule("Module4", 10);
        application.addAppEdge("Sensor", "Module1", 3000, 500, "Sensor", Tuple.UP, AppEdge.SENSOR);
        application.addAppEdge("Module1", "Module2", 100, 1000, "ProcessedData-1", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("Module2", "Module3", 100, 1000, "ProcessedData-2", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("Module3", "Module4", 100, 1000, "ProcessedData-3", Tuple.UP, AppEdge.MODULE);
        application.addAppEdge("Module4", "Module1", 100, 1000, "ProcessedData-4", Tuple.DOWN, AppEdge.MODULE);
        application.addAppEdge("Module1", "Actuators", 100, 50, "OutputData", Tuple.DOWN, AppEdge.ACTUATOR);
        application.addTupleMapping("Module1", "Sensor", "ProcessedData-1", new FractionalSelectivity(1.0));
        application.addTupleMapping("Module2", "ProcessedData-1", "ProcessedData-2", new FractionalSelectivity(1.0));
        application.addTupleMapping("Module3", "ProcessedData-2", "ProcessedData-3", new FractionalSelectivity(1.0));
        application.addTupleMapping("Module4", "ProcessedData-3", "ProcessedData-4", new FractionalSelectivity(1.0));
        application.addTupleMapping("Module1", "ProcessedData-4", "OutputData", new FractionalSelectivity(1.0));
        final AppLoop loop1 = new AppLoop(new ArrayList<String>() {{
            add("Sensor");
            add("Module1");
            add("Module2");
            add("Module3");
            add("Module4");
            add("Module1");
            add("Actuator");
        }});
        List<AppLoop> loops = new ArrayList<AppLoop>() {{
            add(loop1);
        }};
        application.setLoops(loops);
        return application;
    }

    private static double getValue(double min) {
        Random rn = new Random();
        return rn.nextDouble() * 10 + min;
    }

    private static double getValue(double min, double max) {
        Random r = new Random();
        double randomValue = min + (max - min) * r.nextDouble();
        return randomValue;
    }

    private static int getValue(int min, int max) {
        Random r = new Random();
        int randomValue = min + r.nextInt() % (max - min);
        return randomValue;
    }
}
