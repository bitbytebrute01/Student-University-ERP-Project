package com.university.faculty;

import com.university.db.DatabaseManager;
import com.university.interfaces.Searchable;
import com.university.models.Faculty;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FacultyManager implements Searchable<Faculty> {

    public FacultyManager() {}

    public void addFaculty(Faculty faculty) {
        String sql = "INSERT INTO faculty (id, name, email, phone, department, designation, experience) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, faculty.getId());
            pstmt.setString(2, faculty.getName());
            pstmt.setString(3, faculty.getEmail());
            pstmt.setString(4, faculty.getPhone());
            pstmt.setString(5, faculty.getDepartment());
            pstmt.setString(6, faculty.getDesignation());
            pstmt.setInt(7, faculty.getYearsOfExperience());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding faculty: " + e.getMessage());
        }
    }

    public void removeFaculty(String facultyId) {
        String sql = "DELETE FROM faculty WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, facultyId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error removing faculty: " + e.getMessage());
        }
    }

    @Override
    public Faculty searchById(String id) {
        String sql = "SELECT * FROM faculty WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return mapResultSetToFaculty(rs);
        } catch (SQLException e) {
            System.err.println("Error searching faculty: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Faculty> searchByName(String name) {
        List<Faculty> list = new ArrayList<>();
        String sql = "SELECT * FROM faculty WHERE name LIKE ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + name + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapResultSetToFaculty(rs));
        } catch (SQLException e) {
            System.err.println("Error searching faculty by name: " + e.getMessage());
        }
        return list;
    }

    public void displayAllFaculty() {
        String sql = "SELECT * FROM faculty";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                mapResultSetToFaculty(rs).printDetails();
                System.out.println("-----------------");
            }
        } catch (SQLException e) {
            System.err.println("Error displaying faculty: " + e.getMessage());
        }
    }

    public List<Faculty> getFacultySortedByExperience() {
        List<Faculty> list = new ArrayList<>();
        String sql = "SELECT * FROM faculty ORDER BY experience DESC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSetToFaculty(rs));
        } catch (SQLException e) {
            System.err.println("Error getting sorted faculty: " + e.getMessage());
        }
        return list;
    }

    private Faculty mapResultSetToFaculty(ResultSet rs) throws SQLException {
        return new Faculty(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("department"),
                rs.getString("designation"),
                rs.getInt("experience")
        );
    }
}
