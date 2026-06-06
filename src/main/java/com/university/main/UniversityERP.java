package com.university.main;

import com.university.ai.AIRecommendationEngine;
import com.university.attendance.AttendanceManager;
import com.university.erp.security.*;
import com.university.erp.gui.student.StudentDashboard;
import com.university.erp.dao.CertificationDAO;
import com.university.erp.dao.ProjectDAO;
import com.university.erp.gui.theme.ThemeManager;
import com.university.courses.CourseManager;
import com.university.examination.ExamManager;
import com.university.faculty.FacultyManager;
import com.university.fees.FeeManager;
import com.university.hostel.HostelManager;
import com.university.library.LibraryManager;
import com.university.lms.AssignmentManager;
import com.university.models.Assignment;
import com.university.models.Certification;
import com.university.models.Course;
import com.university.models.Faculty;
import com.university.models.Project;
import com.university.models.Student;
import com.university.models.Submission;
import com.university.placement.PlacementManager;
import com.university.rmi.server.UniversityServer;
import com.university.students.StudentManager;
import com.university.utils.MediaManager;
import com.university.threads.AttendanceThread;
import com.university.threads.FeeProcessingThread;
import com.university.threads.NotificationThread;

import java.util.Scanner;

import com.university.gui.LoginFrame;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class UniversityERP {
    private static Scanner scanner = new Scanner(System.in);

    // Managers
    private static AuthenticationManager authManager = new AuthenticationManager();
    private static StudentManager studentManager = new StudentManager();
    private static FacultyManager facultyManager = new FacultyManager();
    private static CourseManager courseManager = new CourseManager();
    private static LibraryManager libraryManager = new LibraryManager();
    private static HostelManager hostelManager = new HostelManager();
    private static PlacementManager placementManager = new PlacementManager();
    private static AssignmentManager assignmentManager = new AssignmentManager();
    private static ProjectDAO projectDAO = new ProjectDAO();
    private static CertificationDAO certificationDAO = new CertificationDAO();
    
    private static AttendanceManager attendanceManager = new AttendanceManager(studentManager);
    private static ExamManager examManager = new ExamManager(studentManager);
    private static FeeManager feeManager = new FeeManager(studentManager);
    private static AIRecommendationEngine aiEngine = new AIRecommendationEngine();

    public static void main(String[] args) {
        System.out.println("Starting AI-Powered Digital University Operating System (ERP 2.0)...");

        // Initialize Relational Database
        com.university.db.DatabaseManager.initializeSchema();
        // com.university.db.DataMigrator.migrateAll();

        if (isLoginVerificationMode(args) || isWorkflowVerificationMode(args)) {
            seedData();
            verifyLogin("alice", "pass");
            verifyLogin("admin", "admin123");
            verifyLogin("faculty", "pass");
            if (isWorkflowVerificationMode(args)) {
                verifyStudentWorkflows();
            }
            System.out.println("Login verification completed successfully.");
            return;
        }

        // Initialize Modern Theme Engine
        ThemeManager.initialize();

        // Start background threads
        new AttendanceThread(studentManager).start();
        new FeeProcessingThread(studentManager).start();
        NotificationThread notifThread = new NotificationThread("Global");
        notifThread.setDaemon(true);
        notifThread.start();

        // Start RMI Server in background
        new Thread(() -> UniversityServer.startServer(studentManager, libraryManager, placementManager)).start();

        // Seed data if empty
        seedData();

        // Launch GUI
        SwingUtilities.invokeLater(() -> {
            new LoginFrame(authManager).setVisible(true);
        });
    }

    private static boolean isLoginVerificationMode(String[] args) {
        return args != null && args.length > 0 && "--verify-login".equals(args[0]);
    }

    private static boolean isWorkflowVerificationMode(String[] args) {
        return args != null && args.length > 0 && "--verify-workflows".equals(args[0]);
    }

    private static void verifyLogin(String username, String password) {
        try {
            User user = authManager.authenticate(username, password);
            if (user.getRole() == UserRole.STUDENT) {
                SessionManager.startSession(user);
                StudentContext.requireCurrentStudent();
                SessionManager.endSession();
            }
            System.out.println("Verified login: " + username + " (" + user.getRole().getLabel() + ")");
        } catch (Exception e) {
            throw new IllegalStateException("Login verification failed for " + username + ": " + e.getMessage(), e);
        }
    }

    private static void verifyStudentWorkflows() {
        try {
            ThemeManager.initialize();

            User aliceUser = authManager.authenticate("alice", "pass");
            SessionManager.startSession(aliceUser);
            Student alice = StudentContext.requireCurrentStudent();
            StudentDashboard dashboard = new StudentDashboard(alice);

            Path sourceImage = Files.createTempFile("alice-profile-", ".png");
            writeVerificationImage(sourceImage);
            String tempProfile = MediaManager.saveTempImage(sourceImage.toFile());
            // finalize during update
            alice.setProfilePicturePath(tempProfile);
            // finalize and update in one step
            try {
                String finalizedProfile = MediaManager.finalizeUpload(tempProfile, "profiles/" + alice.getId());
                alice.setProfilePicturePath(finalizedProfile);
                studentManager.updateStudent(alice);
            } catch (Exception io) {
                throw new IllegalStateException("Failed to finalize profile during verification: " + io.getMessage(), io);
            }

            Student reloadedAlice = studentManager.searchById("S001");
            if (reloadedAlice == null || reloadedAlice.getProfilePicturePath() == null || !Files.exists(Path.of(reloadedAlice.getProfilePicturePath()))) {
                throw new IllegalStateException("Profile photo did not persist.");
            }
            dashboard.refreshStudent(reloadedAlice);

            User facultyUser = authManager.authenticate("faculty", "pass");
            SessionManager.startSession(facultyUser);

            Course workflowCourse = new Course("C-LMS-VERIFY", "Workflow Verification Course", 3);
            workflowCourse.setAssignedFacultyId(facultyUser.getRefId());
            courseManager.addCourse(workflowCourse);
            courseManager.enrollStudent(workflowCourse.getCourseId(), reloadedAlice.getId());

            Assignment assignment = new Assignment(
                    "A-LMS-VERIFY",
                    workflowCourse.getCourseId(),
                    "Workflow Verification Assignment",
                    "Submit a PDF to verify the full LMS assignment workflow.",
                    new Date(System.currentTimeMillis() + 2L * 60L * 60L * 1000L),
                    100
            );
            assignment.setCreatedBy(facultyUser.getUsername());
            assignment.setStatus("Published");
            assignmentManager.createAssignment(assignment);

            Assignment loadedAssignment = assignmentManager.getAssignmentById(assignment.getId());
            if (loadedAssignment == null) throw new IllegalStateException("Created assignment was not found.");
            if (assignmentManager.getUnreadAssignmentNotificationCount(reloadedAlice.getId()) <= 0) {
                throw new IllegalStateException("Assignment notification was not created for enrolled student.");
            }

            User studentUser = authManager.authenticate("alice", "pass");
            SessionManager.startSession(studentUser);
            List<Assignment> aliceAssignments = assignmentManager.getAssignmentsForStudent(reloadedAlice.getId());
            boolean assignmentVisible = aliceAssignments.stream().anyMatch(a -> assignment.getId().equals(a.getId()));
            if (!assignmentVisible) {
                throw new IllegalStateException("Published assignment is not visible to enrolled student.");
            }

            Path sourcePdf = Files.createTempFile("alice-assignment-", ".pdf");
            Files.writeString(sourcePdf, "%PDF-1.4\nAlice verification submission\n%%EOF\n");
            Path sourceZip = Files.createTempFile("alice-assignment-bundle-", ".zip");
            Files.writeString(sourceZip, "verification bundle");
            // save to temp and submit; AssignmentManager will finalize
            List<String> tempSubmissionPaths = new java.util.ArrayList<>();
            tempSubmissionPaths.add(MediaManager.saveTemp(sourcePdf.toFile()));
            tempSubmissionPaths.add(MediaManager.saveTemp(sourceZip.toFile()));
            Submission submission = new Submission("VERIFY-S001-A-LMS", assignment.getId(), reloadedAlice.getId(), null);
            submission.setSubmissionText("Verification text response for the LMS assignment workflow.");
            submission.setAttachmentPaths(tempSubmissionPaths);
            assignmentManager.submitAssignment(submission);

            Submission loadedSubmission = assignmentManager.getStudentSubmission(reloadedAlice.getId(), assignment.getId());
            if (loadedSubmission == null || loadedSubmission.isGraded() || loadedSubmission.getAttachmentPaths().size() != 2) {
                throw new IllegalStateException("Assignment submission did not persist as submitted.");
            }
            if (!assignmentSubmissionRecordExists(assignment.getId(), reloadedAlice.getId())) {
                throw new IllegalStateException("assignment_submissions record was not created.");
            }
            boolean workflowNotificationStillUnread = assignmentManager.getUnreadAssignmentNotifications(reloadedAlice.getId()).stream()
                    .anyMatch(notification -> assignment.getId().equals(notification.getAssignmentId()));
            if (workflowNotificationStillUnread) {
                throw new IllegalStateException("Assignment notification was not marked read after submission.");
            }

            User adminUser = authManager.authenticate("admin", "admin123");
            SessionManager.startSession(adminUser);
            Path downloadDir = Files.createTempDirectory("admin-submission-download-");
            List<Path> downloaded = assignmentManager.downloadSubmissionFiles(loadedSubmission.getId(), downloadDir.toFile());
            if (downloaded.size() != 2 || downloaded.stream().anyMatch(path -> {
                try {
                    return !Files.exists(path) || Files.size(path) == 0;
                } catch (Exception e) {
                    return true;
                }
            })) {
                throw new IllegalStateException("Admin download did not produce all files.");
            }

            assignmentManager.gradeSubmission(loadedSubmission.getId(), 95.0, "Verified workflow.");

            Submission gradedSubmission = assignmentManager.getStudentSubmission(reloadedAlice.getId(), assignment.getId());
            if (gradedSubmission == null || !gradedSubmission.isGraded() || gradedSubmission.getFeedback() == null) {
                throw new IllegalStateException("Faculty grading did not persist.");
            }

            SessionManager.endSession();
            System.out.println("Verified workflows: course creation, assignment publishing, notification, student visibility, text and multi-file submission, database persistence, admin download, grading.");
        } catch (Exception e) {
            throw new IllegalStateException("Workflow verification failed: " + e.getMessage(), e);
        }
    }

    private static boolean assignmentSubmissionRecordExists(String assignmentId, String studentId) throws Exception {
        String sql = "SELECT COUNT(*) FROM assignment_submissions WHERE assignment_id = ? AND student_id = ? " +
                "AND submission_text IS NOT NULL AND file_path IS NOT NULL";
        try (Connection conn = com.university.db.DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, assignmentId);
            pstmt.setString(2, studentId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static void writeVerificationImage(Path target) throws Exception {
        BufferedImage image = new BufferedImage(48, 48, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0, 115, 177));
        g.fillRect(0, 0, 48, 48);
        g.setColor(Color.WHITE);
        g.fillOval(12, 8, 24, 24);
        g.dispose();
        ImageIO.write(image, "png", target.toFile());
    }

    private static void seedData() {
        authManager.ensureUser(new AdminUser("admin", ""), "admin123", null);
        
        // Explicitly seed Alice
        if(studentManager.searchById("S001") == null) {
            Student alice = new Student("S001", "Alice", "alice@univ.edu", "999-0001", "CS", 1);
            studentManager.addStudent(alice);
        }
        seedAliceProfile();
        authManager.ensureUser(new StudentUser("alice", ""), "pass", "S001");
        
        // Departments
        String[] depts = {"CS", "IS", "ECE", "ME"};
        
        // Seed Faculty
        for(int i=1; i<=5; i++) {
            String id = "F10" + i;
            if(facultyManager.searchById(id) == null) {
                facultyManager.addFaculty(new Faculty(id, "Prof " + i, "prof"+i+"@univ.edu", "555-000"+i, depts[i%4], "Professor", 5+i));
            }
            authManager.ensureUser(new FacultyUser("faculty"+i, ""), "pass", id);
        }
        authManager.ensureUser(new FacultyUser("faculty", ""), "pass", "F101");

        // Seed Courses
        String[] courses = {"Data Structures", "DBMS", "Operating Systems", "Computer Networks", "Software Engineering", "Java Programming"};
        for(int i=0; i<courses.length; i++) {
            String id = "C10" + i;
            if(courseManager.searchById(id) == null) {
                Course c = new Course(id, courses[i], 4);
                c.setAssignedFacultyId("F10" + (i%5 + 1));
                courseManager.addCourse(c);
            }
        }
        seedAliceAcademicData();

        // Seed Students
        for(int i=2; i<=20; i++) { // Start from 2
            String id = "S10" + i;
            String name = "Student " + i;
            if(studentManager.searchById(id) == null) {
                Student s = new Student(id, name, "s"+i+"@univ.edu", "999-000"+i, depts[i%4], (i%8)+1);
                s.setCgpa(5.0 + (i * 0.2) % 5.0);
                s.setAttendancePercentage(60.0 + (i * 2) % 40.0);
                studentManager.addStudent(s);
                
                // Enroll in courses
                try {
                    courseManager.enrollStudent("C10" + (i%6), id);
                } catch(Exception e) {}
            }
            authManager.ensureUser(new StudentUser("student"+i, ""), "pass", id);
        }
        seedAttendance();
        System.out.println("System seeded with Alice, 19 students, 5 faculty, 6 courses, and LMS demo data.");
    }

    private static void seedAliceProfile() {
        Student alice = studentManager.searchById("S001");
        if (alice == null) return;

        boolean changed = false;
        if (alice.getCgpa() <= 0.0) {
            alice.setCgpa(8.65);
            changed = true;
        }
        if (alice.getAttendancePercentage() <= 0.0) {
            alice.setAttendancePercentage(92.5);
            changed = true;
        }
        if (alice.getLevel() <= 0) {
            alice.setLevel(1);
            changed = true;
        }
        if (alice.getSkills().isEmpty()) {
            alice.getSkills().addAll(Arrays.asList("Java", "SQL", "Data Structures"));
            changed = true;
        }
        if (alice.getProjects().isEmpty()) {
            alice.getProjects().addAll(Arrays.asList("Campus ERP Portal", "Library Analytics"));
            changed = true;
        }
        if (alice.getCertifications().isEmpty()) {
            alice.getCertifications().add("Oracle Java Foundations");
            changed = true;
        }
        if (alice.getLanguages().isEmpty()) {
            alice.getLanguages().addAll(Arrays.asList("English", "Hindi"));
            changed = true;
        }
        if (alice.getInterests().isEmpty()) {
            alice.getInterests().addAll(Arrays.asList("Cloud Systems", "Academic Automation"));
            changed = true;
        }
        if (alice.getBio() == null || alice.getBio().isBlank() || "Dedicated student at University.".equals(alice.getBio())) {
            alice.setBio("Computer Science student focused on reliable academic systems, Java, and data-driven learning tools.");
            changed = true;
        }

        if (changed) {
            try {
                studentManager.updateStudent(alice);
            } catch (Exception e) {
                System.err.println("Alice profile seed error: " + e.getMessage());
            }
        }
    }

    private static void seedAliceAcademicData() {
        try {
            courseManager.enrollStudent("C100", "S001");
            courseManager.enrollStudent("C101", "S001");

            seedAssignment("A1001", "C100", "Data Structures Lab Submission",
                    "Upload the completed stack and queue implementation with a short report.", 100, 5);
            seedAssignment("A1011", "C101", "SQL Schema Design Case Study",
                    "Submit the normalized schema, ER notes, and SQLite script bundle.", 50, 7);

            if (!projectDAO.exists("P-S001-ERP")) {
                Project project = new Project("P-S001-ERP", "S001", "Campus ERP Portal",
                        "Student dashboard and academic workflow consolidation.");
                project.setStatus("Reviewed");
                projectDAO.addProject(project);
            }

            if (!certificationDAO.exists("CERT-S001-JAVA")) {
                Certification certification = new Certification("CERT-S001-JAVA", "S001",
                        "Java Foundations", "Oracle Academy", new Date());
                certificationDAO.addCertification(certification);
            }
        } catch (Exception e) {
            System.err.println("Alice academic data seed error: " + e.getMessage());
        }
    }

    private static void seedAssignment(String id, String courseId, String title, String description, double marks, int dueDays) {
        if (assignmentManager.getAssignmentById(id) != null) return;
        Assignment assignment = new Assignment(
                id,
                courseId,
                title,
                description,
                new Date(System.currentTimeMillis() + dueDays * 24L * 60L * 60L * 1000L),
                marks
        );
        assignmentManager.createAssignment(assignment);
    }

    private static void seedAttendance() {
        markSeedAttendance("S001", "C100");
        for(int i=2; i<=20; i++) {
            String studentId = "S10" + i;
            String courseId = "C10" + (i%6);
            markSeedAttendance(studentId, courseId);
        }
    }

    private static void markSeedAttendance(String studentId, String courseId) {
        for(int j=0; j<10; j++) {
            try {
                attendanceManager.markAttendance(studentId, courseId, Math.random() > 0.2);
            } catch (Exception e) {}
        }
    }

    private static void showLoginMenu() {
        System.out.println("\n--- LOGIN ---");
        System.out.print("Username: ");
        String user = scanner.nextLine();
        System.out.print("Password: ");
        String pass = scanner.nextLine();

        try {
            User loggedInUser = authManager.authenticate(user, pass);
            SessionManager.startSession(loggedInUser);
            System.out.println("Login successful! Welcome " + loggedInUser.getUsername());
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private static void showAdminMenu() {
        System.out.println("\n--- ADMIN DASHBOARD ---");
        System.out.println("1. Add Student");
        System.out.println("2. View All Students");
        System.out.println("3. AI: Detect At-Risk Students");
        System.out.println("4. Logout");
        System.out.print("Choice: ");
        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                System.out.print("Enter ID: "); String id = scanner.nextLine();
                System.out.print("Enter Name: "); String name = scanner.nextLine();
                Student s = new Student(id, name, name+"@univ.edu", "000", "CS", 1);
                studentManager.addStudent(s);
                System.out.println("Student added.");
                break;
            case "2":
                studentManager.displayAllStudents();
                break;
            case "3":
                System.out.println("At-Risk Students:");
                for (Student r : aiEngine.detectRiskStudents(new java.util.ArrayList<>(studentManager.getStudentMap().values()))) {
                    System.out.println(r.getName());
                }
                break;
            case "4":
                SessionManager.endSession();
                break;
            default:
                System.out.println("Invalid choice.");
        }
    }

    private static void showFacultyMenu() {
        System.out.println("\n--- FACULTY DASHBOARD ---");
        System.out.println("1. Logout");
        System.out.print("Choice: ");
        String choice = scanner.nextLine();
        if ("1".equals(choice)) SessionManager.endSession();
    }

    private static void showStudentMenu() {
        System.out.println("\n--- STUDENT DASHBOARD ---");
        System.out.println("1. View Profile");
        System.out.println("2. Logout");
        System.out.print("Choice: ");
        String choice = scanner.nextLine();
        if ("1".equals(choice)) {
            try {
                Student student = StudentContext.requireCurrentStudent();
                System.out.println(student.generateReport());
            } catch (Exception e) {
                System.out.println("Unable to load profile: " + e.getMessage());
            }
        }
        else if ("2".equals(choice)) SessionManager.endSession();
    }
}
