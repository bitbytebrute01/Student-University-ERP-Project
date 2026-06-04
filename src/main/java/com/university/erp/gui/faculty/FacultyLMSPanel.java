package com.university.erp.gui.faculty;

import com.university.lms.AssignmentManager;
import com.university.courses.CourseManager;
import com.university.models.Assignment;
import com.university.models.Submission;
import com.university.models.Course;
import com.university.erp.security.SessionManager;
import com.university.erp.security.User;
import com.university.erp.security.UserRole;
import com.university.erp.gui.theme.ThemeManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class FacultyLMSPanel extends JPanel {
    private AssignmentManager assignmentManager = new AssignmentManager();
    private CourseManager courseManager = new CourseManager();
    private JTable assignmentTable;
    private DefaultTableModel tableModel;
    private String facultyId;
    private boolean adminMode;

    public FacultyLMSPanel() {
        facultyId = resolveFacultyId();
        adminMode = isAdminUser();
        setLayout(new MigLayout("ins 30, wrap 1, fillx", "[grow]", "[]20[grow]20[]"));
        ThemeManager.stylePage(this);

        JLabel title = new JLabel("Academic Assignment & Grading Portal");
        title.setFont(new Font("Inter", Font.BOLD, 24));
        add(title);

        String[] cols = {"ID", "Course", "Title", "Due Date", "Due Time", "Status", "Max Marks"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        assignmentTable = new JTable(tableModel);
        assignmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        refreshAssignments();
        add(new JScrollPane(assignmentTable), "grow");

        JPanel btnPanel = new JPanel(new MigLayout("ins 0, gap 15"));
        btnPanel.setOpaque(false);

        JButton createBtn = new JButton("Create New Assignment");
        createBtn.setBackground(ThemeManager.ACCENT_BLUE);
        createBtn.setForeground(Color.WHITE);
        createBtn.addActionListener(e -> showCreateDialog());

        JButton viewSubBtn = new JButton("Grade Submissions");
        viewSubBtn.addActionListener(e -> showSubmissionsDialog());

        btnPanel.add(createBtn, "h 45!");
        btnPanel.add(viewSubBtn, "h 45!");
        add(btnPanel);
    }

    private void refreshAssignments() {
        tableModel.setRowCount(0);
        List<Course> courses = getManagedCourses();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (Course c : courses) {
            List<Assignment> assignments = assignmentManager.getAssignmentsByCourse(c.getCourseId());
            for (Assignment a : assignments) {
                String dueDate = a.getDeadline() == null ? "-" : dateFormat.format(a.getDeadline());
                tableModel.addRow(new Object[]{a.getId(), c.getCourseName(), a.getTitle(), dueDate, a.getDueTime(), a.getStatus(), a.getMaxMarks()});
            }
        }
    }

    private void showCreateDialog() {
        List<Course> courses = getManagedCourses();
        if (courses.isEmpty()) {
            JOptionPane.showMessageDialog(this, "You have no active courses. Please create a course first.");
            return;
        }

        JComboBox<Course> courseCombo = new JComboBox<>(courses.toArray(new Course[0]));
        courseCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Course) {
                    setText(((Course) value).getCourseName() + " (" + ((Course) value).getCourseId() + ")");
                }
                return this;
            }
        });

        JTextField titleField = new JTextField();
        JTextArea descArea = new JTextArea(5, 20);
        JTextField marksField = new JTextField("100");
        JTextField dueDateField = new JTextField(LocalDate.now().plusDays(7).toString());
        JTextField dueTimeField = new JTextField("23:59");

        Object[] message = {
            "Select Course:", courseCombo,
            "Assignment Title:", titleField,
            "Description:", new JScrollPane(descArea),
            "Max Marks:", marksField,
            "Due Date (yyyy-MM-dd):", dueDateField,
            "Due Time (HH:mm):", dueTimeField
        };

        int option = JOptionPane.showConfirmDialog(this, message, "Publish Assignment", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                Course selectedCourse = (Course) courseCombo.getSelectedItem();
                if (selectedCourse == null) throw new IllegalArgumentException("Please select a course.");
                if (titleField.getText().trim().isEmpty()) throw new IllegalArgumentException("Assignment title is required.");
                double marks = Double.parseDouble(marksField.getText());
                if (marks <= 0) throw new IllegalArgumentException("Max marks must be greater than zero.");

                LocalDate dueDate = LocalDate.parse(dueDateField.getText().trim());
                LocalTime dueTime = LocalTime.parse(dueTimeField.getText().trim());
                Date deadline = Date.from(LocalDateTime.of(dueDate, dueTime).atZone(ZoneId.systemDefault()).toInstant());

                Assignment a = new Assignment("A-" + UUID.randomUUID().toString().substring(0, 8),
                                           selectedCourse.getCourseId(), titleField.getText().trim(), 
                                           descArea.getText(), deadline,
                                           marks);
                a.setDueTime(dueTime.toString());
                a.setStatus("Published");
                assignmentManager.createAssignment(a);
                refreshAssignments();
                JOptionPane.showMessageDialog(this, "Assignment published to enrolled students.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    private void showSubmissionsDialog() {
        int row = assignmentTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select an assignment to grade.");
            return;
        }
        String assignmentId = (String) tableModel.getValueAt(row, 0);
        List<Submission> subs = assignmentManager.getSubmissionOverviewByAssignment(assignmentId);
        if (subs.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No students are enrolled for this assignment course yet.");
            return;
        }

        String[] cols = {"Student ID", "File Path", "Submitted At", "Status", "Grade", "Feedback"};
        DefaultTableModel subModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (Submission s : subs) {
            String submittedAt = s.getSubmissionDate() == null ? "-" : dateTimeFormat.format(s.getSubmissionDate());
            subModel.addRow(new Object[]{
                    s.getStudentId(),
                    s.getFilePath() == null ? "-" : s.getFilePath(),
                    submittedAt,
                    s.isGraded() ? "Graded" : s.getStatus(),
                    s.isGraded() ? s.getMarks() : "-",
                    s.getFeedback() == null ? "-" : s.getFeedback()
            });
        }

        JTable subTable = new JTable(subModel);
        subTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JButton downloadBtn = new JButton("Download File");
        downloadBtn.addActionListener(e -> {
            int subRow = subTable.getSelectedRow();
            if (subRow == -1) {
                JOptionPane.showMessageDialog(this, "Select a submission to download.");
                return;
            }
            Submission selectedSub = subs.get(subRow);
            if (selectedSub.getId() == null || selectedSub.getFilePath() == null) {
                JOptionPane.showMessageDialog(this, "This student has not submitted a file yet.");
                return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Choose Download Folder");
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    Path downloaded = assignmentManager.downloadSubmission(selectedSub.getId(), chooser.getSelectedFile());
                    JOptionPane.showMessageDialog(this, "Downloaded to: " + downloaded);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Download Error: " + ex.getMessage());
                }
            }
        });

        JButton gradeBtn = new JButton("Submit Grade & Feedback");
        gradeBtn.addActionListener(e -> {
            int subRow = subTable.getSelectedRow();
            if (subRow == -1) {
                JOptionPane.showMessageDialog(this, "Select a submission to grade.");
                return;
            }

            Submission selectedSub = subs.get(subRow);
            if (selectedSub.getId() == null || selectedSub.getFilePath() == null) {
                JOptionPane.showMessageDialog(this, "This student has not submitted a file yet.");
                return;
            }

            double maxMarks = ((Number) tableModel.getValueAt(row, 6)).doubleValue();
            String marksStr = JOptionPane.showInputDialog(this, "Enter Marks (Max " + maxMarks + "):");
            if (marksStr == null) return;
            String feedback = JOptionPane.showInputDialog(this, "Enter Feedback:");
            if (feedback == null) return;

            try {
                double marks = Double.parseDouble(marksStr);
                if (marks < 0 || marks > maxMarks) {
                    throw new IllegalArgumentException("Marks must be between 0 and " + maxMarks + ".");
                }
                assignmentManager.gradeSubmission(selectedSub.getId(), marks, feedback);
                selectedSub.setMarks(marks);
                selectedSub.setFeedback(feedback);
                selectedSub.setGraded(true);
                selectedSub.setStatus("Graded");
                subModel.setValueAt("Graded", subRow, 3);
                subModel.setValueAt(marks, subRow, 4);
                subModel.setValueAt(feedback, subRow, 5);
                JOptionPane.showMessageDialog(this, "Grade recorded.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Grade Error: " + ex.getMessage());
            }
        });

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JScrollPane(subTable), BorderLayout.CENTER);
        JPanel actionPanel = new JPanel(new MigLayout("ins 8, gap 10", "[][]", "[]"));
        actionPanel.add(downloadBtn, "h 38!");
        actionPanel.add(gradeBtn, "h 38!");
        panel.add(actionPanel, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(this, panel, "Student Submissions", JOptionPane.PLAIN_MESSAGE);
    }

    private List<Course> getManagedCourses() {
        return adminMode ? courseManager.getAllCourses() : courseManager.getCoursesByFaculty(facultyId);
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
