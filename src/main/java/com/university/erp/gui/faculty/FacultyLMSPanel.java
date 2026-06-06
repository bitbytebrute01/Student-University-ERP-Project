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
import java.util.ArrayList;
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
    private JPanel metricsPanel;
    private String facultyId;
    private boolean adminMode;

    public FacultyLMSPanel() {
        facultyId = resolveFacultyId();
        adminMode = isAdminUser();
        setLayout(new MigLayout("ins 30, wrap 1, fillx", "[grow]", "[]16[]16[grow]20[]"));
        ThemeManager.stylePage(this);

        JLabel title = new JLabel("Academic Assignment & Grading Portal");
        title.setFont(new Font("Inter", Font.BOLD, 24));
        add(title);

        metricsPanel = new JPanel(new MigLayout("ins 0, gap 14", "[grow,fill][grow,fill][grow,fill][grow,fill]", "[]"));
        metricsPanel.setOpaque(false);
        add(metricsPanel, "growx");

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
        refreshMetrics();
        tableModel.setRowCount(0);
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        JDialog loading = new JDialog(SwingUtilities.getWindowAncestor(this));
        loading.setUndecorated(true);
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createLineBorder(ThemeManager.border(), 1));
        p.setBackground(ThemeManager.surface());
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setPreferredSize(new Dimension(220, 16));
        p.add(new JLabel("Loading assignments...", SwingConstants.CENTER), BorderLayout.NORTH);
        p.add(bar, BorderLayout.CENTER);
        loading.getContentPane().add(p);
        loading.pack();
        loading.setLocationRelativeTo(this);

        SwingWorker<Void, Object[]> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<Course> courses = getManagedCourses();
                for (Course c : courses) {
                    List<Assignment> assignments = assignmentManager.getAssignmentsByCourse(c.getCourseId());
                    for (Assignment a : assignments) {
                        String dueDate = a.getDeadline() == null ? "-" : dateFormat.format(a.getDeadline());
                        publish(new Object[]{a.getId(), c.getCourseName(), a.getTitle(), dueDate, a.getDueTime(), a.getStatus(), a.getMaxMarks()});
                    }
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Object[]> chunks) {
                for (Object[] row : chunks) tableModel.addRow(row);
            }

            @Override
            protected void done() {
                loading.setVisible(false);
                loading.dispose();
            }
        };

        SwingUtilities.invokeLater(() -> {
            loading.setVisible(true);
            worker.execute();
        });
    }

    private void refreshMetrics() {
        if (metricsPanel == null) return;
        metricsPanel.removeAll();
        AssignmentManager.FacultyAssignmentMetrics metrics = assignmentManager.getFacultyAssignmentMetrics(facultyId, adminMode);
        metricsPanel.add(createMetricCard("Total Assignments", metrics.getTotalAssignments()));
        metricsPanel.add(createMetricCard("Pending Reviews", metrics.getPendingReviews()));
        metricsPanel.add(createMetricCard("Late Submissions", metrics.getLateSubmissions()));
        metricsPanel.add(createMetricCard("Recent Submissions", metrics.getRecentSubmissions()));
        metricsPanel.revalidate();
        metricsPanel.repaint();
    }

    private JPanel createMetricCard(String label, int value) {
        JPanel card = ThemeManager.createGlassCard();
        card.setLayout(new MigLayout("ins 14, wrap 1", "[grow]", "[]2[]"));
        JLabel valueLabel = new JLabel(String.valueOf(value));
        valueLabel.setFont(new Font("Inter", Font.BOLD, 22));
        valueLabel.setForeground(ThemeManager.ACCENT_BLUE);
        JLabel labelText = new JLabel(label);
        labelText.setForeground(ThemeManager.textSecondary());
        card.add(valueLabel);
        card.add(labelText);
        return card;
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
                // notify UI listeners so student dashboards refresh immediately
                com.university.erp.gui.UIEventBus.publish("ASSIGNMENT_PUBLISHED", a);
                // also signal notification creation so header badges refresh immediately
                com.university.erp.gui.UIEventBus.publish("NOTIFICATION_CREATED", a);
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

        String[] cols = {"Student ID", "Text Response", "Files", "Submitted At", "Status", "Grade", "Feedback"};
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
                    summarizeText(s.getSubmissionText()),
                    s.hasAttachments() ? s.getAttachmentPaths().size() + " file(s)" : "-",
                    submittedAt,
                    s.isGraded() ? "Graded" : s.getStatus(),
                    s.isGraded() ? s.getMarks() : "-",
                    s.getFeedback() == null ? "-" : s.getFeedback()
            });
        }

        JTable subTable = new JTable(subModel);
        subTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JButton downloadBtn = new JButton("Download Files");
        downloadBtn.addActionListener(e -> {
            int subRow = subTable.getSelectedRow();
            if (subRow == -1) {
                JOptionPane.showMessageDialog(this, "Select a submission to download.");
                return;
            }
            Submission selectedSub = subs.get(subRow);
            if (selectedSub.getId() == null || !selectedSub.hasAttachments()) {
                JOptionPane.showMessageDialog(this, "This student has not submitted a file yet.");
                return;
            }

            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setDialogTitle("Choose Download Folder");
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    List<Path> downloaded = assignmentManager.downloadSubmissionFiles(selectedSub.getId(), chooser.getSelectedFile());
                    JOptionPane.showMessageDialog(this, "Downloaded " + downloaded.size() + " file(s) to: " + chooser.getSelectedFile());
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
            if (selectedSub.getId() == null) {
                JOptionPane.showMessageDialog(this, "This student has not submitted yet.");
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
                subModel.setValueAt("Graded", subRow, 4);
                subModel.setValueAt(marks, subRow, 5);
                subModel.setValueAt(feedback, subRow, 6);
                // notify UI listeners to refresh dashboards/notifications
                com.university.erp.gui.UIEventBus.publish("ASSIGNMENT_GRADED", selectedSub);
                refreshMetrics();
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

    private String summarizeText(String text) {
        if (text == null || text.isBlank()) return "-";
        String normalized = text.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 70 ? normalized : normalized.substring(0, 67) + "...";
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
