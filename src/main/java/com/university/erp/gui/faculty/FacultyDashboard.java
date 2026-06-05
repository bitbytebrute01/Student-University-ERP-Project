package com.university.erp.gui.faculty;

import com.university.erp.analytics.ExecutiveAnalytics;
import com.university.courses.CourseManager;
import com.university.erp.security.SessionManager;
import com.university.erp.security.User;
import com.university.erp.gui.theme.ThemeManager;
import com.university.lms.AssignmentManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class FacultyDashboard extends JPanel {
    private CourseManager courseManager = new CourseManager();
    private AssignmentManager assignmentManager = new AssignmentManager();
    private String facultyId;

    public FacultyDashboard() {
        facultyId = resolveFacultyId();
        setLayout(new MigLayout("ins 30, wrap 2, fillx, gap 30", "[grow][grow]", "[]30[]30[grow]"));
        ThemeManager.stylePage(this);

        JLabel title = new JLabel("Faculty Academic Hub");
        title.setFont(new Font("Inter", Font.BOLD, 32));
        add(title, "span 2");

        // Metrics
        AssignmentManager.FacultyAssignmentMetrics metrics = assignmentManager.getFacultyAssignmentMetrics(facultyId, false);
        JPanel stats = new JPanel(new MigLayout("ins 0, gap 20", "[grow][grow][grow][grow]", "[]"));
        stats.setOpaque(false);
        stats.add(createStatCard("Total Assignments", metrics.getTotalAssignments()));
        stats.add(createStatCard("Pending Reviews", metrics.getPendingReviews()));
        stats.add(createStatCard("Late Submissions", metrics.getLateSubmissions()));
        stats.add(createStatCard("Recent Submissions", metrics.getRecentSubmissions()));
        add(stats, "span 2, growx");

        // Info Cards
        JPanel coursesCard = ThemeManager.createGlassCard();
        coursesCard.setLayout(new MigLayout("wrap 1", "[grow]"));
        coursesCard.add(new JLabel("Your Active Courses") {{ setFont(new Font("Inter", Font.BOLD, 18)); }}, "gapbottom 10");
        for (com.university.models.Course c : courseManager.getCoursesByFaculty(facultyId)) {
            coursesCard.add(new JLabel("• " + c.getCourseName() + " (" + c.getCourseId() + ")"));
        }
        add(coursesCard, "grow, h 300!");

        JPanel alerts = ThemeManager.createGlassCard();
        alerts.setLayout(new MigLayout("wrap 1", "[grow]"));
        alerts.add(new JLabel("Recent Academic Alerts") {{ setFont(new Font("Inter", Font.BOLD, 18)); }}, "gapbottom 10");
        alerts.add(new JLabel("● Attendance checking thread is active."));
        alerts.add(new JLabel("● Assignment deadlines approaching for CS-101."));
        add(alerts, "grow, h 300!");
    }

    private int getTotalStudentsEnrolled() {
        int total = 0;
        for (com.university.models.Course c : courseManager.getCoursesByFaculty(facultyId)) {
            total += courseManager.getEnrolledStudents(c.getCourseId()).size();
        }
        return total;
    }

    private JPanel createStatCard(String label, int value) {
        JPanel p = ThemeManager.createGlassCard();
        p.setLayout(new MigLayout("ins 15, wrap 1"));
        JLabel val = new JLabel(String.valueOf(value));
        val.setFont(new Font("Inter", Font.BOLD, 22));
        val.setForeground(ThemeManager.ACCENT_BLUE);
        p.add(val);
        p.add(new JLabel(label));
        return p;
    }

    private String resolveFacultyId() {
        User user = SessionManager.getCurrentUser();
        if (user != null && user.getRefId() != null && !user.getRefId().isBlank()) {
            return user.getRefId();
        }
        return user != null ? user.getUsername() : "";
    }
}
