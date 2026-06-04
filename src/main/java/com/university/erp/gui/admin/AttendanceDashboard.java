package com.university.erp.gui.admin;

import com.university.erp.analytics.AttendanceAnalytics;
import com.university.erp.gui.theme.ThemeManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AttendanceDashboard extends JPanel {
    public AttendanceDashboard() {
        setLayout(new MigLayout("ins 30, wrap 2, fillx, gap 30", "[grow][grow]", "[]30[grow]30[grow]"));
        ThemeManager.stylePage(this);

        JLabel title = new JLabel("University-Wide Attendance Analytics");
        title.setFont(new Font("Inter", Font.BOLD, 32));
        add(title, "span 2");

        // Charts
        JPanel overallCard = ThemeManager.createGlassCard();
        overallCard.setLayout(new BorderLayout());
        overallCard.add(AttendanceAnalytics.createOverallAttendanceChart(), BorderLayout.CENTER);
        add(overallCard, "grow, h 400!");

        JPanel deptCard = ThemeManager.createGlassCard();
        deptCard.setLayout(new BorderLayout());
        deptCard.add(AttendanceAnalytics.createDeptAttendanceChart(), BorderLayout.CENTER);
        add(deptCard, "grow, h 400!");

        // Low Attendance Table
        JPanel tableCard = ThemeManager.createGlassCard();
        tableCard.setLayout(new MigLayout("ins 0, wrap 1, fillx", "[grow]"));
        
        JLabel tableTitle = new JLabel("Students Below 75% Attendance (At-Risk)");
        tableTitle.setFont(new Font("Inter", Font.BOLD, 18));
        tableTitle.setForeground(ThemeManager.DANGER_RED);
        tableCard.add(tableTitle, "gapbottom 15");

        String[] cols = {"ID", "Name", "Department", "Attendance %"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        List<Object[]> data = AttendanceAnalytics.getLowAttendanceStudents();
        for (Object[] row : data) model.addRow(row);
        
        JTable table = new JTable(model);
        tableCard.add(new JScrollPane(table), "grow");
        
        add(tableCard, "span 2, growx, h 300!");
    }
}
