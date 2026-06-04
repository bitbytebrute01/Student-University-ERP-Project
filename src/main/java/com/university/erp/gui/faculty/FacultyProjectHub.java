package com.university.erp.gui.faculty;

import com.university.erp.dao.ProjectDAO;
import com.university.models.Project;
import com.university.erp.gui.theme.ThemeManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.SQLException;
import java.util.List;

public class FacultyProjectHub extends JPanel {
    private ProjectDAO projectDAO = new ProjectDAO();
    private JTable projectTable;
    private DefaultTableModel tableModel;

    public FacultyProjectHub() {
        setLayout(new MigLayout("ins 30, wrap 1, fillx", "[grow]", "[]20[grow]20[]"));
        ThemeManager.stylePage(this);

        JLabel title = new JLabel("Project Innovation Review Hub");
        title.setFont(new Font("Inter", Font.BOLD, 24));
        add(title);

        String[] cols = {"Project ID", "Student ID", "Title", "Status"};
        tableModel = new DefaultTableModel(cols, 0);
        projectTable = new JTable(tableModel);
        refreshTable();
        add(new JScrollPane(projectTable), "grow");

        JButton reviewBtn = new JButton("Review Selected Project");
        reviewBtn.setBackground(ThemeManager.SUCCESS_GREEN);
        reviewBtn.setForeground(Color.WHITE);
        reviewBtn.addActionListener(e -> showReviewDialog());
        add(reviewBtn, "h 45!");
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        // This is a simple implementation, in a real app we'd fetch projects for faculty's courses
        // For now, let's assume faculty can see all pending projects
        String sql = "SELECT * FROM student_projects";
        try (java.sql.Connection conn = com.university.db.DatabaseManager.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                tableModel.addRow(new Object[]{rs.getString("id"), rs.getString("student_id"), rs.getString("title"), rs.getString("status")});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void showReviewDialog() {
        int row = projectTable.getSelectedRow();
        if (row == -1) return;
        String projectId = (String) tableModel.getValueAt(row, 0);

        String[] statuses = {"Pending", "Reviewed", "Approved"};
        JComboBox<String> statusCombo = new JComboBox<>(statuses);
        JTextArea feedbackArea = new JTextArea(5, 20);

        Object[] message = {
            "Update Status:", statusCombo,
            "Faculty Feedback:", new JScrollPane(feedbackArea)
        };

        int option = JOptionPane.showConfirmDialog(null, message, "Project Review", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                projectDAO.updateProjectStatus(projectId, (String)statusCombo.getSelectedItem(), feedbackArea.getText());
                JOptionPane.showMessageDialog(this, "Review submitted!");
                refreshTable();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }
}
