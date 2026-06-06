package com.university.erp.dao;

import com.university.db.DatabaseManager;
import com.university.models.Project;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjectDAO {
    public void addProject(Project p) throws SQLException {
        String sql = "INSERT INTO student_projects (id, student_id, title, description, screenshot_path, report_path, zip_path, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseManager.getConnection();
        boolean committed = false;
        List<String> finalized = new java.util.ArrayList<>();
        try {
            conn.setAutoCommit(false);

            // finalize temp uploads into project-specific folders
            try {
                if (p.getScreenshotPath() != null && com.university.utils.MediaManager.isTempPath(p.getScreenshotPath())) {
                    String finalPath = com.university.utils.MediaManager.finalizeUpload(p.getScreenshotPath(), "projects/screenshots/" + p.getId());
                    p.setScreenshotPath(finalPath);
                    finalized.add(finalPath);
                }
                if (p.getReportPath() != null && com.university.utils.MediaManager.isTempPath(p.getReportPath())) {
                    String finalPath = com.university.utils.MediaManager.finalizeUpload(p.getReportPath(), "projects/reports/" + p.getId());
                    p.setReportPath(finalPath);
                    finalized.add(finalPath);
                }
                if (p.getZipPath() != null && com.university.utils.MediaManager.isTempPath(p.getZipPath())) {
                    String finalPath = com.university.utils.MediaManager.finalizeUpload(p.getZipPath(), "projects/source/" + p.getId());
                    p.setZipPath(finalPath);
                    finalized.add(finalPath);
                }
            } catch (java.io.IOException ioe) {
                // cleanup any finalized and rethrow as SQLException
                for (String fp : finalized) com.university.utils.MediaManager.deleteFile(fp);
                throw new SQLException("Failed to finalize project files: " + ioe.getMessage(), ioe);
            }

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, p.getId());
                pstmt.setString(2, p.getStudentId());
                pstmt.setString(3, p.getTitle());
                pstmt.setString(4, p.getDescription());
                pstmt.setString(5, p.getScreenshotPath());
                pstmt.setString(6, p.getReportPath());
                pstmt.setString(7, p.getZipPath());
                pstmt.setString(8, p.getStatus());
                pstmt.executeUpdate();
            }

            conn.commit();
            committed = true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (Exception ignored) {}
            // cleanup finalized files to avoid orphans
            for (String fp : finalized) com.university.utils.MediaManager.deleteFile(fp);
            throw e;
        } finally {
            try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
        }
    }

    public List<Project> getProjectsByStudent(String studentId) throws SQLException {
        List<Project> projects = new ArrayList<>();
        String sql = "SELECT * FROM student_projects WHERE student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Project p = new Project(rs.getString("id"), rs.getString("student_id"), rs.getString("title"), rs.getString("description"));
                p.setScreenshotPath(rs.getString("screenshot_path"));
                p.setReportPath(rs.getString("report_path"));
                p.setZipPath(rs.getString("zip_path"));
                p.setStatus(rs.getString("status"));
                p.setFacultyFeedback(rs.getString("faculty_feedback"));
                projects.add(p);
            }
        }
        return projects;
    }

    public void updateProjectStatus(String projectId, String status, String feedback) throws SQLException {
        String sql = "UPDATE student_projects SET status = ?, faculty_feedback = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setString(2, feedback);
            pstmt.setString(3, projectId);
            pstmt.executeUpdate();
        }
    }

    public int getProjectCount(String studentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM student_projects WHERE student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public void deleteProject(String projectId, String studentId) throws SQLException {
        String sql = "DELETE FROM student_projects WHERE id = ? AND student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, projectId);
            pstmt.setString(2, studentId);
            pstmt.executeUpdate();
        }
    }

    public boolean exists(String projectId) throws SQLException {
        String sql = "SELECT 1 FROM student_projects WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, projectId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }
}
