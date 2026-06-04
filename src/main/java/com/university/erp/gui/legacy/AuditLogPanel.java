package com.university.erp.gui.legacy;

import com.university.db.DatabaseManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class AuditLogPanel extends JPanel {
    private JTable logTable;
    private DefaultTableModel tableModel;

    public AuditLogPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JLabel title = new JLabel("System Audit Logs", JLabel.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        String[] cols = {"ID", "User", "Action", "Timestamp"};
        tableModel = new DefaultTableModel(cols, 0);
        logTable = new JTable(tableModel);
        refreshLogs();

        add(new JScrollPane(logTable), BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh Logs");
        refreshBtn.addActionListener(e -> refreshLogs());
        add(refreshBtn, BorderLayout.SOUTH);
    }

    private void refreshLogs() {
        tableModel.setRowCount(0);
        String sql = "SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 100";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("action"),
                        rs.getString("timestamp")
                });
            }
        } catch (Exception e) {
            System.err.println("Error loading logs: " + e.getMessage());
        }
    }
}
