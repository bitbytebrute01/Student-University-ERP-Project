package com.university.erp;

import com.university.courses.CourseManager;
import com.university.db.DatabaseManager;
import com.university.erp.dao.CertificationDAO;
import com.university.erp.dao.ProjectDAO;
import com.university.erp.gui.faculty.CourseManagementPanel;
import com.university.erp.gui.faculty.FacultyDashboard;
import com.university.erp.gui.faculty.FacultyLMSPanel;
import com.university.erp.gui.student.AssignmentHub;
import com.university.erp.gui.student.CertificationHub;
import com.university.erp.gui.student.GamificationPanel;
import com.university.erp.gui.student.PlacementProfilePanel;
import com.university.erp.gui.student.ProfileEditor;
import com.university.erp.gui.student.ProjectPanel;
import com.university.erp.gui.student.StudentDashboard;
import com.university.erp.gui.student.StudentLinkedInProfile;
import com.university.erp.gui.theme.ThemeManager;
import com.university.erp.security.AdminUser;
import com.university.erp.security.AuthenticationManager;
import com.university.erp.security.FacultyUser;
import com.university.erp.security.SessionManager;
import com.university.erp.security.StudentContext;
import com.university.erp.security.StudentUser;
import com.university.erp.security.User;
import com.university.erp.security.UserRole;
import com.university.faculty.FacultyManager;
import com.university.lms.AssignmentManager;
import com.university.models.Assignment;
import com.university.models.Certification;
import com.university.models.Course;
import com.university.models.Faculty;
import com.university.models.Project;
import com.university.models.Student;
import com.university.models.Submission;
import com.university.students.StudentManager;
import com.university.utils.MediaManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginFlowTest {
    @TempDir
    Path tempDir;

    private AuthenticationManager authManager;
    private StudentManager studentManager;
    private FacultyManager facultyManager;
    private CourseManager courseManager;
    private AssignmentManager assignmentManager;
    private ProjectDAO projectDAO;
    private CertificationDAO certificationDAO;

    @BeforeEach
    void setUp() throws Exception {
        System.setProperty("java.awt.headless", "true");
        System.setProperty("university.db.url", "jdbc:sqlite:" + tempDir.resolve("university_erp_test.db"));
        System.setProperty("university.upload.dir", tempDir.resolve("uploads").toString());

        DatabaseManager.initializeSchema();
        ThemeManager.initialize();

        authManager = new AuthenticationManager();
        studentManager = new StudentManager();
        facultyManager = new FacultyManager();
        courseManager = new CourseManager();
        assignmentManager = new AssignmentManager();
        projectDAO = new ProjectDAO();
        certificationDAO = new CertificationDAO();

        seedLoginData();
    }

    @AfterEach
    void tearDown() {
        SessionManager.endSession();
        System.clearProperty("university.db.url");
        System.clearProperty("university.upload.dir");
    }

    @Test
    void aliceLoginResolvesStudentBeforeOpeningStudentScreens() throws Exception {
        User alice = authManager.authenticate("alice", "pass");

        assertEquals(UserRole.STUDENT, alice.getRole());
        assertEquals("S001", alice.getRefId());

        SessionManager.startSession(alice);
        Student student = StudentContext.requireCurrentStudent();
        assertEquals("Alice", student.getName());

        assertDoesNotThrow(() -> new StudentDashboard(student));
        assertDoesNotThrow(() -> new StudentLinkedInProfile(student));
        assertDoesNotThrow(() -> new ProfileEditor(student));
        assertDoesNotThrow(() -> new ProjectPanel(student));
        assertDoesNotThrow(() -> new AssignmentHub(student));
        assertDoesNotThrow(() -> new CertificationHub(student));
        assertDoesNotThrow(() -> new PlacementProfilePanel(student));
        assertDoesNotThrow(() -> new GamificationPanel(student));
    }

    @Test
    void profilePhotoPersistsAndDashboardRefreshes() throws Exception {
        Student alice = studentManager.searchById("S001");
        File sourceImage = createPng("alice-profile.png");

        String savedPath = MediaManager.saveImage(sourceImage, "profiles");
        alice.setProfilePicturePath(savedPath);
        studentManager.updateStudent(alice);

        Student reloaded = studentManager.searchById("S001");
        assertEquals(savedPath, reloaded.getProfilePicturePath());
        assertTrue(Files.exists(Path.of(savedPath)));

        StudentDashboard dashboard = new StudentDashboard(reloaded);
        assertDoesNotThrow(() -> dashboard.refreshStudent(reloaded));
    }

    @Test
    void assignmentCanBeSubmittedAndGraded() throws Exception {
        Assignment assignment = assignmentManager.getAssignmentById("A-TST-1");
        assertNotNull(assignment);
        assertEquals("Published", assignment.getStatus());
        assertNotNull(assignment.getDueTime());

        List<Assignment> visibleAssignments = assignmentManager.getAssignmentsForStudent("S001");
        assertTrue(visibleAssignments.stream().anyMatch(a -> "A-TST-1".equals(a.getId())));
        assertEquals(1, assignmentManager.getUnreadAssignmentNotificationCount("S001"));
        assertTrue(assignmentManager.getUnreadAssignmentNotifications("S001").get(0).getMessage().contains("Stack Implementation"));
        assertEquals("Pending", assignmentManager.getStudentAssignmentStatus("S001", assignment));
        assertFalse(assignmentManager.getDeadlineCountdown(assignment).isBlank());

        File pdf = tempDir.resolve("submission.pdf").toFile();
        Files.writeString(pdf.toPath(), "%PDF-1.4\nStudent submission\n%%EOF\n");
        File zip = tempDir.resolve("source-bundle.zip").toFile();
        Files.writeString(zip.toPath(), "zip-placeholder");
        List<String> savedPaths = MediaManager.saveSubmissionFiles(List.of(pdf, zip),
                "submissions/" + assignment.getId() + "/S001");

        Submission submission = new Submission("SUB-TST-1", assignment.getId(), "S001", null);
        submission.setSubmissionText("Here is my stack implementation and supporting source bundle.");
        submission.setAttachmentPaths(savedPaths);
        assignmentManager.submitAssignment(submission);

        Submission loaded = assignmentManager.getStudentSubmission("S001", assignment.getId());
        assertNotNull(loaded);
        assertFalse(loaded.isGraded());
        assertEquals("Here is my stack implementation and supporting source bundle.", loaded.getSubmissionText());
        assertEquals(2, loaded.getAttachmentPaths().size());
        assertEquals("Submitted", loaded.getStatus());
        assertEquals(0, assignmentManager.getUnreadAssignmentNotificationCount("S001"));
        assertEquals(1, countRows("submissions"));
        assertEquals(1, countRows("assignment_submissions"));
        assertEquals(2, countRows("assignment_submission_files"));

        List<Path> downloaded = assignmentManager.downloadSubmissionFiles(loaded.getId(), tempDir.resolve("downloads").toFile());
        assertEquals(2, downloaded.size());
        assertTrue(downloaded.stream().allMatch(Files::exists));

        File docx = tempDir.resolve("revised-submission.docx").toFile();
        Files.writeString(docx.toPath(), "docx-placeholder");
        List<String> revisedPaths = MediaManager.saveSubmissionFiles(List.of(docx),
                "submissions/" + assignment.getId() + "/S001");
        Submission revised = new Submission("SUB-TST-REVISED-ID-SHOULD-NOT-REPLACE", assignment.getId(), "S001", null);
        revised.setSubmissionText("Revised response.");
        revised.setAttachmentPaths(revisedPaths);
        assignmentManager.submitAssignment(revised);

        Submission reloadedRevision = assignmentManager.getStudentSubmission("S001", assignment.getId());
        assertEquals(loaded.getId(), reloadedRevision.getId());
        assertEquals("Revised response.", reloadedRevision.getSubmissionText());
        assertEquals(1, reloadedRevision.getAttachmentPaths().size());
        assertEquals(1, countRows("assignment_submission_files"));
        List<Path> revisedDownload = assignmentManager.downloadSubmissionFiles(reloadedRevision.getId(), tempDir.resolve("revised-downloads").toFile());
        assertEquals(1, revisedDownload.size());
        assertTrue(Files.exists(revisedDownload.get(0)));

        assignmentManager.gradeSubmission(reloadedRevision.getId(), 88.0, "Good work.");
        Submission graded = assignmentManager.getStudentSubmission("S001", assignment.getId());
        assertTrue(graded.isGraded());
        assertEquals(88.0, graded.getMarks());
        assertEquals("Good work.", graded.getFeedback());
        assertEquals("Graded", graded.getStatus());
    }

    @Test
    void enrollingStudentAfterAssignmentCreatesVisibilityAndNotification() throws Exception {
        Student bob = new Student("S002", "Bob", "bob@univ.edu", "999-0002", "CS", 1);
        studentManager.addStudent(bob);

        assertEquals(0, assignmentManager.getUnreadAssignmentNotificationCount("S002"));

        courseManager.enrollStudent("C101", "S002");

        List<Assignment> visibleAssignments = assignmentManager.getAssignmentsForStudent("S002");
        assertTrue(visibleAssignments.stream().anyMatch(a -> "A-TST-1".equals(a.getId())));
        assertEquals(1, assignmentManager.getUnreadAssignmentNotificationCount("S002"));
        assertTrue(assignmentManager.getUnreadAssignmentNotifications("S002").get(0).getMessage().contains("Stack Implementation"));
    }

    @Test
    void lateSubmissionsAreMarkedAutomatically() throws Exception {
        Assignment assignment = new Assignment("A-TST-LATE", "C101", "Late Upload Check",
                "Submit after the deadline.", new Date(System.currentTimeMillis() - 60_000L), 100);
        assignmentManager.createAssignment(assignment);

        Submission submission = new Submission("SUB-TST-LATE", assignment.getId(), "S001", null);
        submission.setSubmissionText("Late text response.");
        assignmentManager.submitAssignment(submission);

        Submission loaded = assignmentManager.getStudentSubmission("S001", assignment.getId());
        assertNotNull(loaded);
        assertEquals("Late", loaded.getStatus());
    }

    @Test
    void legacyMillisecondDeadlinesStillRefreshLateSubmissionStatus() throws Exception {
        long deadline = System.currentTimeMillis() - 120_000L;
        long submittedAt = System.currentTimeMillis() - 60_000L;

        try (Connection conn = DatabaseManager.getConnection()) {
            try (PreparedStatement assignment = conn.prepareStatement(
                    "INSERT INTO assignments (id, assignment_id, course_id, title, description, deadline, max_marks, status) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                assignment.setString(1, "A-TST-LEGACY-LATE");
                assignment.setString(2, "A-TST-LEGACY-LATE");
                assignment.setString(3, "C101");
                assignment.setString(4, "Legacy Late Assignment");
                assignment.setString(5, "Legacy millisecond deadline.");
                assignment.setLong(6, deadline);
                assignment.setDouble(7, 100);
                assignment.setString(8, "Published");
                assignment.executeUpdate();
            }
            try (PreparedStatement submission = conn.prepareStatement(
                    "INSERT INTO submissions (id, submission_id, assignment_id, student_id, submission_text, submitted_at, submission_date, status, is_graded) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)")) {
                submission.setString(1, "SUB-TST-LEGACY-LATE");
                submission.setString(2, "SUB-TST-LEGACY-LATE");
                submission.setString(3, "A-TST-LEGACY-LATE");
                submission.setString(4, "S001");
                submission.setString(5, "Submitted after a legacy deadline.");
                submission.setLong(6, submittedAt);
                submission.setLong(7, submittedAt);
                submission.setString(8, "Submitted");
                submission.executeUpdate();
            }
        }

        Submission loaded = assignmentManager.getStudentSubmission("S001", "A-TST-LEGACY-LATE");
        assertNotNull(loaded);
        assertEquals("Late", loaded.getStatus());
    }

    @Test
    void adminLoginDoesNotRequireStudentRecord() throws Exception {
        User admin = authManager.authenticate("admin", "admin123");

        assertEquals(UserRole.ADMIN, admin.getRole());
    }

    @Test
    void facultyLoginUsesFacultyReferenceForFacultyScreens() throws Exception {
        User faculty = authManager.authenticate("faculty", "pass");

        assertEquals(UserRole.FACULTY, faculty.getRole());
        assertEquals("F101", faculty.getRefId());

        SessionManager.startSession(faculty);
        assertDoesNotThrow(FacultyDashboard::new);
        assertDoesNotThrow(CourseManagementPanel::new);
        assertDoesNotThrow(FacultyLMSPanel::new);
    }

    private void seedLoginData() throws Exception {
        Student alice = new Student("S001", "Alice", "alice@univ.edu", "999-0001", "CS", 1);
        alice.setCgpa(8.6);
        alice.setAttendancePercentage(91.0);
        studentManager.addStudent(alice);
        authManager.registerUser(new StudentUser("alice", ""), "pass", "S001");

        Faculty faculty = new Faculty("F101", "Prof 1", "prof1@univ.edu", "555-0001", "CS", "Professor", 6);
        facultyManager.addFaculty(faculty);
        authManager.registerUser(new FacultyUser("faculty", ""), "pass", "F101");

        Course course = new Course("C101", "Data Structures", 4);
        course.setAssignedFacultyId("F101");
        courseManager.addCourse(course);
        courseManager.enrollStudent("C101", "S001");

        Assignment assignment = new Assignment("A-TST-1", "C101", "Stack Implementation",
                "Submit stack implementation with a PDF note.", new Date(System.currentTimeMillis() + 86_400_000L), 100);
        assignmentManager.createAssignment(assignment);

        Project project = new Project("P-TST-1", "S001", "Campus ERP Portal", "Dashboard stabilization work.");
        project.setStatus("Reviewed");
        projectDAO.addProject(project);

        Certification certification = new Certification("CERT-TST-1", "S001", "Java Foundations", "Oracle Academy", new Date());
        certificationDAO.addCertification(certification);

        authManager.registerUser(new AdminUser("admin", ""), "admin123", null);
    }

    private File createPng(String name) throws Exception {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(0, 115, 177));
        g.fillRect(0, 0, 32, 32);
        g.setColor(Color.WHITE);
        g.fillOval(8, 6, 16, 16);
        g.dispose();

        File file = tempDir.resolve(name).toFile();
        ImageIO.write(image, "png", file);
        return file;
    }

    private int countRows(String table) throws Exception {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT COUNT(*) FROM " + table);
             ResultSet rs = pstmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
