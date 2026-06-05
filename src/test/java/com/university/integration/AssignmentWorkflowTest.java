package com.university.integration;

import com.university.courses.CourseManager;
import com.university.lms.AssignmentManager;
import com.university.models.Assignment;
import com.university.models.Submission;
import com.university.students.StudentManager;
import com.university.erp.security.AuthenticationManager;
import com.university.erp.security.StudentUser;
import com.university.erp.security.FacultyUser;
import com.university.erp.security.AdminUser;
import com.university.models.Student;
import com.university.utils.MediaManager;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AssignmentWorkflowTest extends IntegrationTestBase {

    @Test
    public void testAssignmentEndToEnd() throws Exception {
        AuthenticationManager auth = new AuthenticationManager();
        StudentManager sm = new StudentManager();
        CourseManager cm = new CourseManager();
        AssignmentManager am = new AssignmentManager();

        // Seed users and student
        auth.registerUser(new AdminUser("admin", ""), "admin123", null);
        auth.registerUser(new FacultyUser("faculty", ""), "pass", "F101");

        Student alice = new Student("S001", "Alice", "alice@univ.edu", "999-0001", "CS", 1);
        sm.addStudent(alice);
        auth.registerUser(new StudentUser("alice", ""), "pass", "S001");

        // Create faculty record, course and enroll
        String courseId = "C200";
        com.university.faculty.FacultyManager fm = new com.university.faculty.FacultyManager();
        fm.addFaculty(new com.university.models.Faculty("F101", "Prof X", "profx@univ.edu", "555-0001", "CS", "Professor", 10));
        cm.addCourse(new com.university.models.Course(courseId, "Integration Course", 3));
        cm.assignFacultyToCourse(courseId, "F101");
        cm.enrollStudent(courseId, "S001");

        // Faculty create assignment
        Assignment assignment = new Assignment("A200", courseId, "Integration Assignment", "Do this.", new Date(System.currentTimeMillis() + 2*60*60*1000), 100);
        assignment.setCreatedBy("faculty");
        assignment.setStatus("Published");
        am.createAssignment(assignment);

        // Student sees assignment
        List<Assignment> studentAssignments = am.getAssignmentsForStudent("S001");
        assertTrue(studentAssignments.stream().anyMatch(a -> a.getId().equals(assignment.getId())));

        // Prepare submission files
        Path pdf = Files.createTempFile(tempDir, "sub", ".pdf");
        Files.writeString(pdf, "%PDF-1.4\nTest PDF\n%%EOF\n");
        Path zip = Files.createTempFile(tempDir, "sub", ".zip");
        Files.writeString(zip, "zipcontent");

        List<String> saved = MediaManager.saveSubmissionFiles(List.of(pdf.toFile(), zip.toFile()), "submissions/" + assignment.getId() + "/S001");

        Submission submission = new Submission(null, assignment.getId(), "S001", null);
        submission.setSubmissionText("Here is my work.");
        submission.setAttachmentPaths(saved);
        am.submitAssignment(submission);

        Submission loaded = am.getStudentSubmission("S001", assignment.getId());
        assertNotNull(loaded);
        assertFalse(loaded.getAttachmentPaths().isEmpty());
        assertEquals(2, loaded.getAttachmentPaths().size());

        // Faculty download
        Path downloadDir = Files.createTempDirectory(tempDir, "dl");
        List<Path> downloaded = am.downloadSubmissionFiles(loaded.getId(), downloadDir.toFile());
        assertEquals(2, downloaded.size());
        for (Path p : downloaded) {
            assertTrue(Files.exists(p));
            assertTrue(Files.size(p) > 0);
        }

        // Grade
        am.gradeSubmission(loaded.getId(), 95.0, "Excellent");

        Submission graded = am.getStudentSubmission("S001", assignment.getId());
        assertTrue(graded.isGraded());
        assertEquals(95.0, graded.getMarks());
        assertNotNull(graded.getFeedback());
    }
}
