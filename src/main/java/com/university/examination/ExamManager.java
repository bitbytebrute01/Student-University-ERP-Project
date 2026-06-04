package com.university.examination;

import com.university.db.DatabaseManager;
import com.university.exceptions.InvalidGradeException;
import com.university.interfaces.ResultGenerator;
import com.university.models.Student;
import com.university.students.StudentManager;

import java.sql.*;
import java.util.HashMap;

public class ExamManager implements ResultGenerator {
    private StudentManager studentManager;

    public ExamManager(StudentManager studentManager) {
        this.studentManager = studentManager;
    }

    public void uploadMarks(String studentId, String courseId, double marks) throws InvalidGradeException {
        if (marks < 0 || marks > 100) throw new InvalidGradeException("Invalid marks");
        
        String sql = "INSERT INTO marks (student_id, course_id, marks) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, courseId);
            pstmt.setDouble(3, marks);
            pstmt.executeUpdate();
            
            updateStudentCgpa(studentId);
        } catch (SQLException e) {
            System.err.println("Error uploading marks: " + e.getMessage());
        }
    }

    private void updateStudentCgpa(String studentId) {
        String sql = "SELECT marks FROM marks WHERE student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            
            double totalGpa = 0;
            int count = 0;
            while (rs.next()) {
                double m = rs.getDouble("marks");
                try {
                    totalGpa += GradeCalculator.calculateGPA(m);
                    count++;
                } catch (InvalidGradeException ignored) {}
            }
            
            if (count > 0) {
                double cgpa = totalGpa / count;
                Student s = studentManager.searchById(studentId);
                if (s != null) {
                    s.setCgpa(cgpa);
                    studentManager.updateStudent(s);
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating CGPA: " + e.getMessage());
        }
    }

    @Override
    public String generateResult(String studentId) {
        Student s = studentManager.searchById(studentId);
        if (s == null) return "Student not found.";
        
        StringBuilder sb = new StringBuilder();
        sb.append("--- RESULT FOR ").append(s.getName()).append(" ---\n");
        sb.append("CGPA: ").append(String.format("%.2f", s.getCgpa())).append("\n");
        
        String sql = "SELECT course_id, marks FROM marks WHERE student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String cId = rs.getString("course_id");
                double m = rs.getDouble("marks");
                try {
                    sb.append("Course ").append(cId).append(": ")
                      .append(m).append(" (")
                      .append(GradeCalculator.getGrade(m)).append(")\n");
                } catch (InvalidGradeException e) {
                    sb.append("Error calculating grade for ").append(cId).append("\n");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error generating result: " + e.getMessage());
        }
        return sb.toString();
    }
}
