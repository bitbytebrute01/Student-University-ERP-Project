package com.university.erp.gui.student;

import com.university.erp.dao.ProjectDAO;
import com.university.erp.security.StudentContext;
import com.university.models.Project;
import com.university.models.Student;
import com.university.utils.MediaManager;
import com.university.erp.gui.theme.ThemeManager;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.util.UUID;

public class ProjectPanel extends JPanel {
    private ProjectDAO projectDAO = new ProjectDAO();
    private JTable projectTable;
    private DefaultTableModel tableModel;
    private String studentId;

    public ProjectPanel() {
        this(StudentContext.requireCurrentStudent());
    }

    public ProjectPanel(Student student) {
        if (student == null) {
            throw new IllegalStateException("ProjectPanel requires a valid Student.");
        }
        studentId = student.getId();
        
        setLayout(new MigLayout("ins 30, wrap 1, fillx", "[grow]", "[]20[grow]20[]"));
        ThemeManager.stylePage(this);

        JLabel title = new JLabel("Project Management Hub");
        title.setFont(new Font("Inter", Font.BOLD, 24));
        add(title);

        // Table
        String[] cols = {"ID", "Title", "Status", "Feedback"};
        tableModel = new DefaultTableModel(cols, 0);
        projectTable = new JTable(tableModel);
        refreshTable();
        add(new JScrollPane(projectTable), "grow");

        // Actions
        JPanel btnPanel = new JPanel(new MigLayout("ins 0, gap 15"));
        btnPanel.setOpaque(false);
        
        JButton addBtn = new JButton("Add Project");
        addBtn.setIcon(FontIcon.of(MaterialDesignP.PLUS, 18, Color.WHITE));
        addBtn.setBackground(ThemeManager.SUCCESS_GREEN);
        addBtn.setForeground(Color.WHITE);
        addBtn.addActionListener(e -> showAddProjectDialog());

        JButton deleteBtn = new JButton("Delete Selected");
        deleteBtn.addActionListener(e -> deleteSelectedProject());

        btnPanel.add(addBtn, "h 40!");
        btnPanel.add(deleteBtn, "h 40!");
        add(btnPanel);
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        try {
            for (Project p : projectDAO.getProjectsByStudent(studentId)) {
                tableModel.addRow(new Object[]{p.getId(), p.getTitle(), p.getStatus(), p.getFacultyFeedback()});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void showAddProjectDialog() {
        JTextField titleField = new JTextField();
        JTextArea descArea = new JTextArea(5, 20);
        
        JButton screenshotBtn = new JButton("Select Screenshot");
        JLabel screenshotPath = new JLabel("None");
        screenshotBtn.addActionListener(e -> selectFile(screenshotPath));

        JButton reportBtn = new JButton("Select Report (PDF)");
        JLabel reportPath = new JLabel("None");
        reportBtn.addActionListener(e -> selectFile(reportPath));

        JButton zipBtn = new JButton("Select Source (ZIP)");
        JLabel zipPath = new JLabel("None");
        zipBtn.addActionListener(e -> selectFile(zipPath));

        Object[] message = {
            "Project Title:", titleField,
            "Description:", new JScrollPane(descArea),
            "Screenshot:", screenshotBtn, screenshotPath,
            "Project Report:", reportBtn, reportPath,
            "Source Code:", zipBtn, zipPath
        };

        int option = JOptionPane.showConfirmDialog(null, message, "Add New Project", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                Project p = new Project(UUID.randomUUID().toString().substring(0, 8), studentId, titleField.getText(), descArea.getText());
                
                if (!screenshotPath.getText().equals("None")) 
                    p.setScreenshotPath(MediaManager.saveImage(new File(screenshotPath.getText()), "projects/screenshots"));
                if (!reportPath.getText().equals("None")) 
                    p.setReportPath(MediaManager.saveFile(new File(reportPath.getText()), "projects/reports"));
                if (!zipPath.getText().equals("None")) 
                    p.setZipPath(MediaManager.saveSubmissionFile(new File(zipPath.getText()), "projects/source"));

                projectDAO.addProject(p);
                JOptionPane.showMessageDialog(this, "Project added successfully!");
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    private void selectFile(JLabel pathLabel) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            pathLabel.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void deleteSelectedProject() {
        int row = projectTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select a project to delete.");
            return;
        }

        String projectId = (String) tableModel.getValueAt(row, 0);
        int option = JOptionPane.showConfirmDialog(this, "Delete selected project?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (option != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            projectDAO.deleteProject(projectId, studentId);
            refreshTable();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Delete failed: " + e.getMessage());
        }
    }
}
