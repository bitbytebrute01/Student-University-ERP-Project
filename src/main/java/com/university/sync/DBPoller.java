package com.university.sync;

import com.university.db.DatabaseManager;
import com.university.erp.gui.UIEventBus;
import com.university.erp.security.SessionManager;
import com.university.erp.security.User;
import com.university.erp.security.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DBPoller {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "db-poller"));
    private volatile Instant lastAssignmentTs = Instant.EPOCH;
    private volatile Instant lastNotificationTs = Instant.EPOCH;
    private volatile Instant lastGradeTs = Instant.EPOCH;
    private volatile Instant lastProfileTs = Instant.EPOCH;
    private volatile Instant lastAttendanceTs = Instant.EPOCH;

    private final int intervalSeconds;

    public DBPoller(int intervalSeconds) {
        this.intervalSeconds = Math.max(2, intervalSeconds);
    }

    public void start() {
        executor.scheduleAtFixedRate(this::poll, 0, intervalSeconds, TimeUnit.SECONDS);
    }

    public void stop() {
        executor.shutdownNow();
    }

    private void poll() {
        try {
            User user = SessionManager.getCurrentUser();
            if (user == null) return;
            if (user.getRole() == UserRole.STUDENT) {
                pollForStudent(user);
            } else if (user.getRole() == UserRole.FACULTY) {
                pollForFaculty(user);
            } else {
                pollForAdmin(user);
            }
        } catch (Exception e) {
            // Don't let exceptions kill the poller
            System.err.println("DBPoller error: " + e.getMessage());
        }
    }

    private void pollForStudent(User user) {
        String studentId = user.getRefId() != null ? user.getRefId() : user.getUsername();
        try (Connection conn = DatabaseManager.getConnection()) {
            Instant assignTs = queryInstant(conn, "SELECT MAX(a.created_at) FROM assignments a JOIN enrollments e ON e.course_id = a.course_id WHERE e.student_id = ?", studentId);
            if (assignTs != null && assignTs.isAfter(lastAssignmentTs)) {
                lastAssignmentTs = assignTs;
                UIEventBus.publish("ASSIGNMENT_PUBLISHED", null);
            }

            Instant noteTs = queryInstant(conn, "SELECT MAX(created_at) FROM assignment_notifications WHERE student_id = ?", studentId);
            if (noteTs != null && noteTs.isAfter(lastNotificationTs)) {
                lastNotificationTs = noteTs;
                UIEventBus.publish("NOTIFICATION_CREATED", null);
            }

            Instant gradeTs = queryInstant(conn, "SELECT MAX(graded_at) FROM assignment_submissions WHERE student_id = ?", studentId);
            if (gradeTs != null && gradeTs.isAfter(lastGradeTs)) {
                lastGradeTs = gradeTs;
                UIEventBus.publish("ASSIGNMENT_GRADED", null);
            }

            Instant profileTs = queryInstant(conn, "SELECT MAX(profile_updated_at) FROM students WHERE id = ?", studentId);
            if (profileTs != null && profileTs.isAfter(lastProfileTs)) {
                lastProfileTs = profileTs;
                UIEventBus.publish("PROFILE_UPDATED", null);
            }

            Instant attendanceTs = queryInstant(conn, "SELECT MAX(updated_at) FROM attendance WHERE student_id = ?", studentId);
            if (attendanceTs != null && attendanceTs.isAfter(lastAttendanceTs)) {
                lastAttendanceTs = attendanceTs;
                UIEventBus.publish("ATTENDANCE_UPDATED", null);
            }
        } catch (SQLException e) {
            System.err.println("DBPoller student poll error: " + e.getMessage());
        }
    }

    private void pollForFaculty(User user) {
        String facultyId = user.getRefId() != null ? user.getRefId() : user.getUsername();
        try (Connection conn = DatabaseManager.getConnection()) {
            // check assignments created by faculty
            Instant assignTs = queryInstant(conn, "SELECT MAX(created_at) FROM assignments WHERE created_by = ? OR course_id IN (SELECT course_id FROM courses WHERE faculty_id = ?)", facultyId, facultyId);
            if (assignTs != null && assignTs.isAfter(lastAssignmentTs)) {
                lastAssignmentTs = assignTs;
                UIEventBus.publish("ASSIGNMENT_PUBLISHED", null);
            }
            // submissions to assignments of this faculty
            Instant subTs = queryInstant(conn, "SELECT MAX(submitted_at) FROM assignment_submissions WHERE assignment_id IN (SELECT id FROM assignments WHERE course_id IN (SELECT course_id FROM courses WHERE faculty_id = ?))", facultyId);
            if (subTs != null && subTs.isAfter(lastNotificationTs)) {
                lastNotificationTs = subTs;
                UIEventBus.publish("SUBMISSION_CREATED", null);
            }
            // graded changes
            Instant gradeTs = queryInstant(conn, "SELECT MAX(graded_at) FROM assignment_submissions WHERE assignment_id IN (SELECT id FROM assignments WHERE course_id IN (SELECT course_id FROM courses WHERE faculty_id = ?))", facultyId);
            if (gradeTs != null && gradeTs.isAfter(lastGradeTs)) {
                lastGradeTs = gradeTs;
                UIEventBus.publish("ASSIGNMENT_GRADED", null);
            }

            Instant attendanceTs = queryInstant(conn, "SELECT MAX(updated_at) FROM attendance WHERE course_id IN (SELECT course_id FROM courses WHERE faculty_id = ?)", facultyId);
            if (attendanceTs != null && attendanceTs.isAfter(lastAttendanceTs)) {
                lastAttendanceTs = attendanceTs;
                UIEventBus.publish("ATTENDANCE_UPDATED", null);
            }
        } catch (SQLException e) {
            System.err.println("DBPoller faculty poll error: " + e.getMessage());
        }
    }

    private void pollForAdmin(User user) {
        try (Connection conn = DatabaseManager.getConnection()) {
            Instant assignTs = queryInstant(conn, "SELECT MAX(created_at) FROM assignments");
            if (assignTs != null && assignTs.isAfter(lastAssignmentTs)) {
                lastAssignmentTs = assignTs;
                UIEventBus.publish("ASSIGNMENT_PUBLISHED", null);
            }

            Instant noteTs = queryInstant(conn, "SELECT MAX(created_at) FROM assignment_notifications");
            if (noteTs != null && noteTs.isAfter(lastNotificationTs)) {
                lastNotificationTs = noteTs;
                UIEventBus.publish("NOTIFICATION_CREATED", null);
            }

            Instant gradeTs = queryInstant(conn, "SELECT MAX(graded_at) FROM assignment_submissions");
            if (gradeTs != null && gradeTs.isAfter(lastGradeTs)) {
                lastGradeTs = gradeTs;
                UIEventBus.publish("ASSIGNMENT_GRADED", null);
            }

            Instant attendanceTs = queryInstant(conn, "SELECT MAX(updated_at) FROM attendance");
            if (attendanceTs != null && attendanceTs.isAfter(lastAttendanceTs)) {
                lastAttendanceTs = attendanceTs;
                UIEventBus.publish("ATTENDANCE_UPDATED", null);
            }
        } catch (SQLException e) {
            System.err.println("DBPoller admin poll error: " + e.getMessage());
        }
    }

    private Instant queryInstant(Connection conn, String sql, String... params) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) pstmt.setString(i + 1, params[i]);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp ts = rs.getTimestamp(1);
                    if (ts != null) {
                        return ts.toInstant();
                    }
                }
            }
        }
        return null;
    }
}
