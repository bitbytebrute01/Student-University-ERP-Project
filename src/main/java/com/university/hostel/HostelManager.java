package com.university.hostel;

import com.university.db.DatabaseManager;
import com.university.models.Room;

import java.sql.*;
import java.util.ArrayList;

public class HostelManager {

    public HostelManager() {}

    public void addRoom(Room room) {
        String sql = "INSERT INTO rooms (room_key, room_number, hostel_name, capacity, occupied) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, room.getStorageKey());
            pstmt.setString(2, room.getRoomNumber());
            pstmt.setString(3, room.getHostelName());
            pstmt.setInt(4, room.getCapacity());
            pstmt.setInt(5, room.getOccupied());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding room: " + e.getMessage());
        }
    }

    public boolean allocateRoom(String hostelName, String roomNumber) {
        String key = hostelName + "-" + roomNumber;
        String sql = "UPDATE rooms SET occupied = occupied + 1 WHERE room_key = ? AND occupied < capacity";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error allocating room: " + e.getMessage());
        }
        return false;
    }

    public void vacateRoom(String hostelName, String roomNumber) {
        String key = hostelName + "-" + roomNumber;
        String sql = "UPDATE rooms SET occupied = occupied - 1 WHERE room_key = ? AND occupied > 0";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, key);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error vacating room: " + e.getMessage());
        }
    }

    public void printHostelReports() {
        System.out.println("Hostel Occupancy Report:");
        String sql = "SELECT * FROM rooms";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                System.out.println("Hostel: " + rs.getString("hostel_name") + 
                                   " | Room: " + rs.getString("room_number") + 
                                   " | Occupancy: " + rs.getInt("occupied") + 
                                   "/" + rs.getInt("capacity"));
            }
        } catch (SQLException e) {
            System.err.println("Error printing hostel reports: " + e.getMessage());
        }
    }
}
