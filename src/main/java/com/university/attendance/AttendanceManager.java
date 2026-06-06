package com.university.attendance;

import com.university.db.DatabaseManager;
import com.university.exceptions.InvalidAttendanceException;
import com.university.models.Student;
import com.university.students.StudentManager;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class AttendanceManager {
    private StudentManager studentManager;
    // sessionId -> code
    private static final java.util.Map<String, String> activeSessions = new java.util.concurrent.ConcurrentHashMap<>();

    public AttendanceManager(StudentManager studentManager) {
        this.studentManager = studentManager;
    }

    /**
     * Start a new attendance session for a course by a faculty member.
     * Returns the one-time attendance code for students to enter.
     */
    public String startAttendanceSession(String facultyId, String courseId, int durationMinutes) {
        String sessionId = "S-" + java.util.UUID.randomUUID();
        String code = String.format("%06d", new Random().nextInt(999999));
        String sql = "INSERT INTO attendance_sessions (session_id, course_id, faculty_id, attendance_code, created_at, expires_at, status) " +
                "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, datetime('now', ?), 'ACTIVE')";
        String expiresArg = "+" + Math.max(1, durationMinutes) + " minutes";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);
            pstmt.setString(2, courseId);
            pstmt.setString(3, facultyId);
            pstmt.setString(4, code);
            pstmt.setString(5, expiresArg);
            pstmt.executeUpdate();
            activeSessions.put(sessionId, code);
            // Notify UI
            com.university.erp.gui.UIEventBus.publish("ATTENDANCE_SESSION_STARTED", java.util.Map.of("session_id", sessionId, "course_id", courseId, "faculty_id", facultyId));
            return code;
        } catch (SQLException e) {
            throw new IllegalStateException("Error starting attendance session: " + e.getMessage(), e);
        }
    }

    /**
     * Ends an attendance session (manual faculty action or programmatic).
     */
    public void endAttendanceSession(String sessionId, String facultyId) {
        String sql = "UPDATE attendance_sessions SET status = 'ENDED', expires_at = CURRENT_TIMESTAMP WHERE session_id = ? AND faculty_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, sessionId);
            pstmt.setString(2, facultyId);
            pstmt.executeUpdate();
            activeSessions.remove(sessionId);
            com.university.erp.gui.UIEventBus.publish("ATTENDANCE_SESSION_ENDED", java.util.Map.of("session_id", sessionId));
        } catch (SQLException e) {
            throw new IllegalStateException("Error ending attendance session: " + e.getMessage(), e);
        }
    }

    /**
     * Backwards-compatible API: mark attendance directly (legacy flows).
     */
    public void markAttendance(String studentId, String courseId, boolean isPresent) throws InvalidAttendanceException {
        if (studentManager.searchById(studentId) == null) {
            throw new InvalidAttendanceException("Cannot mark attendance for unknown student.");
        }

        String sql = "INSERT INTO attendance (student_id, course_id, is_present, updated_at) VALUES (?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseId);
            pstmt.setBoolean(3, isPresent);
            pstmt.executeUpdate();

            updateStudentAttendancePercentage(studentId);

            // Notify UI
            com.university.erp.gui.UIEventBus.publish("ATTENDANCE_UPDATED", java.util.Map.of("student_id", studentId, "course_id", courseId));
        } catch (SQLException e) {
            throw new IllegalStateException("Error marking attendance: " + e.getMessage(), e);
        }
    }

    /**
     * Mark attendance using a session ID and code. Device identifier (IP or device token) should be provided.
     */
    public boolean markAttendanceWithSession(String studentId, String sessionId, String code, String deviceIdentifier) throws InvalidAttendanceException {
        // Validate session
        String sessionSql = "SELECT course_id, attendance_code, expires_at, status FROM attendance_sessions WHERE session_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sessionSql)) {
            pstmt.setString(1, sessionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (!rs.next()) {
                    logAudit(conn, sessionId, studentId, deviceIdentifier, "SESSION_NOT_FOUND");
                    throw new InvalidAttendanceException("Attendance session not found.");
                }
                String courseId = rs.getString("course_id");
                String expectedCode = rs.getString("attendance_code");
                Timestamp expiresAt = rs.getTimestamp("expires_at");
                String status = rs.getString("status");

                if (!"ACTIVE".equalsIgnoreCase(status)) {
                    logAudit(conn, sessionId, studentId, deviceIdentifier, "SESSION_INACTIVE");
                    throw new InvalidAttendanceException("Attendance session is not active.");
                }
                if (expiresAt != null && expiresAt.before(new java.util.Date())) {
                    logAudit(conn, sessionId, studentId, deviceIdentifier, "SESSION_EXPIRED");
                    throw new InvalidAttendanceException("Attendance session has expired.");
                }
                if (expectedCode == null || !expectedCode.equals(code)) {
                    logAudit(conn, sessionId, studentId, deviceIdentifier, "INVALID_CODE");
                    throw new InvalidAttendanceException("Invalid attendance code.");
                }

                // Verify enrollment: student must be enrolled in course
                try (PreparedStatement enrollCheck = conn.prepareStatement("SELECT 1 FROM enrollments WHERE course_id = ? AND student_id = ?")) {
                    enrollCheck.setString(1, courseId);
                    enrollCheck.setString(2, studentId);
                    try (ResultSet ers = enrollCheck.executeQuery()) {
                        if (!ers.next()) {
                            logAudit(conn, sessionId, studentId, deviceIdentifier, "NOT_ENROLLED");
                            throw new InvalidAttendanceException("Student not enrolled in course.");
                        }
                    }
                }

                // Prevent duplicates (attendance_audit has unique index)
                try (PreparedStatement dupCheck = conn.prepareStatement("SELECT 1 FROM attendance_audit WHERE session_id = ? AND student_id = ?")) {
                    dupCheck.setString(1, sessionId);
                    dupCheck.setString(2, studentId);
                    try (ResultSet drs = dupCheck.executeQuery()) {
                        if (drs.next()) {
                            logAudit(conn, sessionId, studentId, deviceIdentifier, "DUPLICATE");
                            throw new InvalidAttendanceException("Attendance already recorded for this session.");
                        }
                    }
                }

                // Record audit entry and attendance atomically
                conn.setAutoCommit(false);
                try (PreparedStatement auditStmt = conn.prepareStatement("INSERT INTO attendance_audit (audit_id, session_id, student_id, timestamp, ip_or_device_identifier, status) VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?)");
                     PreparedStatement attStmt = conn.prepareStatement("INSERT INTO attendance (student_id, course_id, is_present, updated_at) VALUES (?, ?, 1, CURRENT_TIMESTAMP)")) {
                    String auditId = "A-" + java.util.UUID.randomUUID();
                    auditStmt.setString(1, auditId);
                    auditStmt.setString(2, sessionId);
                    auditStmt.setString(3, studentId);
                    auditStmt.setString(4, deviceIdentifier);
                    auditStmt.setString(5, "ACCEPTED");
                    auditStmt.executeUpdate();

                    attStmt.setString(1, studentId);
                    attStmt.setString(2, courseId);
                    attStmt.executeUpdate();

                    conn.commit();
                } catch (SQLException ex) {
                    try { conn.rollback(); } catch (SQLException ignored) {}
                    logAudit(conn, sessionId, studentId, deviceIdentifier, "ERROR");
                    throw new IllegalStateException("Error recording attendance: " + ex.getMessage(), ex);
                } finally {
                    conn.setAutoCommit(true);
                }

                // Update cached session usage if token must be invalidated after first use across session
                // (We keep session active for the class; tokens are single-use per student via audit unique index.)
                // Update student attendance percentage
                updateStudentAttendancePercentage(studentId);

                // Publish event for UI
                com.university.erp.gui.UIEventBus.publish("ATTENDANCE_MARKED", java.util.Map.of("session_id", sessionId, "student_id", studentId));
                return true;
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error validating attendance session: " + e.getMessage(), e);
        }
    }

    private void logAudit(Connection conn, String sessionId, String studentId, String deviceIdentifier, String status) {
        try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO attendance_audit (audit_id, session_id, student_id, timestamp, ip_or_device_identifier, status) VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?)") ) {
            pstmt.setString(1, "A-" + java.util.UUID.randomUUID());
            pstmt.setString(2, sessionId);
            pstmt.setString(3, studentId);
            pstmt.setString(4, deviceIdentifier);
            pstmt.setString(5, status);
            pstmt.executeUpdate();
        } catch (SQLException ignored) {
            // best effort
        }
    }

    private void updateStudentAttendancePercentage(String studentId) {
        String countSql = "SELECT COUNT(*) as total, SUM(CASE WHEN is_present THEN 1 ELSE 0 END) as present FROM attendance WHERE student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(countSql)) {
            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int total = rs.getInt("total");
                int present = rs.getInt("present");
                if (total > 0) {
                    double percent = ((double) present / total) * 100;
                    Student s = studentManager.searchById(studentId);
                    if (s != null) {
                        s.setAttendancePercentage(percent);
                        studentManager.updateStudent(s);
                    }
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error updating attendance percentage: " + e.getMessage(), e);
        }
    }
}
