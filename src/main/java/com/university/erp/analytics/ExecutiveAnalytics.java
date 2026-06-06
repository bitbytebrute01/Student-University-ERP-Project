package com.university.erp.analytics;

import com.university.db.DatabaseManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import javax.swing.*;
import java.sql.*;

public class ExecutiveAnalytics {

    public static JPanel createEnrollmentChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String sql = "SELECT department, COUNT(*) as count FROM students GROUP BY department";
        try (Connection conn = DatabaseManager.getConnection()) {
            // Defensive logging: list students with NULL or blank department for data hygiene
            try (Statement hygieneStmt = conn.createStatement();
                 ResultSet hygieneRs = hygieneStmt.executeQuery("SELECT id, name FROM students WHERE department IS NULL OR TRIM(department) = ''")) {
                boolean any = false;
                while (hygieneRs.next()) {
                    if (!any) {
                        System.out.println("ExecutiveAnalytics: Found students with NULL/blank department:");
                        any = true;
                    }
                    System.out.println("  - id=" + hygieneRs.getString("id") + ", name=" + hygieneRs.getString("name"));
                }
                if (any) {
                    System.out.println("ExecutiveAnalytics: Please clean up student.department values to ensure charts render correctly.");
                }
            } catch (SQLException ignored) {
                // non-fatal; continue to generate chart
            }

            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    String dept = rs.getString("department");
                    if (dept == null || dept.isBlank()) dept = "Unknown";
                    dataset.addValue(rs.getInt("count"), "Students", dept);
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }

        JFreeChart chart = ChartFactory.createBarChart("Student Enrollment", "Dept", "Total", dataset);
        return new ChartPanel(chart);
    }

    public static JPanel createPlacementStats() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        String sql = "SELECT fee_status, COUNT(*) FROM students GROUP BY fee_status";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                dataset.setValue(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        return new ChartPanel(ChartFactory.createPieChart("Fee Status Distribution", dataset, true, true, false));
    }

    public static int getCount(String table) {
        String sql = "SELECT COUNT(*) FROM " + table;
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public static double getAverageAttendance() {
        String sql = "SELECT AVG(attendance) FROM students";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0.0;
    }

    public static java.util.List<String> getRecentActivity(int limit) {
        java.util.List<String> list = new java.util.ArrayList<>();
        String sql = "SELECT action, timestamp FROM audit_logs ORDER BY timestamp DESC LIMIT ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(rs.getString("action") + " @ " + rs.getString("timestamp"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}
