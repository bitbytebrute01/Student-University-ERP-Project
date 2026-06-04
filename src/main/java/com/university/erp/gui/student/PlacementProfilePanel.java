package com.university.erp.gui.student;

import com.university.models.Student;
import com.university.students.StudentManager;
import com.university.erp.security.StudentContext;
import com.university.utils.MediaManager;
import com.university.erp.gui.theme.ThemeManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;

public class PlacementProfilePanel extends JPanel {
    private StudentManager studentManager = new StudentManager();
    private Student student;

    public PlacementProfilePanel() {
        this(StudentContext.requireCurrentStudent());
    }

    public PlacementProfilePanel(Student student) {
        this.student = student;
        if (this.student == null) {
            throw new IllegalStateException("PlacementProfilePanel requires a valid Student.");
        }

        setLayout(new MigLayout("ins 30, wrap 1, fillx", "[grow]", "[]20[]"));
        ThemeManager.stylePage(this);

        JLabel title = new JLabel("Career & Placement Profile");
        title.setFont(new Font("Inter", Font.BOLD, 24));
        add(title);

        JPanel card = ThemeManager.createGlassCard();
        card.setLayout(new MigLayout("ins 20, wrap 2, fillx", "[grow 30][grow 70]", "[]20[]20[]20[]"));
        
        JTextField githubField = new JTextField(student.getGithubUrl());
        JTextField linkedinField = new JTextField(student.getLinkedInUrl());
        JTextField portfolioField = new JTextField(student.getPortfolioUrl());
        
        JButton uploadResumeBtn = new JButton("Upload New Resume (PDF)");
        JLabel resumeStatus = new JLabel(student.getResumePath() == null ? "No Resume Uploaded" : "Resume: " + student.getResumePath());

        card.add(new JLabel("GitHub URL:"));
        card.add(githubField, "growx");

        card.add(new JLabel("LinkedIn URL:"));
        card.add(linkedinField, "growx");

        card.add(new JLabel("Portfolio URL:"));
        card.add(portfolioField, "growx");

        card.add(new JLabel("Professional Resume:"));
        JPanel resumeBox = new JPanel(new FlowLayout(FlowLayout.LEFT));
        resumeBox.setOpaque(false);
        resumeBox.add(uploadResumeBtn);
        resumeBox.add(resumeStatus);
        card.add(resumeBox, "growx");

        JButton saveBtn = new JButton("Save Placement Profile");
        saveBtn.setBackground(ThemeManager.ACCENT_BLUE);
        saveBtn.setForeground(Color.WHITE);
        
        saveBtn.addActionListener(e -> {
            try {
                student.setGithubUrl(githubField.getText());
                student.setLinkedInUrl(linkedinField.getText());
                student.setPortfolioUrl(portfolioField.getText());
                studentManager.updateStudent(student);
                JOptionPane.showMessageDialog(this, "Placement profile updated!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Save Error: " + ex.getMessage());
            }
        });

        uploadResumeBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("PDF files (*.pdf)", "pdf"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    String path = MediaManager.savePdf(chooser.getSelectedFile(), "resumes");
                    student.setResumePath(path);
                    studentManager.updateStudent(student);
                    resumeStatus.setText("Resume: " + path);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Upload Error: " + ex.getMessage());
                }
            }
        });

        add(card, "growx");
        add(saveBtn, "h 45!, w 200!");
    }
}
