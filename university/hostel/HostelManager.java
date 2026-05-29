package com.university.hostel;

import com.university.models.Room;
import com.university.utils.FileHandler;

import java.util.HashMap;

public class HostelManager {
    private HashMap<String, Room> roomMap;
    private static final String HOSTEL_FILE = "hostel.dat";

    public HostelManager() {
        loadHostel();
    }

    @SuppressWarnings("unchecked")
    private void loadHostel() {
        Object data = FileHandler.loadFromFile(HOSTEL_FILE);
        if (data instanceof HashMap) {
            roomMap = (HashMap<String, Room>) data;
        } else {
            roomMap = new HashMap<>();
        }
    }

    private void saveHostel() {
        FileHandler.saveToFile(HOSTEL_FILE, roomMap);
    }

    public void addRoom(Room room) {
        roomMap.put(room.getStorageKey(), room);
        saveHostel();
    }

    public boolean allocateRoom(String hostelName, String roomNumber) {
        Room r = roomMap.get(hostelName + "-" + roomNumber);
        if (r != null && r.allocate()) {
            saveHostel();
            return true;
        }
        return false;
    }

    public void vacateRoom(String hostelName, String roomNumber) {
        Room r = roomMap.get(hostelName + "-" + roomNumber);
        if (r != null) {
            r.vacate();
            saveHostel();
        }
    }

    public void printHostelReports() {
        System.out.println("Hostel Occupancy Report:");
        for (Room r : roomMap.values()) {
            System.out.println("Hostel: " + r.getHostelName() + " | Room: " + r.getRoomNumber() + " | Occupancy: " + r.getOccupied() + "/" + r.getCapacity());
        }
    }
}
