package com.university.erp.gui.student;

import com.university.erp.gamification.GamificationEngine;
import com.university.erp.security.StudentContext;
import com.university.models.Student;
import com.university.erp.gui.theme.ThemeManager;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.materialdesign2.*;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

public class GamificationPanel extends JPanel {
    public GamificationPanel() {
        this(StudentContext.requireCurrentStudent());
    }

    public GamificationPanel(Student student) {
        if (student == null) {
            throw new IllegalStateException("GamificationPanel requires a valid Student.");
        }

        setLayout(new MigLayout("ins 30, wrap 1, fillx", "[grow]", "[]20[]20[grow]"));
        ThemeManager.stylePage(this);

        JLabel title = new JLabel("Gamification Hub");
        title.setFont(new Font("Inter", Font.BOLD, 24));
        add(title);

        // XP Cards
        JPanel cards = new JPanel(new MigLayout("ins 0, gap 20", "[grow][grow][grow]", "[]"));
        cards.setOpaque(false);
        cards.add(createStatCard("Current XP", String.valueOf(student.getXpPoints()), MaterialDesignT.TROPHY));
        cards.add(createStatCard("Current Level", String.valueOf(student.getLevel()), MaterialDesignL.LABEL));
        cards.add(createStatCard("Rank", "#1", MaterialDesignM.MEDAL));
        add(cards, "growx");

        // Leaderboard
        JPanel lbCard = ThemeManager.createGlassCard();
        lbCard.setLayout(new MigLayout("wrap 1, fillx"));
        lbCard.add(new JLabel("Department Leaderboard") {{ setFont(new Font("Inter", Font.BOLD, 18)); }}, "gapbottom 10");
        
        String[] cols = {"Student", "XP Points"};
        DefaultTableModel model = new DefaultTableModel(cols, 0);
        for (Map.Entry<String, Integer> entry : GamificationEngine.getLeaderboard().entrySet()) {
            model.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }
        lbCard.add(new JScrollPane(new JTable(model)), "grow");
        add(lbCard, "grow");
    }

    private JPanel createStatCard(String l, String v, Enum icon) {
        JPanel p = ThemeManager.createGlassCard();
        p.setLayout(new MigLayout("ins 15, wrap 1"));
        JLabel val = new JLabel(v);
        val.setFont(new Font("Inter", Font.BOLD, 22));
        val.setForeground(ThemeManager.ACCENT_BLUE);
        p.add(new JLabel(FontIcon.of((org.kordamp.ikonli.Ikon)icon, 24, Color.GRAY)));
        p.add(val);
        p.add(new JLabel(l));
        return p;
    }
}
