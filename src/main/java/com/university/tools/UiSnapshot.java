package com.university.tools;

import com.university.db.DatabaseManager;
import com.university.courses.CourseManager;
import com.university.students.StudentManager;
import com.university.lms.AssignmentManager;
import com.university.models.Assignment;
import com.university.models.Student;
import com.university.erp.security.FacultyUser;
import com.university.erp.security.SessionManager;
import com.university.erp.security.StudentUser;
import com.university.erp.gui.faculty.FacultyLMSPanel;
import com.university.erp.gui.student.StudentDashboard;
import com.university.erp.gui.admin.AdminCommandCenter;
import com.university.erp.gui.theme.ThemeManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Date;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class UiSnapshot {
    public static void main(String[] args) throws Exception {
        // Initialize DB schema
        DatabaseManager.initializeSchema();

        // Create managers
        CourseManager cm = new CourseManager();
        StudentManager sm = new StudentManager();
        AssignmentManager am = new AssignmentManager();

        // Create demo course and student
        try {
            cm.addCourse(new com.university.models.Course("C-FINAL", "Final Demo Course", 3));
        } catch (Exception ignored) {}

        Student s1 = new Student("SF01", "Demo Student", "demo1@example.com", "", "Computer", 3);
        try { sm.addStudent(s1); } catch (Exception ignored) {}

        // Enroll
        try { cm.enrollStudent("C-FINAL", s1.getId()); } catch (Exception ignored) {}

        // Create faculty user and session
        FacultyUser faculty = new FacultyUser("faculty_final", "pass", "F-FINAL");
        // persist faculty row if missing
        try (java.sql.Connection conn = DatabaseManager.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement("INSERT OR IGNORE INTO faculty (id, name, email) VALUES (?, ?, ?)") ) {
            pstmt.setString(1, "F-FINAL");
            pstmt.setString(2, "Demo Faculty");
            pstmt.setString(3, "faculty@example.com");
            pstmt.executeUpdate();
        }

        SessionManager.startSession(faculty);

        // Faculty creates an assignment
        Assignment a = new Assignment("AF-1", "C-FINAL", "Final Demo Assignment", "Please submit demo.",
                Date.from(LocalDateTime.now().plusDays(3).atZone(ZoneId.systemDefault()).toInstant()), 100);
        try { am.createAssignment(a); } catch (Exception ignored) {}
        // publish events if UIEventBus available
        try {
            com.university.erp.gui.UIEventBus.publish("ASSIGNMENT_PUBLISHED", a);
            com.university.erp.gui.UIEventBus.publish("NOTIFICATION_CREATED", a);
        } catch (Throwable t) { /* ignore if not present */ }

        // Now create UI components and render to images
        ThemeManager.initialize();
        SwingUtilities.invokeAndWait(() -> {
            try {
                // Faculty panel snapshot
                FacultyLMSPanel facultyPanel = new FacultyLMSPanel();
                saveComponentAsImage(facultyPanel, "faculty_panel.png", 1000, 700);

                // Student dashboard snapshot
                StudentUser stuUser = new StudentUser("student_final", "pass", s1.getId());
                SessionManager.startSession(stuUser);
                StudentDashboard sd = new StudentDashboard(s1);
                saveComponentAsImage(sd, "student_dashboard.png", 1000, 800);

                // Admin dashboard snapshot
                com.university.erp.security.User adminUser = new com.university.erp.security.FacultyUser("admin", "pass", "F-FINAL");
                SessionManager.startSession(adminUser);
                // Admin dashboard snapshot - draw a simple image with metrics (avoid Swing chart issues)
                int totalStudents = com.university.erp.analytics.ExecutiveAnalytics.getCount("students");
                int totalFaculty = com.university.erp.analytics.ExecutiveAnalytics.getCount("faculty");
                int totalCourses = com.university.erp.analytics.ExecutiveAnalytics.getCount("courses");
                int totalAssignments = com.university.erp.analytics.ExecutiveAnalytics.getCount("assignments");
                double avgAttendance = com.university.erp.analytics.ExecutiveAnalytics.getAverageAttendance();

                int w = 1000, h = 400;
                BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = img.createGraphics();
                g.setColor(Color.WHITE);
                g.fillRect(0, 0, w, h);
                g.setColor(Color.DARK_GRAY);
                g.setFont(new Font("SansSerif", Font.BOLD, 24));
                g.drawString("Admin Summary", 20, 40);
                g.setFont(new Font("SansSerif", Font.PLAIN, 18));
                g.drawString("Total Students: " + totalStudents, 20, 100);
                g.drawString("Total Faculty: " + totalFaculty, 20, 140);
                g.drawString("Total Courses: " + totalCourses, 20, 180);
                g.drawString("Total Assignments: " + totalAssignments, 20, 220);
                g.drawString(String.format("Avg Attendance: %.2f", avgAttendance), 20, 260);
                g.dispose();
                ImageIO.write(img, "png", new File("admin_dashboard.png"));

                System.out.println("Screenshots saved: faculty_panel.png, student_dashboard.png, admin_dashboard.png");
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void saveComponentAsImage(JComponent comp, String fileName, int width, int height) throws Exception {
        comp.setSize(width, height);
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        comp.doLayout();
        comp.printAll(g2);
        g2.dispose();
        File out = new File(fileName);
        ImageIO.write(img, "png", out);
    }
}
