package com.university.models;

import com.university.interfaces.Storable;

public class Room implements Storable {
    private String roomNumber;
    private String hostelName;
    private int capacity;
    private int occupied;

    public Room(String roomNumber, String hostelName, int capacity) {
        this.roomNumber = roomNumber;
        this.hostelName = hostelName;
        this.capacity = capacity;
        this.occupied = 0;
    }

    public String getRoomNumber() { return roomNumber; }
    public String getHostelName() { return hostelName; }
    public int getCapacity() { return capacity; }
    public int getOccupied() { return occupied; }

    public boolean allocate() {
        if (occupied < capacity) {
            occupied++;
            return true;
        }
        return false;
    }

    public void vacate() {
        if (occupied > 0) occupied--;
    }

    @Override
    public String getStorageKey() {
        return hostelName + "-" + roomNumber;
    }
}
