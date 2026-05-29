package com.university.utils;

import java.io.*;

public class FileHandler {

    public static void saveToFile(String filePath, Object data) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filePath))) {
            oos.writeObject(data);
        } catch (IOException e) {
            System.err.println("Error saving data to file " + filePath + ": " + e.getMessage());
        }
    }

    public static Object loadFromFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading data from file " + filePath + ": " + e.getMessage());
            return null;
        }
    }
}
