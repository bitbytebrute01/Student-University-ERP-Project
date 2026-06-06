package com.university.erp.gui;

import com.university.erp.security.SessionManager;
import com.university.erp.security.StudentContext;
import com.university.erp.security.User;
import com.university.erp.security.UserRole;
import com.university.erp.gui.theme.ThemeManager;
import com.university.erp.gui.student.*;
import com.university.erp.gui.admin.AdminCommandCenter;
import com.university.erp.gui.faculty.*;
import com.university.models.Student;
import net.miginfocom.swing.MigLayout;
import org.kordamp.ikonli.materialdesign2.*;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;

public class MainFrame extends JFrame {
    private JPanel sidebar;
    private JPanel contentArea;
    private CardLayout cardLayout;
    private final java.util.List<Runnable> uiUnsubHandles = new java.util.ArrayList<>();
    private com.university.sync.DBPoller dbPoller;

    public MainFrame() {
        User user = SessionManager.getCurrentUser();
        if (user == null) System.exit(0);

        setTitle("AI University OS 2.0 - Professional ERP Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(1024, 768));
        setLocationRelativeTo(null);
        
        setLayout(new BorderLayout());

        sidebar = createSidebar(user);
        add(sidebar, BorderLayout.WEST);

        JPanel mainStage = new JPanel(new MigLayout("ins 0, gap 0, wrap 1", "[grow, fill]", "[]0[grow, fill]"));
        ThemeManager.stylePage(mainStage);
        
        mainStage.add(createHeader(user), "h 75!");
        
        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        ThemeManager.stylePage(contentArea);
        mainStage.add(contentArea, "grow");

        add(mainStage, BorderLayout.CENTER);

        setupModules(user.getRole());
        ThemeManager.refreshComponentTree(this);

        // Start DB poller for cross-process synchronization (3 second interval)
        try {
            dbPoller = new com.university.sync.DBPoller(3);
            dbPoller.start();
        } catch (Exception e) {
            System.err.println("Failed to start DBPoller: " + e.getMessage());
        }
    }

    private JPanel createHeader(User user) {
        JPanel header = new JPanel(new MigLayout("ins 0 35 0 35, fillx, aligny center", "[grow]push[]12[]25[]", "[]"));
        ThemeManager.styleHeader(header);

        JTextField search = new JTextField("Search resources...");
        search.setPreferredSize(new Dimension(300, 35));
        header.add(search);

        JButton themeBtn = new JButton();
        ThemeManager.styleIconButton(themeBtn, MaterialDesignB.BRIGHTNESS_4, 22);
        themeBtn.setBorderPainted(false);
        themeBtn.setContentAreaFilled(false);
        themeBtn.addActionListener(e -> ThemeManager.toggleTheme());
        header.add(themeBtn);

        // Notification badge
        JLabel notifBadge = new JLabel();
        notifBadge.setFont(new Font("Inter", Font.BOLD, 13));
        notifBadge.setOpaque(true);
        notifBadge.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        header.add(notifBadge);

        JLabel userProfile = new JLabel(user.getUsername());
        userProfile.setFont(new Font("Inter", Font.BOLD, 15));
        userProfile.setIcon(FontIcon.of(MaterialDesignA.ACCOUNT_CIRCLE, 35, ThemeManager.ACCENT_BLUE));
        userProfile.setIconTextGap(12);
        header.add(userProfile);

        // initialize badge value and subscribe to UI events
        refreshHeaderNotifications(notifBadge);
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("ASSIGNMENT_PUBLISHED", payload -> refreshHeaderNotifications(notifBadge)));
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("ASSIGNMENT_GRADED", payload -> refreshHeaderNotifications(notifBadge)));
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("NOTIFICATION_CREATED", payload -> refreshHeaderNotifications(notifBadge)));
        uiUnsubHandles.add(com.university.erp.gui.UIEventBus.subscribeWithHandle("SUBMISSION_CREATED", payload -> refreshHeaderNotifications(notifBadge)));

        // show popup on click
        notifBadge.setCursor(new Cursor(Cursor.HAND_CURSOR));
        notifBadge.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showNotificationsPopup(notifBadge);
            }
        });

        return header;
    }

    // Minimal HTML-escape to keep popup safe
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;").replaceAll("\n", "<br/>");
    }

    @Override
    public void dispose() {
        // unsubscribe all UI listeners
       for (Runnable r : uiUnsubHandles) {
           try { r.run(); } catch (Exception ignored) {}
       }
       uiUnsubHandles.clear();
       // stop DB poller
       try { if (dbPoller != null) dbPoller.stop(); } catch (Exception ignored) {}
       super.dispose();
    }

    private void showNotificationsPopup(JLabel notifBadge) {
        com.university.erp.security.User user = SessionManager.getCurrentUser();
        if (user == null) return;
        JPopupMenu popup = new JPopupMenu();
        try {
            com.university.lms.AssignmentManager am = new com.university.lms.AssignmentManager();
            if (user.getRole() == com.university.erp.security.UserRole.STUDENT) {
                String studentId = user.getRefId() != null ? user.getRefId() : user.getUsername();
                java.util.List<com.university.models.AssignmentNotification> notes = am.getUnreadAssignmentNotifications(studentId);
                if (notes.isEmpty()) {
                    JMenuItem empty = new JMenuItem("No unread notifications");
                    empty.setEnabled(false);
                    popup.add(empty);
                } else {
                    for (com.university.models.AssignmentNotification n : notes) {
                        String html = "<html><b>" + escapeHtml(n.getTitle()) + "</b><br/><small>" + escapeHtml(n.getMessage()) + "</small></html>";
                        JMenuItem item = new JMenuItem(html);
                        item.addActionListener(ae -> {
                            try {
                                am.markAssignmentNotificationRead(studentId, n.getAssignmentId());
                                refreshHeaderNotifications(notifBadge);
                                // open LMS view
                                cardLayout.show(contentArea, "LMS");
                            } catch (Exception ex) {
                                JOptionPane.showMessageDialog(this, "Notification Error: " + ex.getMessage());
                            }
                        });
                        popup.add(item);
                    }
                }
            } else if (user.getRole() == com.university.erp.security.UserRole.FACULTY) {
                JMenuItem open = new JMenuItem("Open Grading Queue");
                open.addActionListener(ae -> cardLayout.show(contentArea, "FACULTY_LMS"));
                popup.add(open);
                JMenuItem refresh = new JMenuItem("Refresh metrics");
                refresh.addActionListener(ae -> UIEventBus.publish("ASSIGNMENT_PUBLISHED", null));
                popup.add(refresh);
            } else {
                JMenuItem open = new JMenuItem("Open Admin Dashboard");
                open.addActionListener(ae -> cardLayout.show(contentArea, "DASHBOARD"));
                popup.add(open);
            }
        } catch (Exception e) {
            JMenuItem err = new JMenuItem("Error loading notifications");
            err.setEnabled(false);
            popup.add(err);
        }
        popup.show(notifBadge, 0, notifBadge.getHeight());
    }

    private void refreshHeaderNotifications(JLabel notifBadge) {
        SwingUtilities.invokeLater(() -> {
            com.university.erp.security.User user = SessionManager.getCurrentUser();
            if (user == null) {
                notifBadge.setVisible(false);
                return;
            }
            try {
                com.university.lms.AssignmentManager am = new com.university.lms.AssignmentManager();
                int count = 0;
                switch (user.getRole()) {
                    case STUDENT -> count = am.getUnreadAssignmentNotificationCount(user.getRefId() != null ? user.getRefId() : user.getUsername());
                    case FACULTY -> count = am.getFacultyAssignmentMetrics(user.getRefId() != null ? user.getRefId() : user.getUsername(), false).getPendingReviews();
                    case ADMIN -> count = com.university.erp.analytics.ExecutiveAnalytics.getCount("submissions");
                    default -> count = 0;
                }
                if (count > 0) {
                    notifBadge.setText("Notifications: " + count);
                    notifBadge.setBackground(new Color(255, 245, 230));
                    notifBadge.setForeground(ThemeManager.WARNING_ORANGE);
                    notifBadge.setVisible(true);
                } else {
                    notifBadge.setText("No new notifications");
                    notifBadge.setBackground(ThemeManager.surfaceAlt());
                    notifBadge.setForeground(ThemeManager.textSecondary());
                    notifBadge.setVisible(true);
                }
            } catch (Exception e) {
                notifBadge.setText("No new notifications");
                notifBadge.setBackground(ThemeManager.surfaceAlt());
                notifBadge.setForeground(ThemeManager.textSecondary());
            }
        });
    }

    private JPanel createSidebar(User user) {
        JPanel panel = new JPanel(new MigLayout("wrap 1, ins 25 15 20 15, gap 10", "[180!]", "[]30[]push[]"));
        ThemeManager.styleSidebar(panel);

        JLabel logo = new JLabel("AI UNIVERSITY");
        logo.setForeground(Color.WHITE);
        logo.setFont(new Font("Inter", Font.BOLD, 16));
        logo.setIcon(FontIcon.of(MaterialDesignA.ACCOUNT_CIRCLE, 24, ThemeManager.SUCCESS_GREEN));
        logo.setIconTextGap(10);
        panel.add(logo, "align center, wrap");

        JPanel nav = new JPanel(new MigLayout("wrap 1, ins 0, gap 5", "[grow]", ""));
        nav.setOpaque(false);

        if (user.getRole() == UserRole.ADMIN) {
            addNavButton(nav, "Dashboard", MaterialDesignV.VIEW_DASHBOARD, "DASHBOARD");
            addNavButton(nav, "Students", MaterialDesignA.ACCOUNT_GROUP, "ADMIN_STUDENTS");
            addNavButton(nav, "Courses", MaterialDesignB.BOOK_OPEN_PAGE_VARIANT, "ADMIN_COURSES");
            addNavButton(nav, "Assignments", MaterialDesignC.CLIPBOARD_CHECK, "ADMIN_LMS");
            addNavButton(nav, "Attendance", MaterialDesignC.CHART_TIMELINE_VARIANT, "ADMIN_ATTENDANCE");
        } else if (user.getRole() == UserRole.FACULTY) {
            addNavButton(nav, "Overview", MaterialDesignV.VIEW_DASHBOARD, "DASHBOARD");
            addNavButton(nav, "Courses", MaterialDesignB.BOOK_OPEN_PAGE_VARIANT, "FACULTY_COURSES");
            addNavButton(nav, "Assignments", MaterialDesignC.CLIPBOARD_CHECK, "FACULTY_LMS");
            addNavButton(nav, "Projects", MaterialDesignP.PROJECTOR_SCREEN, "FACULTY_PROJECTS");
        } else {
            addNavButton(nav, "Dashboard", MaterialDesignV.VIEW_DASHBOARD, "DASHBOARD");
            addNavButton(nav, "Hub", MaterialDesignA.ACCOUNT_DETAILS, "PROFILE");
            addNavButton(nav, "Edit", MaterialDesignA.ACCOUNT_EDIT, "EDIT_PROFILE");
            addNavButton(nav, "Projects", MaterialDesignC.CLIPBOARD_PLAY_OUTLINE, "PROJECTS");
            addNavButton(nav, "LMS", MaterialDesignL.LAPTOP, "LMS");
            addNavButton(nav, "Certs", MaterialDesignC.CERTIFICATE, "CERTS");
            addNavButton(nav, "Career", MaterialDesignB.BRIEFCASE_ACCOUNT, "PLACEMENT");
            addNavButton(nav, "Trophy", MaterialDesignT.TROPHY, "GAMIFICATION");
        }

        addNavButton(nav, "Library", MaterialDesignL.LIBRARY_SHELVES, "LIBRARY");
        panel.add(nav, "growx");

        JButton logout = new JButton("Logout");
        logout.putClientProperty("JButton.buttonType", "roundRect");
        logout.setBackground(new Color(231, 76, 60));
        logout.setForeground(Color.WHITE);
        logout.addActionListener(e -> {
            SessionManager.endSession();
            dispose();
            System.exit(0);
        });
        panel.add(logout, "growx, h 40!");

        return panel;
    }

    private void addNavButton(JPanel parent, String text, Enum icon, String card) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Inter", Font.PLAIN, 14));
        btn.setIcon(FontIcon.of((org.kordamp.ikonli.Ikon)icon, 18, new Color(210, 210, 210)));
        btn.setForeground(new Color(210, 210, 210));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setIconTextGap(10);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addActionListener(e -> cardLayout.show(contentArea, card));
        parent.add(btn, "growx, h 40!");
    }

    private void setupModules(UserRole role) {
        if (role == UserRole.ADMIN) {
            contentArea.add(new AdminCommandCenter(), "DASHBOARD");
            contentArea.add(new com.university.gui.AdminStudentPanel(), "ADMIN_STUDENTS");
            contentArea.add(new com.university.erp.gui.faculty.CourseManagementPanel(), "ADMIN_COURSES");
            contentArea.add(new FacultyLMSPanel(), "ADMIN_LMS");
            contentArea.add(new com.university.erp.gui.admin.AttendanceDashboard(), "ADMIN_ATTENDANCE");
        } else if (role == UserRole.FACULTY) {
            contentArea.add(new FacultyDashboard(), "DASHBOARD");
            contentArea.add(new com.university.erp.gui.faculty.CourseManagementPanel(), "FACULTY_COURSES");
            contentArea.add(new FacultyLMSPanel(), "FACULTY_LMS");
            contentArea.add(new FacultyProjectHub(), "FACULTY_PROJECTS");
        } else {
            Student student = StudentContext.requireCurrentStudent();
            StudentDashboard studentDashboard = new StudentDashboard(student);
            contentArea.add(studentDashboard, "DASHBOARD");
            contentArea.add(new StudentLinkedInProfile(student), "PROFILE");
            contentArea.add(new ProfileEditor(student, updated -> studentDashboard.refreshStudent(StudentContext.requireCurrentStudent())), "EDIT_PROFILE");
            contentArea.add(new ProjectPanel(student), "PROJECTS");
            contentArea.add(new AssignmentHub(student), "LMS");
            contentArea.add(new CertificationHub(student), "CERTS");
            contentArea.add(new PlacementProfilePanel(student), "PLACEMENT");
            contentArea.add(new GamificationPanel(student), "GAMIFICATION");
        }
        
        contentArea.add(new com.university.gui.LibraryPanel(), "LIBRARY");
    }
}
