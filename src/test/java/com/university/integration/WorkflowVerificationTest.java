package com.university.integration;

import com.university.erp.security.AuthenticationManager;
import com.university.erp.security.AdminUser;
import com.university.erp.security.FacultyUser;
import com.university.erp.security.StudentUser;
import com.university.erp.security.SessionManager;
import com.university.students.StudentManager;
import com.university.models.Student;
import com.university.db.DatabaseManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WorkflowVerificationTest extends IntegrationTestBase {

    @Test
    public void testBasicLoginsAndStudentProfilePersistence() throws Exception {
        AuthenticationManager auth = new AuthenticationManager();
        StudentManager sm = new StudentManager();

        // Register admin, faculty, student
        auth.registerUser(new AdminUser("admin", ""), "admin123", null);
        auth.registerUser(new FacultyUser("faculty", ""), "pass", "F101");

        // create student record
        Student alice = new Student("S001", "Alice", "alice@univ.edu", "999-0001", "CS", 1);
        sm.addStudent(alice);
        auth.registerUser(new StudentUser("alice", ""), "pass", "S001");

        // Authenticate each
        var adminUser = auth.authenticate("admin", "admin123");
        assertNotNull(adminUser);
        SessionManager.startSession(adminUser);
        assertTrue(SessionManager.hasActiveSession());
        SessionManager.endSession();

        var facultyUser = auth.authenticate("faculty", "pass");
        assertNotNull(facultyUser);
        SessionManager.startSession(facultyUser);
        assertTrue(SessionManager.hasActiveSession());
        SessionManager.endSession();

        var studentUser = auth.authenticate("alice", "pass");
        assertNotNull(studentUser);
        SessionManager.startSession(studentUser);
        assertTrue(SessionManager.hasActiveSession());

        // Modify profile and persist
        Student loaded = sm.searchById("S001");
        loaded.setBio("Integration test bio");
        sm.updateStudent(loaded);

        // Simulate restart: reinitialize DB manager
        DatabaseManager.initializeSchema();

        Student reloaded = sm.searchById("S001");
        assertNotNull(reloaded);
        assertEquals("Integration test bio", reloaded.getBio());

        SessionManager.endSession();
    }
}
