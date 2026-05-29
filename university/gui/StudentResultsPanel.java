package com.university.gui;

import com.university.authentication.SessionManager;
import com.university.authentication.User;
import com.university.examination.ExamManager;
import com.university.models.Student;
import com.university.students.StudentManager;

import javax.swing.*;
import java.awt.*;

public class StudentResultsPanel extends JPanel {
    private StudentManager studentManager = new StudentManager();
    private ExamManager examManager = new ExamManager(studentManager);

    public StudentResultsPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 245));

        User user = SessionManager.getCurrentUser();
        Student student = findStudentForUser(user);

        if (student == null) {
            add(new JLabel("Results not found.", JLabel.CENTER));
            return;
        }

        JLabel title = new JLabel("Academic Performance", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 22));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        resultArea.setText(examManager.generateResult(student.getId()));
        
        JScrollPane scrollPane = new JScrollPane(resultArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 50, 50, 50));
        add(scrollPane, BorderLayout.CENTER);
    }

    private Student findStudentForUser(User user) {
        for (Student s : studentManager.getStudentMap().values()) {
            if (s.getName().equalsIgnoreCase(user.getUsername()) || s.getId().equalsIgnoreCase(user.getUsername())) {
                return s;
            }
        }
        if ("alice".equals(user.getUsername())) return studentManager.searchById("S101");
        return null;
    }
}
