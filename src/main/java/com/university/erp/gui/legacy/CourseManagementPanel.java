package com.university.erp.gui.legacy;

import com.university.courses.CourseManager;
import com.university.models.Course;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CourseManagementPanel extends JPanel {
    private JTable courseTable;
    private DefaultTableModel tableModel;
    private CourseManager courseManager = new CourseManager();

    public CourseManagementPanel() {
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Course Management", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        String[] columnNames = {"Course ID", "Course Name", "Credits", "Faculty ID"};
        tableModel = new DefaultTableModel(columnNames, 0);
        courseTable = new JTable(tableModel);
        refreshTable();

        JScrollPane scrollPane = new JScrollPane(courseTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Course");
        JButton assignBtn = new JButton("Assign Faculty");
        JButton refreshBtn = new JButton("Refresh");

        buttonPanel.add(addButton);
        buttonPanel.add(assignBtn);
        buttonPanel.add(refreshBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> showAddCourseDialog());
        assignBtn.addActionListener(e -> showAssignFacultyDialog());
        refreshBtn.addActionListener(e -> refreshTable());
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Course c : courseManager.getCoursesAlphabetically()) {
            tableModel.addRow(new Object[]{c.getCourseId(), c.getCourseName(), c.getCredits(), c.getAssignedFacultyId()});
        }
    }

    private void showAddCourseDialog() {
        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField creditsField = new JTextField();

        Object[] message = {
            "Course ID:", idField,
            "Course Name:", nameField,
            "Credits:", creditsField
        };

        int option = JOptionPane.showConfirmDialog(null, message, "Add Course", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                int credits = Integer.parseInt(creditsField.getText());
                Course c = new Course(idField.getText(), nameField.getText(), credits);
                courseManager.addCourse(c);
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error adding course: " + ex.getMessage());
            }
        }
    }

    private void showAssignFacultyDialog() {
        int selectedRow = courseTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a course.");
            return;
        }
        String courseId = (String) tableModel.getValueAt(selectedRow, 0);
        String facultyId = JOptionPane.showInputDialog(this, "Enter Faculty ID:");
        if (facultyId != null && !facultyId.isEmpty()) {
            try {
                courseManager.assignFacultyToCourse(courseId, facultyId);
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }
}
