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
    }

    private JPanel createHeader(User user) {
        JPanel header = new JPanel(new MigLayout("ins 0 35 0 35, fillx, aligny center", "[grow]push[]25[]", "[]"));
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

        JLabel userProfile = new JLabel(user.getUsername());
        userProfile.setFont(new Font("Inter", Font.BOLD, 15));
        userProfile.setIcon(FontIcon.of(MaterialDesignA.ACCOUNT_CIRCLE, 35, ThemeManager.ACCENT_BLUE));
        userProfile.setIconTextGap(12);
        header.add(userProfile);

        return header;
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
