package com.university.erp.gui.student;

import com.university.ai.AIRecommendationEngine;
import com.university.erp.gui.theme.ThemeManager;
import com.university.erp.security.StudentContext;
import com.university.models.Student;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.materialdesign2.MaterialDesignA;
import org.kordamp.ikonli.materialdesign2.MaterialDesignB;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;
import org.kordamp.ikonli.materialdesign2.MaterialDesignL;
import org.kordamp.ikonli.materialdesign2.MaterialDesignP;
import org.kordamp.ikonli.materialdesign2.MaterialDesignR;
import org.kordamp.ikonli.materialdesign2.MaterialDesignT;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.util.List;

public class StudentLinkedInProfile extends JPanel {
    private final AIRecommendationEngine aiEngine = new AIRecommendationEngine();
    private final Student student;

    public StudentLinkedInProfile() {
        this(StudentContext.requireCurrentStudent());
    }

    public StudentLinkedInProfile(Student student) {
        if (student == null) {
            throw new IllegalStateException("StudentLinkedInProfile requires a valid Student.");
        }
        this.student = student;

        setLayout(new BorderLayout());
        ThemeManager.stylePage(this);

        JPanel content = new JPanel(new MigLayout(
                "fillx, ins 24, gap 16",
                "[grow,fill]",
                "[]16[]"
        ));
        content.setOpaque(false);

        content.add(createHeaderSection(), "growx, wrap");
        content.add(createMainGrid(), "growx");

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(18);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createHeaderSection() {
        JPanel header = ThemeManager.createGlassCard();
        header.setLayout(new MigLayout(
                "fillx, ins 22 24, gap 18",
                "[][grow,fill][right]",
                "[]"
        ));

        header.add(createProfilePhoto(96), "w 96!, h 96!");

        JPanel identity = new JPanel(new MigLayout("ins 0, wrap 1, gap 4", "[grow,fill]", "[][][][]"));
        identity.setOpaque(false);

        JLabel name = new JLabel(student.getName());
        name.setFont(new Font("Inter", Font.BOLD, 28));
        name.setForeground(ThemeManager.textPrimary());
        identity.add(name, "growx");

        JLabel subtitle = new JLabel(student.getDepartment() + " Department  |  Semester " + student.getSemester());
        subtitle.setFont(new Font("Inter", Font.PLAIN, 15));
        subtitle.setForeground(ThemeManager.textSecondary());
        identity.add(subtitle, "growx");

        JLabel email = new JLabel(student.getEmail());
        email.setFont(new Font("Inter", Font.PLAIN, 13));
        email.setForeground(ThemeManager.textSecondary());
        identity.add(email, "growx");

        JLabel bio = new JLabel("<html>" + safeText(student.getBio()) + "</html>");
        bio.setFont(new Font("Inter", Font.PLAIN, 13));
        bio.setForeground(ThemeManager.textSecondary());
        identity.add(bio, "growx");

        header.add(identity, "growx");

        JPanel summary = new JPanel(new MigLayout("ins 0, wrap 1, align right", "[right]", "[][][]"));
        summary.setOpaque(false);
        summary.add(summaryLabel(String.format("%.2f CGPA", student.getCgpa()), ThemeManager.ACCENT_BLUE));
        summary.add(summaryLabel(String.format("%.1f%% attendance", student.getAttendancePercentage()), ThemeManager.SUCCESS_GREEN));
        summary.add(summaryLabel("Level " + student.getLevel(), new Color(116, 90, 242)));
        header.add(summary);

        return header;
    }

    private JPanel createMainGrid() {
        JPanel grid = new JPanel(new MigLayout(
                "fillx, ins 0, gap 16",
                "[grow 65,fill][grow 35,fill]",
                "[]16[]16[]"
        ));
        grid.setOpaque(false);

        grid.add(createAboutSection(), "growx, hmin 170");
        grid.add(createReadinessSection(), "growx, hmin 170, wrap");
        grid.add(createAnalyticsSection(), "growx, hmin 170");
        grid.add(createAIInsightsSection(), "growx, hmin 170, wrap");
        grid.add(createListSection("Key Projects", student.getProjects(), MaterialDesignP.PROJECTOR_SCREEN), "growx, hmin 210");
        grid.add(createListSection("Skills", student.getSkills(), MaterialDesignL.LIGHTBULB_ON), "growx, hmin 210, wrap");
        grid.add(createAcademicSection(), "growx, hmin 170");
        grid.add(createListSection("Credentials", student.getCertifications(), MaterialDesignC.CERTIFICATE), "growx, hmin 170");

        return grid;
    }

    private JPanel createAboutSection() {
        JPanel card = sectionCard("About", MaterialDesignA.ACCOUNT_DETAILS);
        JLabel text = new JLabel("<html>" + safeText(student.getBio()) + "</html>");
        text.setFont(new Font("Inter", Font.PLAIN, 14));
        text.setForeground(ThemeManager.textSecondary());
        card.add(text, "growx");
        return card;
    }

    private JPanel createAnalyticsSection() {
        JPanel card = sectionCard("Academic Snapshot", MaterialDesignC.CHART_BAR);
        JPanel metrics = new JPanel(new MigLayout(
                "fillx, ins 0, gap 12",
                "[grow,fill][grow,fill][grow,fill]",
                "[]"
        ));
        metrics.setOpaque(false);
        metrics.add(createMetric("CGPA", String.format("%.2f", student.getCgpa()), MaterialDesignC.CHART_BAR));
        metrics.add(createMetric("Attendance", String.format("%.1f%%", student.getAttendancePercentage()), MaterialDesignC.CHECK_CIRCLE));
        metrics.add(createMetric("XP", String.valueOf(student.getXpPoints()), MaterialDesignT.TROPHY));
        card.add(metrics, "growx");
        return card;
    }

    private JPanel createReadinessSection() {
        JPanel card = sectionCard("Placement Readiness", MaterialDesignB.BRIEFCASE_ACCOUNT);
        double score = aiEngine.calculatePlacementReadiness(student);
        JProgressBar readiness = new JProgressBar(0, 100);
        readiness.setValue((int) score);
        readiness.setStringPainted(true);
        readiness.setForeground(ThemeManager.SUCCESS_GREEN);
        card.add(readiness, "growx");

        JLabel role = new JLabel("Target role: " + aiEngine.getCareerRecommendation(student));
        role.setFont(new Font("Inter", Font.PLAIN, 13));
        role.setForeground(ThemeManager.textSecondary());
        card.add(role, "growx");
        return card;
    }

    private JPanel createAIInsightsSection() {
        JPanel card = sectionCard("Academic Copilot", MaterialDesignR.ROBOT);
        JLabel advice = new JLabel("<html>" + safeText(aiEngine.getPersonalizedStudyPlan(student)) + "</html>");
        advice.setFont(new Font("Inter", Font.PLAIN, 13));
        advice.setForeground(ThemeManager.textSecondary());
        card.add(advice, "growx");
        return card;
    }

    private JPanel createListSection(String title, List<String> items, Enum<?> icon) {
        JPanel card = sectionCard(title, icon);
        if (items == null || items.isEmpty()) {
            card.add(emptyLabel("No records available."), "growx");
            return card;
        }

        for (String item : items) {
            JPanel row = new JPanel(new MigLayout("fillx, ins 7 0, gap 10", "[][grow,fill]", "[]"));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.border()));
            row.add(new JLabel(FontIcon.of(MaterialDesignC.CIRCLE, 7, ThemeManager.ACCENT_BLUE)), "w 16!");
            row.add(rowText(item), "growx");
            card.add(row, "growx");
        }
        return card;
    }

    private JPanel createAcademicSection() {
        JPanel card = sectionCard("Academic Standing", MaterialDesignB.BOOK_OPEN_PAGE_VARIANT);
        card.add(detailRow("Program", student.getDepartment() + " Undergraduate"), "growx");
        card.add(detailRow("Current Semester", String.valueOf(student.getSemester())), "growx");
        card.add(detailRow("Fee Status", student.getFeeStatus()), "growx");
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

    private JPanel createMetric(String label, String value, Enum<?> icon) {
        JPanel metric = new JPanel(new MigLayout("ins 0, gap 8", "[][grow]", "[]4[]"));
        metric.setOpaque(false);
        metric.add(new JLabel(FontIcon.of((org.kordamp.ikonli.Ikon) icon, 22, ThemeManager.ACCENT_BLUE)), "spany 2, w 28!");

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Inter", Font.BOLD, 22));
        valueLabel.setForeground(ThemeManager.textPrimary());
        metric.add(valueLabel, "growx, wrap");

        JLabel labelText = new JLabel(label);
        labelText.setFont(new Font("Inter", Font.PLAIN, 12));
        labelText.setForeground(ThemeManager.textSecondary());
        metric.add(labelText, "growx");
        return metric;
    }

    private JPanel detailRow(String label, String value) {
        JPanel row = new JPanel(new MigLayout("fillx, ins 7 0", "[grow,fill][right]", "[]"));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.border()));
        row.add(rowText(label), "growx");
        JLabel valueLabel = rowText(value);
        valueLabel.setFont(new Font("Inter", Font.BOLD, 13));
        row.add(valueLabel);
        return row;
    }

    private JLabel createProfilePhoto(int size) {
        JLabel label = new JLabel();
        label.setHorizontalAlignment(SwingConstants.CENTER);
        label.setVerticalAlignment(SwingConstants.CENTER);
        label.setOpaque(true);
        label.setBackground(ThemeManager.surfaceAlt());
        label.setBorder(BorderFactory.createLineBorder(ThemeManager.border(), 1));

        String path = student.getProfilePicturePath();
        if (path != null && !path.isBlank() && new File(path).isFile()) {
            ImageIcon imageIcon = new ImageIcon(path);
            Image scaled = imageIcon.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
            label.setIcon(new ImageIcon(scaled));
        } else {
            label.setIcon(FontIcon.of(MaterialDesignA.ACCOUNT_CIRCLE, size - 8, new Color(156, 166, 176)));
        }
        return label;
    }

    private JLabel summaryLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setFont(new Font("Inter", Font.BOLD, 12));
        label.setForeground(Color.WHITE);
        label.setBackground(color);
        label.setBorder(BorderFactory.createEmptyBorder(5, 9, 5, 9));
        return label;
    }

    private JLabel rowText(String text) {
        JLabel label = new JLabel(text == null || text.isBlank() ? "-" : text);
        label.setFont(new Font("Inter", Font.PLAIN, 13));
        label.setForeground(ThemeManager.textSecondary());
        return label;
    }

    private JLabel emptyLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Inter", Font.PLAIN, 13));
        label.setForeground(ThemeManager.textSecondary());
        return label;
    }

    private String safeText(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
