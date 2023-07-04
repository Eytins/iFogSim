package org.fog.utils;

import org.apache.commons.math3.util.Pair;
import org.fog.entities.FogDevice;
import org.fog.mobilitydata.DataParser;
import org.fog.mobilitydata.Location;
import org.fog.placement.LocationHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FogDeviceUtils {
    /**
     * Get all nodes which locate near the user's route (including repetition)
     * @param dataObject dataObject
     * @param locator LocationHandler
     * @return Map<Time, Map<FogDeviceResID, Location>>
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
     * @param fogDevices Fog Devices
     * @return Map<SendDeviceID, Map<ReceiveDeviceID, Latency>>
     */
    public static Map<Integer, Map<Integer, Double>> createLatencyMatrix(List<FogDevice> fogDevices) {
        Map<Integer, Map<Integer, Double>> matrix = new HashMap<>();
        for (FogDevice sendDevice : fogDevices) {
            Map<Integer, Double> childToLatencyMap = sendDevice.getChildToLatencyMap();
            childToLatencyMap.put(sendDevice.getParentId(), sendDevice.getUplinkLatency());
            matrix.put(sendDevice.getId(), childToLatencyMap);
        }
        return matrix;
    }
}
