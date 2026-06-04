package com.university.erp.gui.legacy;

import com.university.erp.security.SessionManager;
import com.university.erp.security.StudentContext;
import com.university.erp.security.User;
import com.university.models.Student;
import com.university.students.StudentManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class StudentProfilePanel extends JPanel {
    private StudentManager studentManager = new StudentManager();
    private Student student;

    public StudentProfilePanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(236, 240, 241));

        User user = SessionManager.getCurrentUser();
        student = findStudentForUser(user);

        if (student == null) {
            add(new JLabel("Student profile not found.", JLabel.CENTER));
            return;
        }

        JLabel title = new JLabel("Digital Student Identity", JLabel.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(new Color(44, 62, 80));
        title.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(title, BorderLayout.NORTH);

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 14));

        tabbedPane.addTab("Personal Info", createPersonalInfoTab());
        tabbedPane.addTab("Academic Records", createAcademicTab());
        tabbedPane.addTab("Professional Skills", createProfessionalTab());
        tabbedPane.addTab("Documents & ID", createDocumentsTab());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel createPersonalInfoTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        addInfoRow(panel, gbc, 0, "Full Name:", student.getName());
        addInfoRow(panel, gbc, 1, "Email Address:", student.getEmail());
        addInfoRow(panel, gbc, 2, "Phone Number:", student.getPhone());
        addInfoRow(panel, gbc, 3, "Bio:", student.getBio());
        addInfoRow(panel, gbc, 4, "LinkedIn:", student.getLinkedInUrl() == null ? "Not Linked" : student.getLinkedInUrl());

        return panel;
    }

    private JPanel createAcademicTab() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(Color.WHITE);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        addInfoRow(panel, gbc, 0, "University ID:", student.getId());
        addInfoRow(panel, gbc, 1, "Department:", student.getDepartment());
        addInfoRow(panel, gbc, 2, "Current Semester:", String.valueOf(student.getSemester()));
        addInfoRow(panel, gbc, 3, "Current CGPA:", String.format("%.2f", student.getCgpa()));
        addInfoRow(panel, gbc, 4, "Attendance Rate:", student.getAttendancePercentage() + "%");

        return panel;
    }

    private JPanel createProfessionalTab() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        DefaultListModel<String> skillModel = new DefaultListModel<>();
        for (String s : student.getSkills()) skillModel.addElement(s);
        JList<String> skillList = new JList<>(skillModel);
        skillList.setBorder(BorderFactory.createTitledBorder("Key Skills"));

        DefaultListModel<String> projectModel = new DefaultListModel<>();
        for (String p : student.getProjects()) projectModel.addElement(p);
        JList<String> projectList = new JList<>(projectModel);
        projectList.setBorder(BorderFactory.createTitledBorder("Major Projects"));

        JPanel listsPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        listsPanel.add(new JScrollPane(skillList));
        listsPanel.add(new JScrollPane(projectList));

        panel.add(listsPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel();
        JButton addSkillBtn = new JButton("Add Skill");
        JButton addProjectBtn = new JButton("Add Project");
        
        addSkillBtn.addActionListener(e -> {
            String skill = JOptionPane.showInputDialog(this, "Enter Skill:");
            if (skill != null && !skill.isEmpty()) {
                student.getSkills().add(skill);
                skillModel.addElement(skill);
                saveStudent();
            }
        });

        addProjectBtn.addActionListener(e -> {
            String proj = JOptionPane.showInputDialog(this, "Enter Project Name:");
            if (proj != null && !proj.isEmpty()) {
                student.getProjects().add(proj);
                projectModel.addElement(proj);
                saveStudent();
            }
        });

        btnPanel.add(addSkillBtn);
        btnPanel.add(addProjectBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createDocumentsTab() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 50));
        panel.setBackground(Color.WHITE);

        JButton viewIdBtn = new JButton("View Digital ID Card");
        viewIdBtn.setPreferredSize(new Dimension(200, 100));
        viewIdBtn.setBackground(new Color(52, 152, 219));
        viewIdBtn.setForeground(Color.WHITE);
        viewIdBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        viewIdBtn.addActionListener(e -> DigitalIDCardPanel.showIDCard(student));

        JButton genResumeBtn = new JButton("Generate Resume");
        genResumeBtn.setPreferredSize(new Dimension(200, 100));
        genResumeBtn.setBackground(new Color(46, 204, 113));
        genResumeBtn.setForeground(Color.WHITE);
        genResumeBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        genResumeBtn.addActionListener(e -> generateResume());

        panel.add(viewIdBtn);
        panel.add(genResumeBtn);

        return panel;
    }

    private void addInfoRow(JPanel panel, GridBagConstraints gbc, int row, String label, String value) {
        gbc.gridy = row;
        gbc.gridx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 16));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.PLAIN, 16));
        panel.add(val, gbc);
    }

    private void saveStudent() {
        try {
            studentManager.updateStudent(student);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error saving profile: " + e.getMessage());
        }
    }

    private void generateResume() {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================\n");
        sb.append("                      ").append(student.getName().toUpperCase()).append("\n");
        sb.append("========================================================================\n");
        sb.append("Email: ").append(student.getEmail()).append(" | Phone: ").append(student.getPhone()).append("\n");
        sb.append("LinkedIn: ").append(student.getLinkedInUrl()).append(" | GitHub: ").append(student.getGithubUrl()).append("\n");
        sb.append("------------------------------------------------------------------------\n\n");
        
        sb.append("PROFESSIONAL SUMMARY\n");
        sb.append("--------------------\n");
        sb.append(student.getBio()).append("\n\n");

        sb.append("ACADEMIC BACKGROUND\n");
        sb.append("-------------------\n");
        sb.append("University: Smart AI University\n");
        sb.append("Degree: Bachelor of Technology in ").append(student.getDepartment()).append("\n");
        sb.append("Semester: ").append(student.getSemester()).append(" | CGPA: ").append(String.format("%.2f", student.getCgpa())).append("\n\n");

        sb.append("TECHNICAL SKILLS\n");
        sb.append("----------------\n");
        sb.append(String.join(" • ", student.getSkills())).append("\n\n");

        if (!student.getWorkExperience().isEmpty()) {
            sb.append("PROFESSIONAL EXPERIENCE\n");
            sb.append("-----------------------\n");
            for (String exp : student.getWorkExperience()) sb.append("• ").append(exp).append("\n");
            sb.append("\n");
        }

        if (!student.getProjects().isEmpty()) {
            sb.append("KEY PROJECTS\n");
            sb.append("------------\n");
            for (String p : student.getProjects()) sb.append("• ").append(p).append("\n");
            sb.append("\n");
        }

        if (!student.getResearchPapers().isEmpty()) {
            sb.append("RESEARCH & PUBLICATIONS\n");
            sb.append("-----------------------\n");
            for (String paper : student.getResearchPapers()) sb.append("• ").append(paper).append("\n");
            sb.append("\n");
        }

        sb.append("LANGUAGES\n");
        sb.append("---------\n");
        sb.append(String.join(", ", student.getLanguages())).append("\n\n");

        sb.append("========================================================================\n");
        
        JTextArea area = new JTextArea(30, 60);
        area.setText(sb.toString());
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Professional ATS Resume", JOptionPane.INFORMATION_MESSAGE);
    }

    private Student findStudentForUser(User user) {
        if (user == null) return null;
        try {
            return StudentContext.requireStudentForUser(user, studentManager);
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
