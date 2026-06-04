package com.university.erp.gui.legacy;

import com.university.ai.AIRecommendationEngine;
import com.university.models.Student;
import com.university.students.StudentManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AIRiskPanel extends JPanel {
    private JTable riskTable;
    private DefaultTableModel tableModel;
    private StudentManager studentManager = new StudentManager();
    private AIRecommendationEngine aiEngine = new AIRecommendationEngine();

    public AIRiskPanel() {
        setLayout(new BorderLayout());

        JLabel title = new JLabel("AI: Students at Risk", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        String[] columnNames = {"ID", "Name", "CGPA", "Attendance", "Placement Likelihood", "Readiness Score"};
        tableModel = new DefaultTableModel(columnNames, 0);
        riskTable = new JTable(tableModel);
        
        refreshAnalysis();

        JScrollPane scrollPane = new JScrollPane(riskTable);
        add(scrollPane, BorderLayout.CENTER);

        JButton analyzeButton = new JButton("Re-Analyze");
        analyzeButton.addActionListener(e -> refreshAnalysis());
        add(analyzeButton, BorderLayout.SOUTH);
    }

    private void refreshAnalysis() {
        tableModel.setRowCount(0);
        List<Student> students = new java.util.ArrayList<>(studentManager.getStudentMap().values());
        for (Student s : students) {
            boolean likelihood = aiEngine.predictPlacement(s);
            String likelihoodStr = likelihood ? "High" : "Low / At Risk";
            double readiness = aiEngine.calculatePlacementReadiness(s);
            tableModel.addRow(new Object[]{s.getId(), s.getName(), s.getCgpa(), s.getAttendancePercentage() + "%", likelihoodStr, readiness + "%"});
        }
    }
}
