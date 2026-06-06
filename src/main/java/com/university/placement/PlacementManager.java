package com.university.placement;

import com.university.db.DatabaseManager;
import com.university.models.Company;
import com.university.models.Student;
import com.university.students.StudentManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlacementManager {

    public PlacementManager() {}

    public void registerCompany(Company company) {
        String sql = "INSERT INTO companies (company_id, name, required_cgpa, job_role) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, company.getCompanyId());
            pstmt.setString(2, company.getName());
            pstmt.setDouble(3, company.getRequiredCgpa());
            pstmt.setString(4, company.getJobRole());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Error registering company: " + e.getMessage(), e);
        }
    }

    public List<Student> getEligibleStudentsForCompany(String companyId, StudentManager studentManager) {
        List<Student> eligible = new ArrayList<>();
        String sql = "SELECT * FROM students WHERE cgpa >= (SELECT required_cgpa FROM companies WHERE company_id = ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, companyId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                eligible.add(studentManager.searchById(rs.getString("id")));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error getting eligible students: " + e.getMessage(), e);
        }
        return eligible;
    }

    public void printPlacementStatistics() {
        String sql = "SELECT * FROM companies";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            System.out.println("Placement Statistics:");
            while (rs.next()) {
                System.out.println(rs.getString("name") + " - Role: " + rs.getString("job_role") + " - Min CGPA: " + rs.getDouble("required_cgpa"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error printing placement stats: " + e.getMessage(), e);
        }
    }
}
