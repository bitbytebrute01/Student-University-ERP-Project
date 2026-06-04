package com.university.erp.gui.student;

import com.university.lms.AssignmentManager;
import com.university.courses.CourseManager;
import com.university.erp.security.StudentContext;
import com.university.models.Assignment;
import com.university.models.Submission;
import com.university.models.Course;
import com.university.models.Student;
import com.university.utils.MediaManager;
import com.university.erp.gui.theme.ThemeManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

public class AssignmentHub extends JPanel {
    private AssignmentManager assignmentManager = new AssignmentManager();
    private CourseManager courseManager = new CourseManager();
    private JTable assignmentTable;
    private DefaultTableModel tableModel;
    private String studentId;

    public AssignmentHub() {
        this(StudentContext.requireCurrentStudent());
    }

    public AssignmentHub(Student student) {
        if (student == null) {
            throw new IllegalStateException("AssignmentHub requires a valid Student.");
        }
        studentId = student.getId();
        setLayout(new MigLayout("ins 30, wrap 1, fillx", "[grow]", "[]20[grow]20[]"));
        ThemeManager.stylePage(this);

        JLabel title = new JLabel("LMS: Enrolled Course Assignments");
        title.setFont(new Font("Inter", Font.BOLD, 24));
        add(title);

        String[] cols = {"ID", "Course", "Title", "Deadline", "Countdown", "Status", "Grade", "Feedback"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        assignmentTable = new JTable(tableModel);
        assignmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        refreshTable();
        add(new JScrollPane(assignmentTable), "grow");

        JButton submitBtn = new JButton("Submit Assignment");
        submitBtn.setBackground(ThemeManager.ACCENT_BLUE);
        submitBtn.setForeground(Color.WHITE);
        submitBtn.addActionListener(e -> showSubmitDialog());
        add(submitBtn, "h 40!");
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        SimpleDateFormat deadlineFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        List<Assignment> assignments = assignmentManager.getAssignmentsForStudent(studentId);
        for (Assignment a : assignments) {
            Course c = courseManager.searchById(a.getCourseId());
            Submission s = assignmentManager.getStudentSubmission(studentId, a.getId());
            String status = assignmentManager.getStudentAssignmentStatus(studentId, a);
            String grade = (s != null && s.isGraded()) ? String.valueOf(s.getMarks()) : "-";
            String feedback = (s != null && s.getFeedback() != null && !s.getFeedback().isBlank()) ? s.getFeedback() : "-";
            String deadline = a.getDeadline() == null ? "-" : deadlineFormat.format(a.getDeadline());
            String courseName = c == null ? a.getCourseId() : c.getCourseName();
            tableModel.addRow(new Object[]{
                    a.getId(),
                    courseName,
                    a.getTitle(),
                    deadline,
                    assignmentManager.getDeadlineCountdown(a),
                    status,
                    grade,
                    feedback
            });
        }
    }

    private void showSubmitDialog() {
        int row = assignmentTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an assignment from the list.");
            return;
        }
        String assignmentId = (String) tableModel.getValueAt(row, 0);
        String status = (String) tableModel.getValueAt(row, 5);
        
        if ("Graded".equals(status)) {
            JOptionPane.showMessageDialog(this, "This assignment has already been graded and cannot be resubmitted.");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Assignment files (*.pdf, *.docx, *.zip)", "pdf", "docx", "zip"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = chooser.getSelectedFile();
                String savedPath = MediaManager.saveSubmissionFile(file, "submissions/" + assignmentId + "/" + studentId);
                
                Submission s = new Submission("SUB-" + UUID.randomUUID().toString().substring(0, 8), assignmentId, studentId, savedPath);
                assignmentManager.submitAssignment(s);
                
                JOptionPane.showMessageDialog(this, "Assignment submitted successfully. Status: " + s.getStatus());
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Submission Error: " + ex.getMessage());
            }
        }
    }
}
