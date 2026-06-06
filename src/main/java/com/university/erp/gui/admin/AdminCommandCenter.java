package com.university.erp.gui.admin;

import com.university.erp.analytics.ExecutiveAnalytics;
import com.university.erp.gui.theme.ThemeManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

public class AdminCommandCenter extends JPanel {
    private final java.util.List<Runnable> uiUnsubHandles = new java.util.ArrayList<>();

    public AdminCommandCenter() {
        setLayout(new MigLayout("ins 30, wrap 2, fillx, gap 30", "[grow][grow]", "[]30[]30[grow]"));
        ThemeManager.stylePage(this);

        JLabel title = new JLabel("University Executive Dashboard");
        title.setFont(new Font("Inter", Font.BOLD, 32));
        add(title, "span 2");

        // Stat Row
        JPanel statRow = new JPanel(new MigLayout("ins 0, gap 20", "[grow][grow][grow][grow][grow][grow]", "[]"));
        statRow.setOpaque(false);
        statRow.add(createSummaryCard("Students", ExecutiveAnalytics.getCount("students")));
        statRow.add(createSummaryCard("Faculty", ExecutiveAnalytics.getCount("faculty")));
        statRow.add(createSummaryCard("Courses", ExecutiveAnalytics.getCount("courses")));
        statRow.add(createSummaryCard("Assignments", ExecutiveAnalytics.getCount("assignments")));
        statRow.add(createSummaryCard("Submissions", ExecutiveAnalytics.getCount("submissions")));
        statRow.add(createSummaryCard("Avg Attendance", Math.round(ExecutiveAnalytics.getAverageAttendance()) + "%"));
        add(statRow, "span 2, growx");

        // subscribe to UI events to refresh executive stats when submissions/assignments change
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("ASSIGNMENT_PUBLISHED", payload -> refreshStats(statRow)));
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("ASSIGNMENT_GRADED", payload -> refreshStats(statRow)));
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("SUBMISSION_CREATED", payload -> refreshStats(statRow)));
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("ATTENDANCE_UPDATED", payload -> refreshStats(statRow)));

        // Charts
        JPanel enrollmentCard = ThemeManager.createGlassCard();
        enrollmentCard.setLayout(new BorderLayout());
        enrollmentCard.add(ExecutiveAnalytics.createEnrollmentChart(), BorderLayout.CENTER);
        add(enrollmentCard, "grow, h 400!");

        JPanel placementCard = ThemeManager.createGlassCard();
        placementCard.setLayout(new BorderLayout());
        placementCard.add(ExecutiveAnalytics.createPlacementStats(), BorderLayout.CENTER);
        add(placementCard, "grow, h 400!");
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        for (Runnable r : uiUnsubHandles) { try { r.run(); } catch (Exception ignored) {} }
        uiUnsubHandles.clear();
    }

    private JPanel createSummaryCard(String label, Object count) {
        JPanel p = ThemeManager.createGlassCard();
        p.setLayout(new MigLayout("ins 15, wrap 1", "[]", "[]5[]"));
        JLabel c = new JLabel(String.valueOf(count));
        c.setFont(new Font("Inter", Font.BOLD, 24));
        c.setForeground(ThemeManager.ACCENT_BLUE);
        JLabel l = new JLabel(label);
        l.setForeground(Color.DARK_GRAY);
        p.setPreferredSize(new Dimension(220, 90));
        p.add(c);
        p.add(l);
        return p;
    }

    private void refreshStats(JPanel statRow) {
        if (statRow == null) return;
        statRow.removeAll();
        statRow.add(createSummaryCard("Students", ExecutiveAnalytics.getCount("students")));
        statRow.add(createSummaryCard("Faculty", ExecutiveAnalytics.getCount("faculty")));
        statRow.add(createSummaryCard("Courses", ExecutiveAnalytics.getCount("courses")));
        statRow.add(createSummaryCard("Assignments", ExecutiveAnalytics.getCount("assignments")));
        statRow.add(createSummaryCard("Submissions", ExecutiveAnalytics.getCount("submissions")));
        statRow.add(createSummaryCard("Avg Attendance", Math.round(ExecutiveAnalytics.getAverageAttendance()) + "%"));
        statRow.revalidate();
        statRow.repaint();

        // Optionally refresh recent activity
        // find panel below charts and refresh if exists
    }}
