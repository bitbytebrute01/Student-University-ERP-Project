package com.university.erp.gui.legacy;

import com.university.examination.ExamManager;
import com.university.models.Student;
import com.university.students.StudentManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class FacultyMarksPanel extends JPanel {
    private JComboBox<String> studentCombo;
    private JTextField courseIdField;
    private JTextField marksField;
    private StudentManager studentManager = new StudentManager();
    private ExamManager examManager = new ExamManager(studentManager);

    public FacultyMarksPanel() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel title = new JLabel("Upload Marks", JLabel.CENTER);
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
        add(new JLabel("Marks (0-100):"), gbc);

        marksField = new JTextField(5);
        gbc.gridx = 1;
        add(marksField, gbc);

        JButton uploadBtn = new JButton("Upload Marks");
        uploadBtn.setBackground(new Color(52, 152, 219));
        uploadBtn.setForeground(Color.WHITE);
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        add(uploadBtn, gbc);

        uploadBtn.addActionListener(e -> {
            String selected = (String) studentCombo.getSelectedItem();
            if (selected == null) return;
            String studentId = selected.split(" - ")[0];
            String courseId = courseIdField.getText();
            try {
                double marks = Double.parseDouble(marksField.getText());
                examManager.uploadMarks(studentId, courseId, marks);
                JOptionPane.showMessageDialog(this, "Marks uploaded and CGPA updated!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        });
    }
}
