package com.university.gui;

import com.university.authentication.SessionManager;
import com.university.authentication.User;

import javax.swing.*;
import java.awt.*;

public class MainDashboard extends JFrame {
    private JPanel sidebar;
    private JPanel contentArea;
    private CardLayout cardLayout;

    public MainDashboard() {
        User user = SessionManager.getCurrentUser();
        setTitle("University ERP - " + user.getRole() + " Dashboard (" + user.getUsername() + ")");
        setSize(1100, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // Sidebar
        sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(new Color(44, 62, 80));
        sidebar.setPreferredSize(new Dimension(220, getHeight()));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.DARK_GRAY));

        // Content Area
        cardLayout = new CardLayout();
        contentArea = new JPanel(cardLayout);
        contentArea.setBackground(new Color(236, 240, 241));

        add(sidebar, BorderLayout.WEST);
        add(contentArea, BorderLayout.CENTER);

        // Status Bar
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBackground(new Color(52, 73, 94));
        statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        JLabel statusLabel = new JLabel("System Status: Online | RMI Server: Running | Threads: Active");
        statusLabel.setForeground(Color.LIGHT_GRAY);
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.SOUTH);

        setupNavigation(user.getRole());
    }

    private void setupNavigation(String role) {
        JLabel logo = new JLabel("UNIV ERP", JLabel.CENTER);
        logo.setForeground(new Color(26, 188, 156));
        logo.setFont(new Font("SansSerif", Font.BOLD, 24));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        logo.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        sidebar.add(logo);

        addSidebarButton("Dashboard", "DASHBOARD");
        contentArea.add(createWelcomePanel(role), "DASHBOARD");

        if ("Admin".equals(role)) {
            addSidebarButton("Manage Students", "STUDENTS");
            contentArea.add(new AdminStudentPanel(), "STUDENTS");

            addSidebarButton("Manage Courses", "COURSES");
            contentArea.add(new CourseManagementPanel(), "COURSES");

            addSidebarButton("AI Risk Analysis", "AI_RISK");
            contentArea.add(new AIRiskPanel(), "AI_RISK");
        } else if ("Faculty".equals(role)) {
            addSidebarButton("Mark Attendance", "ATTENDANCE");
            contentArea.add(new FacultyAttendancePanel(), "ATTENDANCE");
            
            addSidebarButton("Upload Marks", "MARKS");
            contentArea.add(new FacultyMarksPanel(), "MARKS");
        } else if ("Student".equals(role)) {
            addSidebarButton("My Profile", "PROFILE");
            contentArea.add(new StudentProfilePanel(), "PROFILE");

            addSidebarButton("My Results", "RESULTS");
            contentArea.add(new StudentResultsPanel(), "RESULTS");
        }

        addSidebarButton("Library", "LIBRARY");
        contentArea.add(new LibraryPanel(), "LIBRARY");

        addSidebarButton("Logout", "LOGOUT");
    }

    private void addSidebarButton(String text, String cardName) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(200, 40));
        btn.setBackground(new Color(44, 62, 80));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setAlignmentX(Component.CENTER_ALIGNMENT);

        btn.addActionListener(e -> {
            if ("LOGOUT".equals(cardName)) {
                SessionManager.endSession();
                // Re-open login (would need access to AuthenticationManager)
                // For simplicity, just exit or re-run main
                dispose();
                // This is a bit hacky, normally we'd pass a callback or reference
                System.exit(0); 
            } else {
                cardLayout.show(contentArea, cardName);
            }
        });

        sidebar.add(Box.createVerticalStrut(10));
        sidebar.add(btn);
    }

    private JPanel createWelcomePanel(String role) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(236, 240, 241));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0;
        
        JLabel welcomeLabel = new JLabel("Welcome back, " + SessionManager.getCurrentUser().getUsername() + "!");
        welcomeLabel.setFont(new Font("Arial", Font.BOLD, 32));
        welcomeLabel.setForeground(new Color(44, 62, 80));
        panel.add(welcomeLabel, gbc);

        gbc.gridy = 1;
        JLabel subLabel = new JLabel("You are logged in as: " + role);
        subLabel.setFont(new Font("Arial", Font.ITALIC, 18));
        subLabel.setForeground(new Color(127, 140, 141));
        panel.add(subLabel, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(30, 0, 0, 0);
        JLabel systemMsg = new JLabel("<html><center>The AI Recommendation engine is running in the background.<br>All background processing tasks are active.</center></html>", JLabel.CENTER);
        systemMsg.setFont(new Font("Arial", Font.PLAIN, 14));
        systemMsg.setForeground(new Color(52, 152, 219));
        panel.add(systemMsg, gbc);

        return panel;
    }
}
