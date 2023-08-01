package org.fog.aco2;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class ACO {

    private static final String FILE_NAME = "aa.nrp";

    private static final Logger logger = Logger.getLogger(ACO.class.getName());

    public static void main(String[] args) {
    }

    /**
     * Why are we getting the representation from a file?
     * What is x and y coordinates represent?
     * Does the representation only has two columns?
     * Or it's based on the problem type?
     * @param fileName
     * @return
     * @throws IOException
     */
    public static double[][] getRepresentationFromFile(String fileName) throws IOException {
        List<Double> xCoordinates = new ArrayList<>();
        List<Double> yCoordinates = new ArrayList<>();
        File file = new File(Objects.requireNonNull(ACO.class.getClassLoader().getResource(fileName)).getFile());
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] tokens = line.split(" ");

                if (tokens.length == 3) {
                    xCoordinates.add(Double.parseDouble(tokens[1]));
                    yCoordinates.add(Double.parseDouble(tokens[2]));
                }
            }
        } catch (Exception e) {
            logger.warning("Read ACO representation file failed");
        }

        double[][] representation = new double[xCoordinates.size()][2];
        for (int index = 0; index < xCoordinates.size(); index += 1) {
            representation[index][0] = xCoordinates.get(index);
            representation[index][1] = yCoordinates.get(index);

        }

        return representation;

    }
}
