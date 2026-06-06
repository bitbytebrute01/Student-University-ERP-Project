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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AssignmentHub extends JPanel {
    private AssignmentManager assignmentManager = new AssignmentManager();
    private CourseManager courseManager = new CourseManager();
    private JTable assignmentTable;
    private DefaultTableModel tableModel;
    private String studentId;
    private JLabel notificationLabel;
    private final java.util.List<Runnable> uiUnsubHandles = new java.util.ArrayList<>();

    public AssignmentHub() {
        this(StudentContext.requireCurrentStudent());
    }

    public AssignmentHub(Student student) {
        if (student == null) {
            throw new IllegalStateException("AssignmentHub requires a valid Student.");
        }
        studentId = student.getId();
        setLayout(new MigLayout("ins 30, wrap 1, fillx", "[grow]", "[]12[]12[grow]20[]"));
        ThemeManager.stylePage(this);

        JLabel title = new JLabel("LMS: My Assignments");
        title.setFont(new Font("Inter", Font.BOLD, 24));
        add(title);

        notificationLabel = new JLabel();
        notificationLabel.setFont(new Font("Inter", Font.BOLD, 13));
        notificationLabel.setOpaque(true);
        notificationLabel.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        add(notificationLabel, "growx");

        String[] cols = {"ID", "Course", "Title", "Description", "Due Date", "Due Time", "Time Remaining", "Status", "Grade", "Feedback", "Files"};
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

        // subscribe to UI events to refresh assignment list when relevant
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("ASSIGNMENT_PUBLISHED", payload -> refreshTable()));
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("ASSIGNMENT_GRADED", payload -> refreshTable()));
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("NOTIFICATION_CREATED", payload -> refreshTable()));
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("SUBMISSION_CREATED", payload -> refreshTable()));
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        for (Runnable r : uiUnsubHandles) { try { r.run(); } catch (Exception ignored) {} }
        uiUnsubHandles.clear();
    }

    private void refreshTable() {
        // Load notifications and assignments off the EDT to keep UI responsive
        refreshNotifications();
        tableModel.setRowCount(0);
        final SimpleDateFormat dueDateFormat = new SimpleDateFormat("yyyy-MM-dd");

        JDialog loading = new JDialog(SwingUtilities.getWindowAncestor(this));
        loading.setUndecorated(true);
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createLineBorder(ThemeManager.border(), 1));
        p.setBackground(ThemeManager.surface());
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setPreferredSize(new Dimension(240, 18));
        p.add(new JLabel("Loading assignments...", SwingConstants.CENTER), BorderLayout.NORTH);
        p.add(bar, BorderLayout.CENTER);
        loading.getContentPane().add(p);
        loading.pack();
        loading.setLocationRelativeTo(this);

        SwingWorker<Void, Object[]> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                List<Assignment> assignments = assignmentManager.getAssignmentsForStudent(studentId);
                for (Assignment a : assignments) {
                    Course c = courseManager.searchById(a.getCourseId());
                    Submission s = assignmentManager.getStudentSubmission(studentId, a.getId());
                    String status = assignmentManager.getStudentAssignmentStatus(studentId, a);
                    String grade = (s != null && s.isGraded()) ? String.valueOf(s.getMarks()) : "-";
                    String feedback = (s != null && s.getFeedback() != null && !s.getFeedback().isBlank()) ? s.getFeedback() : "-";
                    String dueDate = a.getDeadline() == null ? "-" : dueDateFormat.format(a.getDeadline());
                    String files = s == null || !s.hasAttachments() ? "-" : s.getAttachmentPaths().size() + " file(s)";
                    String courseName = c == null ? a.getCourseId() : c.getCourseName();
                    publish(new Object[]{a.getId(), courseName, a.getTitle(), a.getDescription(), dueDate, a.getDueTime() == null ? "-" : a.getDueTime(), assignmentManager.getDeadlineCountdown(a), status, grade, feedback, files});
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Object[]> chunks) {
                for (Object[] row : chunks) {
                    tableModel.addRow(row);
                }
            }

            @Override
            protected void done() {
                loading.setVisible(false);
                loading.dispose();
            }
        };

        // show loading and execute
        SwingUtilities.invokeLater(() -> {
            loading.setVisible(true);
            worker.execute();
        });
    }

    private void refreshNotifications() {
        int unread = assignmentManager.getUnreadAssignmentNotificationCount(studentId);
        if (unread > 0) {
            notificationLabel.setText("New Assignment Assigned - " + unread + " unread notification(s)");
            notificationLabel.setBackground(new Color(255, 245, 230));
            notificationLabel.setForeground(ThemeManager.WARNING_ORANGE);
        } else {
            notificationLabel.setText("No unread assignment notifications");
            notificationLabel.setBackground(ThemeManager.surfaceAlt());
            notificationLabel.setForeground(ThemeManager.textSecondary());
        }
    }

    private void showSubmitDialog() {
        int row = assignmentTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Please select an assignment from the list.");
            return;
        }
        String assignmentId = (String) tableModel.getValueAt(row, 0);
        String status = (String) tableModel.getValueAt(row, 7);
        
        if ("Graded".equals(status)) {
            JOptionPane.showMessageDialog(this, "This assignment has already been graded and cannot be resubmitted.");
            return;
        }

        JTextArea responseArea = new JTextArea(6, 36);
        responseArea.setLineWrap(true);
        responseArea.setWrapStyleWord(true);
        DefaultListModel<File> fileModel = new DefaultListModel<>();
        JList<File> fileList = new JList<>(fileModel);
        fileList.setVisibleRowCount(4);
        fileList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof File) {
                    setText(((File) value).getName());
                }
                return this;
            }
        });

        JButton addFilesBtn = new JButton("Add PDF/DOCX/ZIP Files");
        addFilesBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setMultiSelectionEnabled(true);
            chooser.setFileFilter(new FileNameExtensionFilter("Assignment files (*.pdf, *.docx, *.zip)", "pdf", "docx", "zip"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                for (File file : chooser.getSelectedFiles()) {
                    fileModel.addElement(file);
                }
            }
        });

        JPanel panel = new JPanel(new MigLayout("fillx, wrap 1, ins 8", "[grow]", "[]6[]10[]6[grow]"));
        panel.add(new JLabel("Text Response:"), "growx");
        panel.add(new JScrollPane(responseArea), "growx, h 130!");
        panel.add(addFilesBtn, "left");
        panel.add(new JScrollPane(fileList), "growx, h 100!");

        int option = JOptionPane.showConfirmDialog(this, panel, "Submit Assignment", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) {
            try {
                List<File> selectedFiles = new ArrayList<>();
                for (int i = 0; i < fileModel.size(); i++) {
                    selectedFiles.add(fileModel.get(i));
                }
                List<String> savedPaths = new ArrayList<>();
                if (!selectedFiles.isEmpty()) {
                    for (File f : selectedFiles) {
                        savedPaths.add(MediaManager.saveTemp(f));
                    }
                }

                Submission s = new Submission("SUB-" + UUID.randomUUID().toString().substring(0, 8), assignmentId, studentId, null);
                s.setSubmissionText(responseArea.getText());
                s.setAttachmentPaths(savedPaths);
                assignmentManager.submitAssignment(s);
                // notify faculty dashboards and other UI about new submission
                com.university.erp.gui.UIEventBus.publish("SUBMISSION_CREATED", s);

                JOptionPane.showMessageDialog(this, "Assignment submitted successfully. Status: " + s.getStatus());
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Submission Error: " + ex.getMessage());
            }
        }
    }
}
