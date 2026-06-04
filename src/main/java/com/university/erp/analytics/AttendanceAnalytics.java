package com.university.erp.analytics;

import com.university.db.DatabaseManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendanceAnalytics {

    public static JPanel createOverallAttendanceChart() {
        DefaultPieDataset dataset = new DefaultPieDataset();
        String sql = "SELECT is_present, COUNT(*) FROM attendance GROUP BY is_present";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String label = rs.getBoolean(1) ? "Present" : "Absent";
                dataset.setValue(label, rs.getInt(2));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        return new ChartPanel(ChartFactory.createPieChart("Overall Attendance Distribution", dataset, true, true, false));
    }

    public static JPanel createDeptAttendanceChart() {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        String sql = "SELECT s.department, " +
                     "CAST(SUM(CASE WHEN a.is_present THEN 1 ELSE 0 END) AS REAL) / COUNT(*) * 100 as rate " +
                     "FROM attendance a JOIN students s ON a.student_id = s.id GROUP BY s.department";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                dataset.addValue(rs.getDouble("rate"), "Attendance %", rs.getString("department"));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        return new ChartPanel(ChartFactory.createBarChart("Attendance Rate by Department", "Department", "Percentage (%)", dataset));
    }

    public static List<Object[]> getLowAttendanceStudents() {
        List<Object[]> list = new ArrayList<>();
        String sql = "SELECT s.id, s.name, s.department, s.attendance " +
                     "FROM students s WHERE s.attendance < 75.0 ORDER BY s.attendance ASC LIMIT 10";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Object[]{rs.getString(1), rs.getString(2), rs.getString(3), rs.getDouble(4)});
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }
}
