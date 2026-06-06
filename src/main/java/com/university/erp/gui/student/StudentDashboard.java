package com.university.erp.gui.student;

import com.university.courses.CourseManager;
import com.university.erp.dao.CertificationDAO;
import com.university.erp.dao.ProjectDAO;
import com.university.erp.gui.theme.ThemeManager;
import com.university.erp.security.StudentContext;
import com.university.lms.AssignmentManager;
import com.university.models.Assignment;
import com.university.models.Certification;
import com.university.models.Course;
import com.university.models.Project;
import com.university.models.Student;
import com.university.models.Submission;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class StudentDashboard extends JPanel {
    private final ProjectDAO projectDAO = new ProjectDAO();
    private final CertificationDAO certificationDAO = new CertificationDAO();
    private final CourseManager courseManager = new CourseManager();
    private final AssignmentManager assignmentManager = new AssignmentManager();
    private Student student;
    private final java.util.List<Runnable> uiUnsubHandles = new java.util.ArrayList<>();

    public StudentDashboard() {
        this(StudentContext.requireCurrentStudent());
    }

    public StudentDashboard(Student student) {
        if (student == null) {
            throw new IllegalStateException("StudentDashboard requires a valid Student.");
        }
        this.student = student;
        setLayout(new BorderLayout());
        ThemeManager.stylePage(this);
        // subscribe to assignment/notification events to refresh dashboard in real-time
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("ASSIGNMENT_PUBLISHED", payload -> refreshStudent(com.university.erp.security.StudentContext.requireCurrentStudent())));
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("ASSIGNMENT_GRADED", payload -> refreshStudent(com.university.erp.security.StudentContext.requireCurrentStudent())));
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("NOTIFICATION_CREATED", payload -> refreshStudent(com.university.erp.security.StudentContext.requireCurrentStudent())));
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("ATTENDANCE_UPDATED", payload -> refreshStudent(com.university.erp.security.StudentContext.requireCurrentStudent())));
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("PROFILE_UPDATED", payload -> refreshStudent(com.university.erp.security.StudentContext.requireCurrentStudent())));
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("PROFILE_STAGED", payload -> refreshStudent(com.university.erp.security.StudentContext.requireCurrentStudent())));
        buildDashboard();
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        for (Runnable r : uiUnsubHandles) { try { r.run(); } catch (Exception ignored) {} }
        uiUnsubHandles.clear();
    }

    public void refreshStudent(Student student) {
        if (student == null) {
            throw new IllegalStateException("StudentDashboard requires a valid Student.");
        }
        this.student = student;
        removeAll();
        buildDashboard();
        revalidate();
        repaint();
    }

    private void buildDashboard() {
        // Show lightweight loading placeholder and load data off EDT
        JPanel placeholder = new JPanel(new BorderLayout());
        placeholder.setOpaque(false);
        JProgressBar loading = new JProgressBar();
        loading.setIndeterminate(true);
        JLabel label = new JLabel("Loading dashboard...", SwingConstants.CENTER);
        label.setFont(new Font("Inter", Font.PLAIN, 14));
        placeholder.add(label, BorderLayout.NORTH);
        placeholder.add(loading, BorderLayout.CENTER);
        add(placeholder, BorderLayout.CENTER);

        SwingWorker<DashboardData, Void> worker = new SwingWorker<>() {
            @Override
            protected DashboardData doInBackground() throws Exception {
                return loadDashboardData();
            }

            @Override
            protected void done() {
                remove(placeholder);
                try {
                    DashboardData data = get();
                    JPanel content = new JPanel(new MigLayout(
                            "fillx, ins 24, gap 16",
                            "[grow,fill]",
                            "[]16[]16[]"
                    ));
                    content.setOpaque(false);

                    content.add(createProfileHeader(data), "growx, wrap");
                    content.add(createMetricGrid(data), "growx, wrap");
                    content.add(createDetailGrid(data), "growx");

                    JScrollPane scrollPane = new JScrollPane(content);
                    scrollPane.setBorder(null);
                    scrollPane.getVerticalScrollBar().setUnitIncrement(18);
                    scrollPane.setOpaque(false);
                    scrollPane.getViewport().setOpaque(false);
                    add(scrollPane, BorderLayout.CENTER);

                    revalidate();
                    repaint();
                } catch (Exception e) {
                    remove(placeholder);
                    add(new JLabel("Failed to load dashboard: " + e.getMessage()), BorderLayout.CENTER);
                    revalidate();
                    repaint();
                }
            }
        };
        worker.execute();
    }

    private JPanel createProfileHeader(DashboardData data) {
        JPanel header = ThemeManager.createGlassCard();
        header.setLayout(new MigLayout(
                "fillx, ins 22 24, gap 18",
                "[][grow,fill][right]",
                "[]"
        ));

        JLabel photo = createProfilePhoto(92);
        header.add(photo, "w 92!, h 92!");

        JPanel identity = new JPanel(new MigLayout("ins 0, wrap 1, gap 4", "[grow]", "[][][]"));
        identity.setOpaque(false);

        JLabel name = new JLabel(student.getName());
        name.setFont(new Font("Inter", Font.BOLD, 28));
        name.setForeground(ThemeManager.textPrimary());
        identity.add(name);

        JLabel details = new JLabel(student.getDepartment() + " Department  |  Semester " + student.getSemester());
        details.setFont(new Font("Inter", Font.PLAIN, 15));
        details.setForeground(ThemeManager.textSecondary());
        identity.add(details);

        JLabel email = new JLabel(student.getEmail());
        email.setFont(new Font("Inter", Font.PLAIN, 13));
        email.setForeground(ThemeManager.textSecondary());
        identity.add(email);
        header.add(identity, "growx");

        JPanel focus = new JPanel(new MigLayout("ins 0, wrap 1, align right", "[right]", "[][]"));
        focus.setOpaque(false);
        JLabel pending = new JLabel(data.pendingAssignments + " pending assignments");
        pending.setFont(new Font("Inter", Font.BOLD, 16));
        pending.setForeground(data.pendingAssignments > 0 ? ThemeManager.WARNING_ORANGE : ThemeManager.SUCCESS_GREEN);
        focus.add(pending);
        JLabel notifications = new JLabel(data.unreadNotifications + " unread LMS notifications");
        notifications.setForeground(data.unreadNotifications > 0 ? ThemeManager.WARNING_ORANGE : ThemeManager.textSecondary());
        focus.add(notifications);
        JLabel courseCount = new JLabel(data.courses.size() + " enrolled courses");
        courseCount.setForeground(ThemeManager.textSecondary());
        focus.add(courseCount);
        header.add(focus);

        return header;
    }

    private JPanel createMetricGrid(DashboardData data) {
        JPanel grid = new JPanel(new MigLayout(
                "fillx, ins 0, gap 16",
                "[grow,fill][grow,fill][grow,fill][grow,fill][grow,fill][grow,fill]",
                "[]"
        ));
        grid.setOpaque(false);

        grid.add(createMetricCard("Pending Assignments", String.valueOf(data.pendingAssignments), MaterialDesignC.CLIPBOARD_CHECK, ThemeManager.WARNING_ORANGE));
        grid.add(createMetricCard("Submitted Assignments", String.valueOf(data.submittedAssignments), MaterialDesignC.CHECK_CIRCLE, ThemeManager.ACCENT_BLUE));
        grid.add(createMetricCard("Overdue Assignments", String.valueOf(data.overdueAssignments), MaterialDesignA.ALERT_CIRCLE, ThemeManager.DANGER_RED));
        grid.add(createMetricCard("Upcoming Deadlines", String.valueOf(data.upcomingDeadlines), MaterialDesignC.CLOCK_OUTLINE, new Color(116, 90, 242)));
        grid.add(createMetricCard("Courses", String.valueOf(data.courses.size()), MaterialDesignB.BOOK_OPEN_PAGE_VARIANT, new Color(33, 150, 136)));
        grid.add(createMetricCard("CGPA", String.format("%.2f", student.getCgpa()), MaterialDesignC.CHART_BAR, new Color(142, 68, 173)));
        return grid;
    }

    private JPanel createMetricCard(String label, String value, Enum<?> icon, Color accent) {
        JPanel card = ThemeManager.createGlassCard();
        card.setLayout(new MigLayout("ins 14, fillx", "[][grow]", "[]4[]"));

        JLabel iconLabel = new JLabel(FontIcon.of((org.kordamp.ikonli.Ikon) icon, 24, accent));
        card.add(iconLabel, "spany 2, w 32!");

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Inter", Font.BOLD, 22));
        valueLabel.setForeground(ThemeManager.textPrimary());
        card.add(valueLabel, "growx, wrap");

        JLabel labelText = new JLabel(label);
        labelText.setFont(new Font("Inter", Font.PLAIN, 12));
        labelText.setForeground(ThemeManager.textSecondary());
        card.add(labelText, "growx");
        return card;
    }

    private JPanel createDetailGrid(DashboardData data) {
        JPanel grid = new JPanel(new MigLayout(
                "fillx, ins 0, gap 16",
                "[grow,fill][grow,fill]",
                "[]16[]"
        ));
        grid.setOpaque(false);

        grid.add(createCoursesCard(data.courses), "growx, hmin 230");
        grid.add(createAssignmentsCard(data.assignments), "growx, hmin 230, wrap");
        grid.add(createProjectsCard(data.projects), "growx, hmin 230");
        grid.add(createCertificationsCard(data.certifications), "growx, hmin 230");
        return grid;
    }

    private JPanel createCoursesCard(List<Course> courses) {
        JPanel card = sectionCard("Enrolled Courses", MaterialDesignB.BOOK_OPEN_PAGE_VARIANT);
        if (courses.isEmpty()) {
            card.add(emptyLabel("No course enrollments found."));
            return card;
        }

        for (Course course : courses) {
            JPanel row = new JPanel(new MigLayout("fillx, ins 8 0, gap 10", "[grow,fill][right]", "[][]"));
            row.setOpaque(false);
            JLabel name = rowTitle(course.getCourseName());
            row.add(name, "growx");
            JLabel credits = statusLabel(course.getCredits() + " credits", ThemeManager.ACCENT_BLUE);
            row.add(credits, "wrap");
            JLabel meta = rowMeta(course.getCourseId() + "  |  Faculty " + course.getAssignedFacultyId());
            row.add(meta, "span 2, growx");
            card.add(row, "growx, wrap");
        }
        return card;
    }

    private JPanel createAssignmentsCard(List<AssignmentSummary> assignments) {
        JPanel card = sectionCard("My Assignments", MaterialDesignC.CLIPBOARD_CHECK);
        if (assignments.isEmpty()) {
            card.add(emptyLabel("No assignments available for enrolled courses."));
            return card;
        }

        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyy-MM-dd");
        for (AssignmentSummary item : assignments) {
            JPanel row = new JPanel(new MigLayout("fillx, ins 8 0, gap 10", "[grow,fill][right]", "[][][]"));
            row.setOpaque(false);
            row.add(rowTitle(item.assignment.getTitle()), "growx");
            row.add(statusLabel(item.status, statusColor(item.status)), "wrap");
            String courseName = item.course == null ? item.assignment.getCourseId() : item.course.getCourseName();
            String dueDate = item.assignment.getDeadline() == null ? "No due date" : dateFmt.format(item.assignment.getDeadline());
            row.add(rowMeta(courseName + "  |  Due Date " + dueDate + "  |  Due Time " +
                    (item.assignment.getDueTime() == null ? "-" : item.assignment.getDueTime())), "span 2, growx, wrap");
            row.add(rowMeta(item.assignment.getDescription()), "span 2, growx, wrap");
            row.add(rowMeta(assignmentManager.getDeadlineCountdown(item.assignment)), "span 2, growx");
            card.add(row, "growx, wrap");
        }
        return card;
    }

    private JPanel createProjectsCard(List<Project> projects) {
        JPanel card = sectionCard("Projects", MaterialDesignP.PROJECTOR_SCREEN);
        if (projects.isEmpty()) {
            card.add(emptyLabel("No project submissions yet."));
            return card;
        }

        for (Project project : projects) {
            JPanel row = new JPanel(new MigLayout("fillx, ins 8 0, gap 10", "[grow,fill][right]", "[][]"));
            row.setOpaque(false);
            row.add(rowTitle(project.getTitle()), "growx");
            row.add(statusLabel(project.getStatus(), ThemeManager.SUCCESS_GREEN), "wrap");
            row.add(rowMeta(project.getDescription()), "span 2, growx");
            card.add(row, "growx, wrap");
        }
        return card;
    }

    private JPanel createCertificationsCard(List<Certification> certifications) {
        JPanel card = sectionCard("Certifications", MaterialDesignC.CERTIFICATE);
        if (certifications.isEmpty()) {
            card.add(emptyLabel("No certifications uploaded yet."));
            return card;
        }

        SimpleDateFormat fmt = new SimpleDateFormat("MMM yyyy");
        for (Certification certification : certifications) {
            JPanel row = new JPanel(new MigLayout("fillx, ins 8 0, gap 10", "[grow,fill][right]", "[][]"));
            row.setOpaque(false);
            row.add(rowTitle(certification.getTitle()), "growx");
            row.add(statusLabel(certification.getIssuer(), new Color(142, 68, 173)), "wrap");
            String completed = certification.getCompletionDate() == null ? "Completion date unavailable" : "Completed " + fmt.format(certification.getCompletionDate());
            row.add(rowMeta(completed), "span 2, growx");
            card.add(row, "growx, wrap");
        }
        return card;
    }

    private JPanel sectionCard(String title, Enum<?> icon) {
        JPanel card = ThemeManager.createGlassCard();
        card.setLayout(new MigLayout("fillx, ins 18, wrap 1, gap 8", "[grow,fill]", "[]10[]"));
        JLabel heading = new JLabel(title, FontIcon.of((org.kordamp.ikonli.Ikon) icon, 20, ThemeManager.ACCENT_BLUE), JLabel.LEFT);
        heading.setFont(new Font("Inter", Font.BOLD, 17));
        heading.setForeground(ThemeManager.textPrimary());
        heading.setIconTextGap(10);
        card.add(heading, "growx");
        return card;
    }

    private JLabel createProfilePhoto(int size) {
        JLabel label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(ThemeManager.surfaceAlt());
        label.setBorder(BorderFactory.createLineBorder(ThemeManager.border(), 1));

        String path = student.getProfilePicturePath();
        try {
            if (path != null && !path.isBlank() && new File(path).isFile()) {
                // Use ImageIO to robustly load different image formats
                java.awt.Image img = javax.imageio.ImageIO.read(new File(path));
                if (img != null) {
                    Image scaled = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    label.setIcon(new ImageIcon(scaled));
                } else {
                    label.setIcon(FontIcon.of(MaterialDesignA.ACCOUNT_CIRCLE, size - 8, new Color(156, 166, 176)));
                }
            } else {
                label.setIcon(FontIcon.of(MaterialDesignA.ACCOUNT_CIRCLE, size - 8, new Color(156, 166, 176)));
            }
        } catch (Exception e) {
            // Fallback to icon if any issue occurs while loading image
            label.setIcon(FontIcon.of(MaterialDesignA.ACCOUNT_CIRCLE, size - 8, new Color(156, 166, 176)));
        }
        // ensure preferred size to avoid clipping
        label.setPreferredSize(new Dimension(size, size));
        return label;
    }

    private JLabel rowTitle(String text) {
        JLabel label = new JLabel(text == null || text.isBlank() ? "Untitled" : text);
        label.setFont(new Font("Inter", Font.BOLD, 14));
        label.setForeground(ThemeManager.textPrimary());
        return label;
    }

    private JLabel rowMeta(String text) {
        JLabel label = new JLabel(text == null || text.isBlank() ? "-" : text);
        label.setFont(new Font("Inter", Font.PLAIN, 12));
        label.setForeground(ThemeManager.textSecondary());
        return label;
    }

    private JLabel emptyLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Inter", Font.PLAIN, 13));
        label.setForeground(ThemeManager.textSecondary());
        return label;
    }

    private JLabel statusLabel(String text, Color color) {
        JLabel label = new JLabel(text == null || text.isBlank() ? "Pending" : text);
        label.setOpaque(true);
        label.setFont(new Font("Inter", Font.BOLD, 11));
        label.setForeground(Color.WHITE);
        label.setBackground(color);
        label.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return label;
    }

    private Color statusColor(String status) {
        if ("Graded".equalsIgnoreCase(status)) return ThemeManager.SUCCESS_GREEN;
        if ("Submitted".equalsIgnoreCase(status)) return ThemeManager.ACCENT_BLUE;
        if ("Late".equalsIgnoreCase(status) || "Overdue".equalsIgnoreCase(status)) return ThemeManager.DANGER_RED;
        return ThemeManager.WARNING_ORANGE;
    }

    private DashboardData loadDashboardData() {
        DashboardData data = new DashboardData();
        data.courses = courseManager.getEnrolledCourses(student.getId());
        data.unreadNotifications = assignmentManager.getUnreadAssignmentNotificationCount(student.getId());

        for (Assignment assignment : assignmentManager.getAssignmentsForStudent(student.getId())) {
            Course course = courseManager.searchById(assignment.getCourseId());
            String status = assignmentManager.getStudentAssignmentStatus(student.getId(), assignment);
            if ("Pending".equalsIgnoreCase(status)) data.pendingAssignments++;
            if ("Submitted".equalsIgnoreCase(status) || "Late".equalsIgnoreCase(status)) data.submittedAssignments++;
            if ("Overdue".equalsIgnoreCase(status)) data.overdueAssignments++;
            if (!"Graded".equalsIgnoreCase(status) && !"Overdue".equalsIgnoreCase(status)) data.upcomingDeadlines++;
            data.assignments.add(new AssignmentSummary(course, assignment, status));
        }

        try {
            data.projects = projectDAO.getProjectsByStudent(student.getId());
            data.certifications = certificationDAO.getCertificationsByStudent(student.getId());
        } catch (SQLException e) {
            throw new IllegalStateException("Unable to load dashboard data: " + e.getMessage(), e);
        }

        return data;
    }

    private static class DashboardData {
        private List<Course> courses = new ArrayList<>();
        private List<AssignmentSummary> assignments = new ArrayList<>();
        private List<Project> projects = new ArrayList<>();
        private List<Certification> certifications = new ArrayList<>();
        private int pendingAssignments;
        private int submittedAssignments;
        private int overdueAssignments;
        private int upcomingDeadlines;
        private int unreadNotifications;
    }

    private static class AssignmentSummary {
        private final Course course;
        private final Assignment assignment;
        private final String status;

        private AssignmentSummary(Course course, Assignment assignment, String status) {
            this.course = course;
            this.assignment = assignment;
            this.status = status;
        }
    }
}
