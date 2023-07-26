package org.fog.test.perfeval;

import org.fog.entities.FogDevice;
import org.fog.utils.SerializableFogDevices;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) throws IOException {
        List<FogDevice> fogDevices = new ArrayList<>();
        SerializableFogDevices obj = new SerializableFogDevices(fogDevices);
        // Create a FileOutputStream to write to a file (you can replace "data.ser" with your desired file name)
        FileOutputStream fileOut = new FileOutputStream("data.ser");
        // Create an ObjectOutputStream to serialize the object
        ObjectOutputStream out = new ObjectOutputStream(fileOut);

        // Write the object to the output stream
        out.writeObject(obj);

        // Close the streams
        out.close();
        fileOut.close();

        System.out.println("Object serialized successfully.");

    }
}
