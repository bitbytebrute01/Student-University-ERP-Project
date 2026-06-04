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
    private static Map<String, String> activeAttendanceCodes = new HashMap<>(); // courseId -> code

    public AttendanceManager(StudentManager studentManager) {
        this.studentManager = studentManager;
    }

    public String generateAttendanceCode(String courseId) {
        String code = String.format("%06d", new Random().nextInt(999999));
        activeAttendanceCodes.put(courseId, code);
        return code;
    }

    public boolean markAttendanceWithCode(String studentId, String courseId, String code) throws InvalidAttendanceException {
        String validCode = activeAttendanceCodes.get(courseId);
        if (validCode != null && validCode.equals(code)) {
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
