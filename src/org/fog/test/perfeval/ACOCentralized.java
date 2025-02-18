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
import org.fog.mobilitydata.DataParser;
import org.fog.mobilitydata.Location;
import org.fog.mobilitydata.References;
import org.fog.placement.*;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

import java.io.IOException;
import java.util.*;

/**
 * Build a small simulation
 * Replace the algorithm to whatever another algorithm
 * Where to run?
 * How to run?
 * Make it work
 */
public class ACOCentralized {
    static List<FogDevice> fogDevices = new ArrayList<>();

    static Map<String, Integer> getIdByName = new HashMap<>();

    static List<Sensor> sensors = new ArrayList<Sensor>();

    static List<Actuator> actuators = new ArrayList<Actuator>();

    static LocationHandler locator;

    static Map<Integer, Integer> userMobilityPattern = new HashMap<Integer, Integer>();

    public static final int NUM_OF_FOG_DEVICES = 10;

    static final String DATASET_REFERENCE = References.dataset_reference;

    static final int CLOUD_USER_NUM = 1;

    static final boolean TRACE_FLAG = false;

    static final String APP_ID = "MACCO_Service_Placement";

    static final double SENSOR_TRANSMISSION_TIME = 10;

    static final boolean CLOUD = false;

    // Only one user here and its ID is 1
    static final int NUMBER_OF_MOBILE_USER = 1;

    public static void main(String[] args) {
        Log.printLine("Starting Centralized MAACO");
        try {
            Log.disable();
            // What this is used for?
            Calendar calendar = Calendar.getInstance();
            CloudSim.init(CLOUD_USER_NUM, calendar, TRACE_FLAG);
            FogBroker broker = new FogBroker("broker");
            Application application = createApplication(APP_ID, broker.getId());
            application.setUserId(broker.getId());

            DataParser dataObject = new DataParser();
            locator = new LocationHandler(dataObject);

            // TODO: Define Service placement pattern
            createMobileUser(broker.getId(), APP_ID, DATASET_REFERENCE);
            createFogDevices();

            ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();

            for (FogDevice device : fogDevices) {
                if (device.getName().startsWith("one")) {
                    moduleMapping.addModuleToDevice("service_one", device.getName());
                }
            }

            MobilityController controller = new MobilityController("master_controller", fogDevices, sensors, actuators, locator);
            // The ModulePlacementMobileEdgewards here is the placement policy
            controller.submitApplication(application, 0, new ModulePlacementMobileEdgewards(fogDevices, sensors, actuators, application, moduleMapping));

            TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

            Map<Double, Map<String, Location>> nodesNearRoute = getNodesNearRoute(dataObject);
            System.out.println(nodesNearRoute.size());
            CloudSim.startSimulation();
            CloudSim.stopSimulation();
            Log.printLine("Simulating MAACO finished!");
        } catch (Exception e) {
            e.printStackTrace();
            Log.printLine("Unwanted error happened");
        }
    }

    private static void createMobileUser(int userId, String appId, String datasetReference) throws IOException {
        // From TranslationServiceFog_RandomMobility
        for (int id = 1; id <= NUMBER_OF_MOBILE_USER; id++) {
            userMobilityPattern.put(id, References.DIRECTIONAL_MOBILITY);
        }
        locator.parseUserInfo(userMobilityPattern, datasetReference);
        List<String> mobileUserDataIds = locator.getMobileUserDataId();
        for (int i = 0; i < NUMBER_OF_MOBILE_USER; i++) {
            FogDevice mobile = addMobile("mobile_" + i, userId, appId, References.NOT_SET); // adding mobiles to the physical topology. Smartphones have been modeled as fog devices as well.
            mobile.setUplinkLatency(2); // latency of connection between the smartphone and proxy server is 2 ms
            locator.linkDataWithInstance(mobile.getId(), mobileUserDataIds.get(i));
            fogDevices.add(mobile);
        }
    }

    // This function creates a device which has fog device, sensor, and actuator. They are all mobile, sensor and actuator's gateway is the fog device.
    private static FogDevice addMobile(String name, int userId, String appId, int parentId) {
        FogDevice mobile = createFogDevice(name, 500, 20, 1000, 270, 0, 87.53, 82.44);
        mobile.setParentId(parentId);
        //locator.setInitialLocation(name,drone.getId());

        // Input(SENSOR_TRANSMISSION_TIME): Sends an event/message (from sensor) to another entity by delaying the simulation time from the current time, with a tag representing the event type.
        // This param controls the delay of sending this event/message
        // But still not completely understand:
        // what is sensor's role in this network?
        // Is that an end device?
        // Why the delay would happen?
        // Who triggered this sending task?
        Sensor mobileSensor = new Sensor("sensor-" + name, "M-SENSOR", userId, appId, new DeterministicDistribution(SENSOR_TRANSMISSION_TIME)); // inter-transmission time of EEG sensor follows a deterministic distribution
        sensors.add(mobileSensor);
        Actuator mobileDisplay = new Actuator("actuator-" + name, userId, appId, "M-DISPLAY");
        actuators.add(mobileDisplay);
        mobileSensor.setGatewayDeviceId(mobile.getId());
        // Input
        mobileSensor.setLatency(6.0);  // latency of connection between EEG sensors and the parent Smartphone is 6 ms
        mobileDisplay.setGatewayDeviceId(mobile.getId());
        // Input
        mobileDisplay.setLatency(1.0);  // latency of connection between Display actuator and the parent Smartphone is 1 ms
        return mobile;
    }

    private static void createFogDevicesOrg() {
        FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0.01, 16 * 103, 16 * 83.25);
        fogDevices.add(cloud);
        getIdByName.put(cloud.getName(), cloud.getId());
        for (int i = 0; i < NUM_OF_FOG_DEVICES; i++) {
            FogDevice device = createFogDevice("device", 44800, 40000, 100, 10000, 0.01, 16 * 103, 16 * 83.25);
            device.setParentId(cloud.getId());
            device.setUplinkLatency(10);
            fogDevices.add(device);
            getIdByName.put(device.getName(), device.getId());
        }
    }

    private static void createFogDevices() throws IOException {
        locator.parseResourceInfo();

        if (locator.getLevelWiseResources(locator.getLevelID("Cloud")).size() == 1) {
            // creates the fog device Cloud at the apex of the hierarchy with level=0
            FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0.01, 16 * 103, 16 * 83.25);
            cloud.setParentId(References.NOT_SET);
            locator.linkDataWithInstance(cloud.getId(), locator.getLevelWiseResources(locator.getLevelID("Cloud")).get(0));
            fogDevices.add(cloud);

            for (int i = 0; i < locator.getLevelWiseResources(locator.getLevelID("Proxy")).size(); i++) {
                // creates the fog device Proxy Server (level=1)
                FogDevice proxy = createFogDevice("proxy-server_" + i, 2800, 4000, 10000, 10000, 0.0, 107.339, 83.4333);
                locator.linkDataWithInstance(proxy.getId(), locator.getLevelWiseResources(locator.getLevelID("Proxy")).get(i));
                proxy.setParentId(cloud.getId()); // setting Cloud as parent of the Proxy Server
                proxy.setUplinkLatency(100); // latency of connection from Proxy Server to the Cloud is 100 ms
                fogDevices.add(proxy);

            }

            for (int i = 0; i < locator.getLevelWiseResources(locator.getLevelID("Gateway")).size(); i++) {
                FogDevice gateway = createFogDevice("gateway_" + i, 2800, 4000, 10000, 10000, 0.0, 107.339, 83.4333);
                locator.linkDataWithInstance(gateway.getId(), locator.getLevelWiseResources(locator.getLevelID("Gateway")).get(i));
                gateway.setParentId(locator.determineParent(gateway.getId(), References.SETUP_TIME));
                gateway.setUplinkLatency(4);
                fogDevices.add(gateway);
            }

        }
    }

    private static void createFogDevicesV1() {
        for (int i = 0; i < NUM_OF_FOG_DEVICES; i++) {
            FogDevice device = createFogDevice("FogDevice_" + i, 2800, 4000, 10000, 10000, 0.0, 107.339, 83.4333);
            // Do we need to set parent ID to References.NOT_SET?
            device.setUplinkLatency(10);
            fogDevices.add(device);
            getIdByName.put(device.getName(), device.getId());
        }
    }

    private static FogDevice createFogDevice(String nodeName, long mips,
                                             int ram, long upBw, long downBw, double ratePerMips, double busyPower, double idlePower) {
        List<Pe> peList = new ArrayList<>();
        // MIPS is "Millions of Instructions Per Second." It is a measure of the performance of a computer's processor or CPU
        peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));
        int hostId = FogUtils.generateEntityId();
        long storage = 1000000;
        int bw = 10000;
        // PowerHost class enables simulation of power-aware hosts.
        PowerHost host = new PowerHost(hostId,
                new RamProvisionerSimple(ram),
                new BwProvisionerOverbooking(bw),
                storage,
                peList,
                new StreamOperatorScheduler(peList),
                new FogLinearPowerModel(busyPower, idlePower));
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
        FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
                arch, os, vmm, host, time_zone, cost, costPerMem, costPerStorage, costPerBw);
        FogDevice fogdevice = null;
        try {
            fogdevice = new FogDevice(nodeName, characteristics, new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, 0, ratePerMips);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return fogdevice;
    }

    /**
     * This function creates a Sequential Unidirectional dataflow application model
     *
     * @param appId    a
     * @param brokerId b
     * @return r
     */
    private static Application createApplication(String appId, int brokerId) {
        // python/SQL
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

    private static Map<Double, Map<String, Location>> getNodesNearRoute(DataParser dataObject) {
        // Time, <ID of FogDevice, Location>
        Map<Double, Map<String, Location>> res = new HashMap<>();
        // Only one user here (From userMobilityPattern)
        String userId = "usr_1";
        for (Map.Entry<Double, Location> user : dataObject.usersLocation.get(userId).entrySet()) {
            // usersLocation.put("usr_" + userID, tempUserLocationInfo);
            // userID, time (References.INIT_TIME(const, unuseful)), location
            // for every location, find nodes around the location,
            Location location = user.getValue();
            // TODO: Define the scope
            double latitudeMin = location.latitude;
            double latitudeMax = location.latitude;
            double longitudeMin = location.longitude;
            double longitudeMax = location.longitude;

            for (Map.Entry<String, Location> each : locator.dataObject.resourceLocationData.entrySet()) {
                if (each.getValue().longitude > longitudeMin &&
                        each.getValue().longitude < longitudeMax &&
                        each.getValue().latitude > latitudeMin &&
                        each.getValue().latitude < latitudeMax) {
                    Map<String, Location> tempResLocationInfo = new HashMap<>();
                    tempResLocationInfo.put(each.getKey(), each.getValue());
                    res.put(user.getKey(), tempResLocationInfo);
                }
            }
        }
        return res;
    }
}
