package com.university.erp.gui.faculty;

import com.university.courses.CourseManager;
import com.university.models.Course;
import com.university.erp.security.SessionManager;
import com.university.erp.security.User;
import com.university.erp.security.UserRole;
import com.university.erp.gui.theme.ThemeManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class CourseManagementPanel extends JPanel {
    private CourseManager courseManager = new CourseManager();
    private JTable courseTable;
    private DefaultTableModel tableModel;
    private String facultyId;
    private boolean adminMode;

    public CourseManagementPanel() {
        facultyId = resolveFacultyId();
        adminMode = isAdminUser();
        setLayout(new MigLayout("ins 30, wrap 1, fillx", "[grow]", "[]20[grow]20[]"));
        ThemeManager.stylePage(this);

        JLabel title = new JLabel("Academic Course Management");
        title.setFont(new Font("Inter", Font.BOLD, 24));
        add(title);

        String[] cols = {"ID", "Name", "Credits", "Students Enrolled"};
        tableModel = new DefaultTableModel(cols, 0);
        courseTable = new JTable(tableModel);
        refreshTable();
        add(new JScrollPane(courseTable), "grow");

        JPanel btnPanel = new JPanel(new MigLayout("ins 0, gap 15"));
        btnPanel.setOpaque(false);

        JButton addBtn = new JButton("Create New Course");
        addBtn.setBackground(ThemeManager.SUCCESS_GREEN);
        addBtn.setForeground(Color.WHITE);
        addBtn.addActionListener(e -> showAddCourseDialog());

        JButton enrollBtn = new JButton("Enroll Student");
        enrollBtn.addActionListener(e -> showEnrollDialog());

        JButton deleteBtn = new JButton("Delete Course");
        deleteBtn.addActionListener(e -> deleteSelectedCourse());

        btnPanel.add(addBtn, "h 45!");
        btnPanel.add(enrollBtn, "h 45!");
        btnPanel.add(deleteBtn, "h 45!");
        add(btnPanel);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<Course> courses = adminMode ? courseManager.getAllCourses() : courseManager.getCoursesByFaculty(facultyId);
        for (Course c : courses) {
            int studentCount = courseManager.getEnrolledStudents(c.getCourseId()).size();
            tableModel.addRow(new Object[]{c.getCourseId(), c.getCourseName(), c.getCredits(), studentCount});
        }
    }

    private void showAddCourseDialog() {
        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField creditsField = new JTextField();
        JTextField facultyField = new JTextField(adminMode ? "" : facultyId);
        facultyField.setEditable(adminMode);

        Object[] message = {
            "Course ID (Code):", idField,
            "Course Name:", nameField,
            "Credits:", creditsField,
            "Faculty ID:", facultyField
        };

        int option = JOptionPane.showConfirmDialog(null, message, "New Course", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                String courseId = idField.getText().trim();
                String courseName = nameField.getText().trim();
                String faculty = facultyField.getText().trim();
                if (courseId.isEmpty()) throw new IllegalArgumentException("Course ID is required.");
                if (courseName.isEmpty()) throw new IllegalArgumentException("Course name is required.");

                Course c = new Course(courseId, courseName, Integer.parseInt(creditsField.getText().trim()));
                c.setAssignedFacultyId(faculty.isEmpty() ? null : faculty);
                courseManager.addCourse(c);
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    private void showEnrollDialog() {
        int row = courseTable.getSelectedRow();
        if (row == -1) return;
        String courseId = (String) tableModel.getValueAt(row, 0);
        String studentId = JOptionPane.showInputDialog(this, "Enter Student ID to enroll:");
        if (studentId != null && !studentId.isEmpty()) {
            try {
                courseManager.enrollStudent(courseId, studentId);
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    private void deleteSelectedCourse() {
        int row = courseTable.getSelectedRow();
        if (row == -1) return;
        String courseId = (String) tableModel.getValueAt(row, 0);
        if (JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this course?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            courseManager.deleteCourse(courseId);
            refreshTable();
        }
    }

    private String resolveFacultyId() {
        User user = SessionManager.getCurrentUser();
        if (user != null && user.getRefId() != null && !user.getRefId().isBlank()) {
            return user.getRefId();
        }
        return user != null ? user.getUsername() : "";
    }

    private boolean isAdminUser() {
        User user = SessionManager.getCurrentUser();
        return user != null && user.getRole() == UserRole.ADMIN;
    }
}
