package com.university.tools;

import com.university.db.DatabaseManager;
import com.university.courses.CourseManager;
import com.university.students.StudentManager;
import com.university.lms.AssignmentManager;
import com.university.models.Assignment;
import com.university.models.Student;
import com.university.models.Submission;
import com.university.erp.security.FacultyUser;
import com.university.erp.security.SessionManager;
import com.university.erp.security.StudentUser;
import com.university.erp.gui.faculty.FacultyLMSPanel;
import com.university.erp.gui.student.StudentDashboard;
import com.university.erp.gui.theme.ThemeManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

public class UiFlowRunner {
    public static void main(String[] args) throws Exception {
        DatabaseManager.initializeSchema();
        ThemeManager.initialize();

        CourseManager cm = new CourseManager();
        StudentManager sm = new StudentManager();
        AssignmentManager am = new AssignmentManager();

        // Create course
        try { cm.addCourse(new com.university.models.Course("C-FLOW", "Flow Course", 3)); } catch (Exception ignored) {}

        // Create faculty
        FacultyUser faculty = new FacultyUser("faculty_flow", "pass", "F-FLOW");
        try (java.sql.Connection conn = DatabaseManager.getConnection();
             java.sql.PreparedStatement pstmt = conn.prepareStatement("INSERT OR IGNORE INTO faculty (id, name, email) VALUES (?, ?, ?)") ) {
            pstmt.setString(1, "F-FLOW");
            pstmt.setString(2, "Flow Faculty");
            pstmt.setString(3, "flow.faculty@example.com");
            pstmt.executeUpdate();
        }

        // Create student
        Student s = new Student("S-FLOW", "Flow Student", "flow.student@example.com", "", "CS", 3);
        try { sm.addStudent(s); } catch (Exception ignored) {}

        // Enroll student
        try { cm.enrollStudent("C-FLOW", s.getId()); } catch (Exception ignored) {}

        // Faculty session: create assignment
        SessionManager.startSession(faculty);
        Assignment assignment = new Assignment("A-FLOW-1", "C-FLOW", "Flow Assignment", "Please submit flow assignment.",
                Date.from(LocalDateTime.now().plusDays(2).atZone(ZoneId.systemDefault()).toInstant()), 50);
        am.createAssignment(assignment);
        // publish events
        try { com.university.erp.gui.UIEventBus.publish("ASSIGNMENT_PUBLISHED", assignment); } catch (Throwable ignored) {}

        // Capture faculty view after publish
        SwingUtilities.invokeAndWait(() -> {
            try {
                FacultyLMSPanel panel = new FacultyLMSPanel();
                save(panel, "step1_faculty_after_publish.png", 1000, 700);
            } catch (Exception e) { e.printStackTrace(); }
        });

        Thread.sleep(300);

        // Student session: view dashboard (should see notification/assignment)
        StudentUser stuUser = new StudentUser("student_flow", "pass", s.getId());
        SessionManager.startSession(stuUser);
        SwingUtilities.invokeAndWait(() -> {
            try {
                StudentDashboard sd = new StudentDashboard(s);
                save(sd, "step2_student_dashboard_after_publish.png", 1000, 800);
            } catch (Exception e) { e.printStackTrace(); }
        });

        Thread.sleep(300);

        // Student submits assignment (text-only)
        Submission sub = new Submission(null, assignment.getId(), s.getId(), null);
        sub.setSubmissionText("This is a demo submission for flow test.");
        am.submitAssignment(sub);
        try { com.university.erp.gui.UIEventBus.publish("SUBMISSION_CREATED", sub); } catch (Throwable ignored) {}

        // Capture student view after submit
        SwingUtilities.invokeAndWait(() -> {
            try {
                StudentDashboard sd2 = new StudentDashboard(s);
                save(sd2, "step3_student_after_submit.png", 1000, 800);
            } catch (Exception e) { e.printStackTrace(); }
        });

        Thread.sleep(300);

        // Faculty view: see submission
        SessionManager.startSession(faculty);
        SwingUtilities.invokeAndWait(() -> {
            try {
                FacultyLMSPanel panel = new FacultyLMSPanel();
                save(panel, "step4_faculty_sees_submission.png", 1000, 700);
            } catch (Exception e) { e.printStackTrace(); }
        });

        // Grade submission
        java.util.List<com.university.models.Submission> subs = am.getSubmissionsByAssignment(assignment.getId());
        if (!subs.isEmpty()) {
            String subId = subs.get(0).getId();
            am.gradeSubmission(subId, 45.0, "Good work");
            try { com.university.erp.gui.UIEventBus.publish("ASSIGNMENT_GRADED", subs.get(0)); } catch (Throwable ignored) {}
        }

        Thread.sleep(300);

        // Student view: see grade
        SessionManager.startSession(stuUser);
        SwingUtilities.invokeAndWait(() -> {
            try {
                StudentDashboard sd3 = new StudentDashboard(s);
                save(sd3, "step5_student_sees_grade.png", 1000, 800);
            } catch (Exception e) { e.printStackTrace(); }
        });

        System.out.println("Flow screenshots generated: step1..step5 PNG files");
    }

    private static void save(JComponent comp, String fileName, int w, int h) throws Exception {
        comp.setSize(w, h);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        comp.doLayout();
        comp.printAll(g);
        g.dispose();
        ImageIO.write(img, "png", new File(fileName));
    }
}
