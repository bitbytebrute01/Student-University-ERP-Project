package com.university.erp.gui.legacy;

import com.university.attendance.AttendanceManager;
import com.university.models.Student;
import com.university.students.StudentManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class FacultyAttendancePanel extends JPanel {
    private JComboBox<String> studentCombo;
    private JTextField courseIdField;
    private JCheckBox presentCheck;
    private StudentManager studentManager = new StudentManager();
    private AttendanceManager attendanceManager = new AttendanceManager(studentManager);

    public FacultyAttendancePanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Mark Attendance", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        add(title, gbc);

        gbc.gridwidth = 1;
        gbc.gridy = 1; gbc.gridx = 0;
        add(new JLabel("Select Student:"), gbc);

        studentCombo = new JComboBox<>();
        List<Student> students = new java.util.ArrayList<>(studentManager.getStudentMap().values());
        for (Student s : students) {
            studentCombo.addItem(s.getId() + " - " + s.getName());
        }
        gbc.gridx = 1;
        add(studentCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        add(new JLabel("Course ID:"), gbc);

        courseIdField = new JTextField(10);
        gbc.gridx = 1;
        add(courseIdField, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        add(new JLabel("Is Present:"), gbc);

        presentCheck = new JCheckBox();
        gbc.gridx = 1;
        add(presentCheck, gbc);

        JButton markBtn = new JButton("Mark Attendance");
        markBtn.setBackground(new Color(46, 204, 113));
        markBtn.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        add(markBtn, gbc);

        markBtn.addActionListener(e -> {
            String selected = (String) studentCombo.getSelectedItem();
            if (selected == null) return;
            String studentId = selected.split(" - ")[0];
            String courseId = courseIdField.getText();
            boolean isPresent = presentCheck.isSelected();

            try {
                attendanceManager.markAttendance(studentId, courseId, isPresent);
                // notify UI
                com.university.erp.gui.UIEventBus.publish("ATTENDANCE_UPDATED", studentId);
                JOptionPane.showMessageDialog(this, "Attendance marked successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });
    }
}
