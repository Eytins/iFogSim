package org.fog.utils;

import org.apache.commons.math3.util.Pair;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.entities.MicroserviceFogDevice;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.mobilitydata.DataParser;
import org.fog.mobilitydata.Location;
import org.fog.placement.LocationHandler;
import org.fog.placement.MicroservicesMobilityClusteringController2;

import java.io.*;
import java.util.*;

/**
 *
 */
public class FogDeviceUtils {
    /**
     * Get all nodes which locate near the user's route (including repetition)
     *
     * @param dataObject dataObject
     * @param locator    LocationHandler
     * @return Map<Time, Map < FogDeviceResID, Location>>
     */
    public static Map<Double, Map<String, Location>> getNodesNearRoute(DataParser dataObject, LocationHandler locator) {
        Map<Double, Map<String, Location>> res = new HashMap<>();
        // Only one user here (From userMobilityPattern)
        String userId = "usr_1";
        for (Map.Entry<Double, Location> doubleLocationEntry : dataObject.usersLocation.get(userId).entrySet()) {
            // userID, time (References.INIT_TIME(const, unuseful)), location
            // usersLocation.put("usr_" + userID, tempUserLocationInfo);
            // for every location, find nodes around the location,
            Location location = doubleLocationEntry.getValue();
            double latitudeMin = location.latitude - 0.0004495; // 50m
            double latitudeMax = location.latitude + 0.0004495;
            double longitudeMin = location.longitude - 0.0005705; // 50m
            double longitudeMax = location.longitude + 0.0005705;

            for (Map.Entry<String, Location> each : locator.dataObject.resourceLocationData.entrySet()) {
                if (each.getValue().longitude > longitudeMin &&
                        each.getValue().longitude < longitudeMax &&
                        each.getValue().latitude > latitudeMin &&
                        each.getValue().latitude < latitudeMax) {
                    Map<String, Location> tempResLocationInfo = new HashMap<>();
                    tempResLocationInfo.put(each.getKey(), each.getValue());
                    res.put(doubleLocationEntry.getKey(), tempResLocationInfo);
                }
            }
        }
        return res;
    }

    /**
     * Get the start node near the user's route (Time is 0.0)
     *
     * @param allNodes All nodes
     * @return The start node
     */
    public static Pair<String, Location> getStartNode(Map<Double, Map<String, Location>> allNodes) {
        Map<String, Location> map = allNodes.get(0.0);
        Pair<String, Location> pair = null;
        for (Map.Entry<String, Location> entry : map.entrySet()) {
            pair = new Pair<>(entry.getKey(), entry.getValue());
        }
        return pair;
    }

    /**
     * Get the end node near the user's route (Time is max)
     *
     * @param allNodes All nodes
     * @return The end node
     */
    public static Pair<String, Location> getEndNode(Map<Double, Map<String, Location>> allNodes) {
        double max = 0;
        for (Double aDouble : allNodes.keySet()) {
            max = Math.max(aDouble, max);
        }
        Pair<String, Location> pair = null;
        Map<String, Location> map = allNodes.get(max);
        for (Map.Entry<String, Location> entry : map.entrySet()) {
            pair = new Pair<>(entry.getKey(), entry.getValue());
        }
        return pair;
    }

    /**
     * Get the Integer ID of FogDevice By its String Resource ID
     *
     * @param dataId resID
     * @return Instance ID
     */
    public static int getInstanceIdByDataId(String dataId, LocationHandler locator) {
        Map<Integer, String> idReferences = locator.getInstenceDataIdReferences();
        for (Map.Entry<Integer, String> integerStringEntry : idReferences.entrySet()) {
            if (integerStringEntry.getValue().equals(dataId)) {
                return integerStringEntry.getKey();
            }
        }
        return 0;
    }

    /**
     * Transfer Fog Device list to map
     *
     * @param list Fog Device list
     * @return map whose key is Fog Device ID, value is FogDevice
     */
    public static Map<Integer, FogDevice> fogDeviceListToMap(List<FogDevice> list) {
        Map<Integer, FogDevice> map = new HashMap<>();
        for (FogDevice device : list) {
            map.put(device.getId(), device);
        }
        return map;
    }

    /**
     * Create the matrix which contains the map of latency between Fog Devices.
     *
     * @param fogDevices Fog Devices
     * @return Map<SendDeviceID, Map < ReceiveDeviceID, Latency>>
     */
    public static Map<Integer, Map<Integer, Double>> createLatencyMatrix(List<FogDevice> fogDevices) {
        Map<Integer, Map<Integer, Double>> matrix = new HashMap<>();
        for (FogDevice sendDevice : fogDevices) {
            Map<Integer, Double> childToLatencyMap = sendDevice.getChildToLatencyMap();
            childToLatencyMap.put(sendDevice.getParentId(), sendDevice.getUplinkLatency());
            Map<Integer, Double> clusterMembersToLatencyMap = sendDevice.getClusterMembersToLatencyMap();
            if (clusterMembersToLatencyMap != null) {
                childToLatencyMap.putAll(clusterMembersToLatencyMap);
            }
            matrix.put(sendDevice.getId(), childToLatencyMap);
        }
        return matrix;
    }

    public static int getMaxFogDeviceID(List<FogDevice> fogDevices) {
        int maxId = 0;
        for (FogDevice device : fogDevices) {
            maxId = Math.max(maxId, device.getId());
        }
        return maxId;
    }

    public static int getIndexOfFogDeviceById(List<FogDevice> fogDevices, int id) {
        for (int i = 0; i < fogDevices.size(); i++) {
            if (fogDevices.get(i).getId() == id) {
                return i;
            }
        }
        return 0;
    }

    private static final double EARTH_RADIUS = 6371000; // Earth's radius in meters

    /**
     * Calculate the distance of two devices
     *
     * @param device1 one fog device
     * @param device2 another fog device
     * @param locator LocationHandler
     * @return The distance in meters
     */
    public static double calculateDistanceBetweenDevices(FogDevice device1, FogDevice device2, LocationHandler locator) {
        double device1Latitude = locator.dataObject.resourceLocationData.get(locator.instanceToDataId.get(device1.getId())).latitude;
        double device1Longitude = locator.dataObject.resourceLocationData.get(locator.instanceToDataId.get(device1.getId())).longitude;
        double device2Latitude = locator.dataObject.resourceLocationData.get(locator.instanceToDataId.get(device2.getId())).latitude;
        double device2Longitude = locator.dataObject.resourceLocationData.get(locator.instanceToDataId.get(device2.getId())).longitude;
        return calculateDistance(device1Latitude, device1Longitude, device2Latitude, device2Longitude);
    }

    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    public static LinkedHashMap<FogDevice, Double> sortMapByValueDescending(Map<FogDevice, Double> unsortedMap) {
        // Convert the Map to a List of Map.Entry objects
        List<Map.Entry<FogDevice, Double>> list = new ArrayList<>(unsortedMap.entrySet());

        // Use Collections.sort with a custom Comparator to sort the list based on values
        Collections.sort(list, new Comparator<Map.Entry<FogDevice, Double>>() {
            public int compare(Map.Entry<FogDevice, Double> o1, Map.Entry<FogDevice, Double> o2) {
                // Sort in descending order (from big to small)
                return o2.getValue().compareTo(o1.getValue());
            }
        });

        // Convert the sorted list back to a Map
        LinkedHashMap<FogDevice, Double> sortedMap = new LinkedHashMap<>();
        for (Map.Entry<FogDevice, Double> entry : list) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }

    public static <K, V> K getRandomKeyFromMap(Map<K, V> map) {
        if (map.isEmpty()) {
            throw new IllegalArgumentException("Map is empty. Cannot choose a random key.");
        }

        // Generate a random index within the range of the map size
        int randomIndex = new Random().nextInt(map.size());

        // Access the key at the randomly generated index
        Iterator<K> iterator = map.keySet().iterator();
        K randomKey = null;
        for (int i = 0; i <= randomIndex; i++) {
            randomKey = iterator.next();
        }

        return randomKey;
    }

    /**
     * Get the indirect latency from one fog device to another
     *
     * @param sender   sender
     * @param receiver receiver
     * @return the latency
     */
    public static double getIndirectLatency(FogDevice sender, FogDevice receiver, Map<Integer, FogDevice> fogDeviceMap) {
        Map<Integer, Double> senderChildToLatencyMap = sender.getChildToLatencyMap();
        int receiverId = receiver.getId();

        int senderLevel = sender.getLevel();
        int receiverLevel = receiver.getLevel();

        if (senderLevel == 0) {
            if (receiverLevel == 1) {
                return senderChildToLatencyMap.get(receiverId);
            } else if (receiverLevel == 2) {
                for (Map.Entry<Integer, Double> entry : senderChildToLatencyMap.entrySet()) {
                    FogDevice proxy = fogDeviceMap.get(entry.getKey());
                    Map<Integer, Double> childToLatencyMap = proxy.getChildToLatencyMap();
                    if (childToLatencyMap.containsKey(receiverId)) {
                        return entry.getValue() + childToLatencyMap.get(receiverId);
                    }
                }
            } else {
                return Double.NaN;
            }
        } else if (senderLevel == 1) {
            if (receiverLevel == 0) {
                return sender.getUplinkLatency();
            } else if (receiverLevel == 1) {
                FogDevice cloud = fogDeviceMap.get(sender.getParentId());
                Map<Integer, Double> childToLatencyMap = cloud.getChildToLatencyMap();
                if (childToLatencyMap.containsKey(receiverId)) {
                    return childToLatencyMap.get(receiverId) + sender.getUplinkLatency();
                }
            } else if (receiverLevel == 2) {
                if (senderChildToLatencyMap.containsKey(receiverId)) {
                    return senderChildToLatencyMap.get(receiverId);
                } else {
                    FogDevice cloud = fogDeviceMap.get(sender.getParentId());
                    Map<Integer, Double> cloudChildToLatencyMap = cloud.getChildToLatencyMap();
                    for (Map.Entry<Integer, Double> entry : cloudChildToLatencyMap.entrySet()) {
                        FogDevice proxy = fogDeviceMap.get(entry.getKey());
                        Map<Integer, Double> proxyChildToLatencyMap = proxy.getChildToLatencyMap();
                        if (proxyChildToLatencyMap.containsKey(receiverId)) {
                            return sender.getUplinkLatency() + entry.getValue() + proxyChildToLatencyMap.get(receiverId);
                        }
                    }
                }
            } else {
                return Double.NaN;
            }
        } else if (senderLevel == 2) {
            if (receiverLevel == 0) {
                return sender.getUplinkLatency() + fogDeviceMap.get(sender.getParentId()).getUplinkLatency();
            } else if (receiverLevel == 1) {
                if (sender.getParentId() == receiverId) {
                    return sender.getUplinkLatency();
                } else {
                    FogDevice senderParent = fogDeviceMap.get(sender.getParentId());
                    FogDevice cloud = fogDeviceMap.get(senderParent.getParentId());
                    Map<Integer, Double> cloudChildToLatencyMap = cloud.getChildToLatencyMap();
                    for (Map.Entry<Integer, Double> entry : cloudChildToLatencyMap.entrySet()) {
                        if (receiverId == entry.getKey()) {
                            return sender.getUplinkLatency() + senderParent.getUplinkLatency() + entry.getValue();
                        }
                    }
                }
            } else if (receiverLevel == 2) {
                FogDevice senderParent = fogDeviceMap.get(sender.getParentId());
                Map<Integer, Double> senderParentChildToLatencyMap = senderParent.getChildToLatencyMap();
                for (Map.Entry<Integer, Double> entry : senderParentChildToLatencyMap.entrySet()) {
                    if (entry.getKey() == receiverId) {
                        return sender.getUplinkLatency() + entry.getValue();
                    }
                }
                FogDevice cloud = fogDeviceMap.get(senderParent.getParentId());
                Map<Integer, Double> cloudChildToLatencyMap = cloud.getChildToLatencyMap();
                for (Map.Entry<Integer, Double> entry : cloudChildToLatencyMap.entrySet()) {
                    FogDevice proxy = fogDeviceMap.get(entry.getKey());
                    Map<Integer, Double> proxyChildToLatencyMap = proxy.getChildToLatencyMap();
                    for (Map.Entry<Integer, Double> innerEntry : proxyChildToLatencyMap.entrySet()) {
                        if (innerEntry.getKey() == receiverId) {
                            return sender.getUplinkLatency() + senderParent.getUplinkLatency()
                                    + entry.getValue() + innerEntry.getValue();
                        }
                    }

                }
            } else {
                return Double.NaN;
            }
        }
        return 0;
    }

    /**
     * Create the matrix that contains the latency info of all devices
     *
     * @param fogDevices All fog devices
     * @return the matrix which the first index represents the id of sender, the second represents the receiver
     */
    public static double[][] createLatencyMatrixOfAllDevices(List<FogDevice> fogDevices, Map<String, List<FogDevice>> modulesAvailableDevices, List<AppModule> modules) {
        int maxFogDeviceID = getMaxFogDeviceID(fogDevices);
        Map<Integer, FogDevice> fogDeviceMap = fogDeviceListToMap(fogDevices);
        double[][] result = new double[maxFogDeviceID + 1][maxFogDeviceID + 1];

        for (AppModule appModule : modules) {
            modulesAvailableDevices.put(appModule.getName(), new ArrayList<>());
        }

        for (int i = 0; i < result.length; i++) {
            FogDevice fogDeviceI = fogDeviceMap.getOrDefault(i, null);
            if (fogDeviceI == null) {
                continue;
            }
            for (AppModule module : modules) {
                if (fogDeviceI.getHost().getRam() > module.getRam()) {
                    List<FogDevice> list = modulesAvailableDevices.get(module.getName());
                    list.add(fogDeviceI);
                    modulesAvailableDevices.put(module.getName(), list);
                }
            }

            for (int j = 0; j < result.length; j++) {
                if (i == j) {
                    result[i][j] = Double.NaN;
                    continue;
                }
                FogDevice fogDeviceJ = fogDeviceMap.getOrDefault(j, null);
                if (fogDeviceI == null || fogDeviceJ == null) {
                    result[i][j] = Double.NaN;
                    continue;
                }
                if (fogDeviceI.getName().startsWith("mobile") || fogDeviceJ.getName().startsWith("mobile")) {
                    result[i][j] = Double.NaN;
                    continue;
                }
                result[i][j] = 1 / getIndirectLatency(fogDeviceI, fogDeviceJ, fogDeviceMap);
            }
        }

        return result;
    }


    /**
     * Gets all sensors associated with fog-device <b>device</b>
     * @param device The fog device
     * @return map from sensor type to number of such sensors
     */
    public static Map<String, Integer> getAssociatedSensors(FogDevice device, List<Sensor> sensors) {
        Map<String, Integer> endpoints = new HashMap<>();
        for(Sensor sensor : sensors){
            if(sensor.getGatewayDeviceId()==device.getId()){
                if(!endpoints.containsKey(sensor.getTupleType()))
                    endpoints.put(sensor.getTupleType(), 0);
                endpoints.put(sensor.getTupleType(), endpoints.get(sensor.getTupleType())+1);
            }
        }
        return endpoints;
    }

    /**
     * Method to create a copy of a 2D double array
     *
     * @param original Original array
     * @return New array
     */
    public static double[][] copy2DArray(double[][] original) {
        int rows = original.length;
        int cols = original[0].length;
        double[][] copy = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, cols);
        }
        return copy;
    }

    /**
     * Get the current time after running placed modules on devices
     * <p>
     * In normal situation, the size of two lists should be the same.
     *
     * @param modules       Modules been placed
     * @param fogDevices    Devices that have been used to place modules
     * @param latencyMatrix The matrix that saves the latency information
     *                      between any two devices
     * @return Pasted time
     */
    public static double getCurTimeAfterRunModulesOnDevices(List<AppModule> modules,
                                                            List<FogDevice> fogDevices,
                                                            double[][] latencyMatrix) {
        double result = 0.0;
        for (int i = 0; i < modules.size(); i++) {
            result += modules.get(i).getSize() / fogDevices.get(i).getUplinkBandwidth();
        }
        for (int i = 1; i < fogDevices.size(); i++) {
            result += latencyMatrix[fogDevices.get(i - 1).getId()][fogDevices.get(i).getId()];
        }
        return result;
    }

    public static List<FogDevice> getDevicesSatisfyModuleRequirement(List<FogDevice> fogDevices, AppModule module) {
        List<FogDevice> result = new ArrayList<>();
        for (FogDevice fogDevice : fogDevices) {
            if (fogDevice.getHost().getRam() > module.getRam()) {
                result.add(fogDevice);
            }
        }
        return result;
    }

    /**
     * From all devices that satisfy the requirement of first module,
     * choose the closet one with user's init location
     * @param fogDevices All fog devices
     * @param application application
     * @param locator locator
     * @return the Fog Device
     */
    public static FogDevice getInitialFogDevice(List<FogDevice> fogDevices, Application application, LocationHandler locator) {
        List<FogDevice> satisfiedDevices = getDevicesSatisfyModuleRequirement(fogDevices, application.getModules().get(0));
        Location userInitLocation = locator.dataObject.usersLocation.get(Config.USER_NAME).get(0.0);
        FogDevice result = null;
        double minDistance = Double.MAX_VALUE;
        for (int i = 0; i < satisfiedDevices.size(); i++) {
            Location location = locator.dataObject.resourceLocationData.get(locator.instanceToDataId.get(satisfiedDevices.get(i).getId()));
            double distance = calculateDistance(userInitLocation.latitude, userInitLocation.longitude,
                    location.latitude, location.latitude);
            if (distance < minDistance) {
                minDistance = distance;
                result = satisfiedDevices.get(i);
            }
        }
        return result;
    }
}
