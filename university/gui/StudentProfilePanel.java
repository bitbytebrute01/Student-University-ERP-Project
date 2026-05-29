package com.university.gui;

import com.university.authentication.SessionManager;
import com.university.authentication.User;
import com.university.models.Student;
import com.university.students.StudentManager;

import javax.swing.*;
import java.awt.*;

public class StudentProfilePanel extends JPanel {
    private StudentManager studentManager = new StudentManager();

    public StudentProfilePanel() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        User user = SessionManager.getCurrentUser();
        // We need to find the Student model corresponding to this User
        // For simplicity, let's assume username is the student name or ID
        // In the seed data, student S101 is Alice, and user 'alice' is created.
        // Let's search by name (case insensitive) or ID if they match.
        Student student = findStudentForUser(user);

        if (student == null) {
            add(new JLabel("Student profile not found.", JLabel.CENTER));
            return;
        }

        JPanel infoPanel = new JPanel(new GridLayout(0, 2, 20, 20));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));
        infoPanel.setBackground(Color.WHITE);

        addInfo(infoPanel, "ID:", student.getId());
        addInfo(infoPanel, "Name:", student.getName());
        addInfo(infoPanel, "Email:", student.getEmail());
        addInfo(infoPanel, "Department:", student.getDepartment());
        addInfo(infoPanel, "Semester:", String.valueOf(student.getSemester()));
        addInfo(infoPanel, "CGPA:", String.format("%.2f", student.getCgpa()));
        addInfo(infoPanel, "Attendance:", student.getAttendancePercentage() + "%");
        addInfo(infoPanel, "Fee Status:", student.getFeeStatus());

        JLabel title = new JLabel("My Profile", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 0, 0));
        
        add(title, BorderLayout.NORTH);
        add(infoPanel, BorderLayout.CENTER);
    }

    private void addInfo(JPanel panel, String label, String value) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Arial", Font.BOLD, 16));
        JLabel val = new JLabel(value);
        val.setFont(new Font("Arial", Font.PLAIN, 16));
        panel.add(lbl);
        panel.add(val);
    }

    private Student findStudentForUser(User user) {
        // Try to match by name or ID
        for (Student s : studentManager.getStudentMap().values()) {
            if (s.getName().equalsIgnoreCase(user.getUsername()) || s.getId().equalsIgnoreCase(user.getUsername())) {
                return s;
            }
        }
        // Fallback for seed data 'alice' -> 'Alice'
        if ("alice".equals(user.getUsername())) {
            return studentManager.searchById("S101");
        }
        return null;
    }
}
