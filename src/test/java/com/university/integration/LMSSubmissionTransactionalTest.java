package com.university.integration;

import com.university.courses.CourseManager;
import com.university.faculty.FacultyManager;
import com.university.lms.AssignmentManager;
import com.university.models.Assignment;
import com.university.models.Submission;
import com.university.students.StudentManager;
import com.university.erp.security.AuthenticationManager;
import com.university.erp.security.FacultyUser;
import com.university.erp.security.StudentUser;
import com.university.models.Student;
import com.university.models.Faculty;
import com.university.utils.MediaManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class LMSSubmissionTransactionalTest extends IntegrationTestBase {

    // Helper to run raw SQL against the DB
    private void execSql(String sql) throws Exception {
        try (Connection c = java.sql.DriverManager.getConnection(System.getProperty("university.db.url"))) {
            try (Statement s = c.createStatement()) {
                s.execute(sql);
            }
        }
    }

    @Test
    public void case1_fileSaved_dbInsertFails_filesRemoved_submissionNotCreated() throws Exception {
        AuthenticationManager auth = new AuthenticationManager();
        StudentManager sm = new StudentManager();
        CourseManager cm = new CourseManager();
        AssignmentManager am = new AssignmentManager();
        FacultyManager fm = new FacultyManager();

        // seed users
        Student s = new Student("SIM-1", "FailDBStudent", "f@u.edu", "111", "CS", 1);
        sm.addStudent(s);
        auth.registerUser(new StudentUser("simstudent", ""), "pass", "SIM-1");

        fm.addFaculty(new Faculty("F1","Dr","d@u.edu","111","CS","Prof",10));
        String courseId = "CRS1";
        cm.addCourse(new com.university.models.Course(courseId, "C1", 3));
        cm.assignFacultyToCourse(courseId, "F1");
        cm.enrollStudent(courseId, "SIM-1");

        Assignment assignment = new Assignment("ASIM1", courseId, "A1", "Do hw", new Date(System.currentTimeMillis()+3600_000), 100);
        assignment.setCreatedBy("F1");
        am.createAssignment(assignment);

        // create trigger to abort inserts into submissions for this student
        String trigger = "CREATE TRIGGER fail_sub_ins BEFORE INSERT ON submissions WHEN NEW.student_id = 'SIM-1' BEGIN SELECT RAISE(ABORT, 'simulated-db-failure'); END;";
        execSql(trigger);

        // prepare temp upload
        Path pdf = Files.createTempFile(tempDir, "t", ".pdf");
        Files.writeString(pdf, "%PDF\nhello\n");
        String tempPath = MediaManager.saveTemp(pdf.toFile());

        Submission sub = new Submission(null, assignment.getId(), "SIM-1", null);
        sub.setAttachmentPaths(List.of(tempPath));
        Exception ex = null;
        try {
            am.submitAssignment(sub);
        } catch (Exception e) {
            ex = e;
        }
        assertNotNull(ex, "Submission should have failed due to simulated DB failure");

        // ensure no submission record
        assertNull(am.getStudentSubmission("SIM-1", assignment.getId()));

        // ensure no files exist under submissions for this assignment
        Path submissionsRoot = Path.of(System.getProperty("university.upload.dir")).resolve("submissions/" + assignment.getId());
        assertFalse(Files.exists(submissionsRoot), "No submission files should remain after failed DB insert");
    }

    @Test
    public void case2_dbInsertSucceeds_notificationUpdateFails_transactionRollsBack() throws Exception {
        AuthenticationManager auth = new AuthenticationManager();
        StudentManager sm = new StudentManager();
        CourseManager cm = new CourseManager();
        AssignmentManager am = new AssignmentManager();
        FacultyManager fm = new FacultyManager();

        Student s = new Student("SIM-2", "NotifyFailStudent", "n@u.edu", "222", "CS", 1);
        sm.addStudent(s);
        auth.registerUser(new StudentUser("notifystudent", ""), "pass", "SIM-2");

        fm.addFaculty(new Faculty("F2","Dr","d2@u.edu","222","CS","Prof",10));
        String courseId = "CRS2";
        cm.addCourse(new com.university.models.Course(courseId, "C2", 3));
        cm.assignFacultyToCourse(courseId, "F2");
        cm.enrollStudent(courseId, "SIM-2");

        Assignment assignment = new Assignment("ASIM2", courseId, "A2", "Do hw", new Date(System.currentTimeMillis()+3600_000), 100);
        assignment.setCreatedBy("F2");
        am.createAssignment(assignment);

        // Ensure notification exists (created by createAssignment)
        assertTrue(am.getUnreadAssignmentNotificationCount("SIM-2") > 0);

        // create trigger to abort updates to assignment_notifications for this student
        String trigger = "CREATE TRIGGER fail_not_upd BEFORE UPDATE ON assignment_notifications WHEN NEW.student_id = 'SIM-2' BEGIN SELECT RAISE(ABORT, 'simulated-notify-failure'); END;";
        execSql(trigger);

        Path pdf = Files.createTempFile(tempDir, "t", ".pdf");
        Files.writeString(pdf, "%PDF\nhello\n");
        String tempPath = MediaManager.saveTemp(pdf.toFile());

        Submission sub = new Submission(null, assignment.getId(), "SIM-2", null);
        sub.setAttachmentPaths(List.of(tempPath));

        Exception ex = null;
        try {
            am.submitAssignment(sub);
        } catch (Exception e) {
            ex = e;
        }
        assertNotNull(ex, "Submission should have failed due to simulated notification update failure");

        // ensure no submission record
        assertNull(am.getStudentSubmission("SIM-2", assignment.getId()));

        // ensure no files exist under submissions for this assignment
        Path submissionsRoot = Path.of(System.getProperty("university.upload.dir")).resolve("submissions/" + assignment.getId());
        assertFalse(Files.exists(submissionsRoot), "No submission files should remain after failed notification update");
    }

    @Test
    public void case3_multipleAttachments_allSaved_and_recordsCreated() throws Exception {
        AuthenticationManager auth = new AuthenticationManager();
        StudentManager sm = new StudentManager();
        CourseManager cm = new CourseManager();
        AssignmentManager am = new AssignmentManager();
        FacultyManager fm = new FacultyManager();

        Student s = new Student("SIM-3", "MultiAttachStudent", "m@u.edu", "333", "CS", 1);
        sm.addStudent(s);
        auth.registerUser(new StudentUser("multistudent", ""), "pass", "SIM-3");

        fm.addFaculty(new Faculty("F3","Dr","d3@u.edu","333","CS","Prof",10));
        String courseId = "CRS3";
        cm.addCourse(new com.university.models.Course(courseId, "C3", 3));
        cm.assignFacultyToCourse(courseId, "F3");
        cm.enrollStudent(courseId, "SIM-3");

        Assignment assignment = new Assignment("ASIM3", courseId, "A3", "Do hw", new Date(System.currentTimeMillis()+3600_000), 100);
        assignment.setCreatedBy("F3");
        am.createAssignment(assignment);

        Path pdf = Files.createTempFile(tempDir, "t", ".pdf");
        Files.writeString(pdf, "%PDF\nhello\n");
        Path zip = Files.createTempFile(tempDir, "t", ".zip");
        Files.writeString(zip, "zipcontent");

        String temp1 = MediaManager.saveTemp(pdf.toFile());
        String temp2 = MediaManager.saveTemp(zip.toFile());

        Submission sub = new Submission(null, assignment.getId(), "SIM-3", null);
        sub.setAttachmentPaths(List.of(temp1, temp2));

        am.submitAssignment(sub);

        Submission loaded = am.getStudentSubmission("SIM-3", assignment.getId());
        assertNotNull(loaded);
        assertEquals(2, loaded.getAttachmentPaths().size());
        for (String p : loaded.getAttachmentPaths()) {
            assertTrue(Files.exists(Path.of(p)));
        }
    }

    @Test
    public void case4_oneAttachmentInvalid_entireSubmissionRejected() throws Exception {
        AuthenticationManager auth = new AuthenticationManager();
        StudentManager sm = new StudentManager();
        CourseManager cm = new CourseManager();
        AssignmentManager am = new AssignmentManager();
        FacultyManager fm = new FacultyManager();

        Student s = new Student("SIM-4", "InvalidAttachStudent", "i@u.edu", "444", "CS", 1);
        sm.addStudent(s);
        auth.registerUser(new StudentUser("invstudent", ""), "pass", "SIM-4");

        fm.addFaculty(new Faculty("F4","Dr","d4@u.edu","444","CS","Prof",10));
        String courseId = "CRS4";
        cm.addCourse(new com.university.models.Course(courseId, "C4", 3));
        cm.assignFacultyToCourse(courseId, "F4");
        cm.enrollStudent(courseId, "SIM-4");

        Assignment assignment = new Assignment("ASIM4", courseId, "A4", "Do hw", new Date(System.currentTimeMillis()+3600_000), 100);
        assignment.setCreatedBy("F4");
        am.createAssignment(assignment);

        // create a valid temp PDF then rename it to have an invalid extension to simulate bad attachment
        Path pdf = Files.createTempFile(tempDir, "t", ".pdf");
        Files.writeString(pdf, "%PDF\nhello\n");
        String tempOk = MediaManager.saveTemp(pdf.toFile());
        // rename to .exe to simulate invalid file format while keeping it in temp dir
        Path tempOkPath = Path.of(tempOk);
        Path badRenamed = tempOkPath.resolveSibling(tempOkPath.getFileName().toString().replaceAll("\\.pdf$", ".exe"));
        Files.move(tempOkPath, badRenamed);
        String tempBad = badRenamed.toString();

        Submission sub = new Submission(null, assignment.getId(), "SIM-4", null);
        sub.setAttachmentPaths(List.of(tempBad));

        Exception ex = null;
        try {
            am.submitAssignment(sub);
        } catch (Exception e) {
            ex = e;
        }
        assertNotNull(ex);
        assertTrue(ex.getMessage().contains("Unsupported submission file format") || ex.getMessage().contains("Submission file was not saved"));

        // ensure no submission record
        assertNull(am.getStudentSubmission("SIM-4", assignment.getId()));

        // ensure no files remain
        Path submissionsRoot = Path.of(System.getProperty("university.upload.dir")).resolve("submissions/" + assignment.getId());
        assertFalse(Files.exists(submissionsRoot));
    }

    @Test
    public void case5_facultyGrades_submissionVisibleToStudent() throws Exception {
        // reuse end-to-end test but simplified
        AuthenticationManager auth = new AuthenticationManager();
        StudentManager sm = new StudentManager();
        CourseManager cm = new CourseManager();
        AssignmentManager am = new AssignmentManager();
        FacultyManager fm = new FacultyManager();

        Student s = new Student("SIM-5", "GradeStudent", "g@u.edu", "555", "CS", 1);
        sm.addStudent(s);
        auth.registerUser(new StudentUser("gradestudent", ""), "pass", "SIM-5");

        fm.addFaculty(new Faculty("F5","Dr","d5@u.edu","555","CS","Prof",10));
        String courseId = "CRS5";
        cm.addCourse(new com.university.models.Course(courseId, "C5", 3));
        cm.assignFacultyToCourse(courseId, "F5");
        cm.enrollStudent(courseId, "SIM-5");

        Assignment assignment = new Assignment("ASIM5", courseId, "A5", "Do hw", new Date(System.currentTimeMillis()+3600_000), 100);
        assignment.setCreatedBy("F5");
        am.createAssignment(assignment);

        Path pdf = Files.createTempFile(tempDir, "t", ".pdf");
        Files.writeString(pdf, "%PDF\nhello\n");
        String temp = MediaManager.saveTemp(pdf.toFile());

        Submission sub = new Submission(null, assignment.getId(), "SIM-5", null);
        sub.setAttachmentPaths(List.of(temp));
        am.submitAssignment(sub);

        Submission loaded = am.getStudentSubmission("SIM-5", assignment.getId());
        assertNotNull(loaded);

        am.gradeSubmission(loaded.getId(), 88.0, "Good work");

        Submission after = am.getStudentSubmission("SIM-5", assignment.getId());
        assertTrue(after.isGraded());
        assertEquals(88.0, after.getMarks());
    }
}
