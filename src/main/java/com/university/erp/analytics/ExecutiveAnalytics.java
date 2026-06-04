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
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                dataset.addValue(rs.getInt("count"), "Students", rs.getString("department"));
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
}
