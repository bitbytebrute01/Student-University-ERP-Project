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
    private static Map<String, String> activeAttendanceCodes = new HashMap<>(); // courseId -> code (cache)

    public AttendanceManager(StudentManager studentManager) {
        this.studentManager = studentManager;
    }

    public String generateAttendanceCode(String courseId) {
        String code = String.format("%06d", new Random().nextInt(999999));
        activeAttendanceCodes.put(courseId, code);
        // Persist code with expiry (default 2 hours)
        String id = "AC-" + java.util.UUID.randomUUID();
        String sql = "INSERT INTO attendance_codes (id, course_id, code, created_by, expires_at, active) VALUES (?, ?, ?, ?, datetime('now','+2 hours'), 1)";
        try (Connection conn = com.university.db.DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, courseId);
            pstmt.setString(3, code);
            pstmt.setString(4, "system");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error persisting attendance code: " + e.getMessage());
        }
        return code;
    }

    public boolean markAttendanceWithCode(String studentId, String courseId, String code) throws InvalidAttendanceException {
        // Validate against persistent codes table where active=1 and not expired
        String sql = "SELECT code FROM attendance_codes WHERE course_id = ? AND active = 1 AND (expires_at IS NULL OR datetime(expires_at) >= datetime('now')) ORDER BY created_at DESC LIMIT 1";
        try (Connection conn = com.university.db.DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String validCode = rs.getString(1);
                if (validCode != null && validCode.equals(code)) {
                    markAttendance(studentId, courseId, true);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error validating attendance code: " + e.getMessage());
        }

        // Fallback: check in-memory cache
        String cached = activeAttendanceCodes.get(courseId);
        if (cached != null && cached.equals(code)) {
            markAttendance(studentId, courseId, true);
            return true;
        }
        return false;
    }

    public void markAttendance(String studentId, String courseId, boolean isPresent) throws InvalidAttendanceException {
        if (studentManager.searchById(studentId) == null) {
            throw new InvalidAttendanceException("Cannot mark attendance for unknown student.");
        }
        
        String sql = "INSERT INTO attendance (student_id, course_id, is_present) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseId);
            pstmt.setBoolean(3, isPresent);
            pstmt.executeUpdate();
            
            updateStudentAttendancePercentage(studentId);
        } catch (SQLException e) {
            System.err.println("Error marking attendance: " + e.getMessage());
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
            System.err.println("Error updating attendance percentage: " + e.getMessage());
        }
    }
}
