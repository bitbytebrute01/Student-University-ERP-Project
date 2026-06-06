package com.university.erp.dao;

import com.university.db.DatabaseManager;
import com.university.models.Certification;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CertificationDAO {
    public void addCertification(Certification c) throws SQLException {
        String sql = "INSERT INTO student_certifications (id, student_id, title, issuer, completion_date, file_path) VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = DatabaseManager.getConnection();
        boolean committed = false;
        String finalized = null;
        try {
            conn.setAutoCommit(false);
            if (c.getFilePath() != null && com.university.utils.MediaManager.isTempPath(c.getFilePath())) {
                finalized = com.university.utils.MediaManager.finalizeUpload(c.getFilePath(), "certifications/" + c.getStudentId());
                c.setFilePath(finalized);
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, c.getId());
                pstmt.setString(2, c.getStudentId());
                pstmt.setString(3, c.getTitle());
                pstmt.setString(4, c.getIssuer());
                pstmt.setDate(5, new java.sql.Date(c.getCompletionDate().getTime()));
                pstmt.setString(6, c.getFilePath());
                pstmt.executeUpdate();
            }
            conn.commit();
            committed = true;
        } catch (SQLException e) {
            try { conn.rollback(); } catch (Exception ignored) {}
            if (finalized != null) com.university.utils.MediaManager.deleteFile(finalized);
            throw e;
        } catch (java.io.IOException ioe) {
            if (finalized != null) com.university.utils.MediaManager.deleteFile(finalized);
            throw new SQLException("Failed to finalize certification file: " + ioe.getMessage(), ioe);
        } finally {
            try { conn.setAutoCommit(true); conn.close(); } catch (Exception ignored) {}
        }
    }

    public List<Certification> getCertificationsByStudent(String studentId) throws SQLException {
        List<Certification> certs = new ArrayList<>();
        String sql = "SELECT * FROM student_certifications WHERE student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                Certification c = new Certification(rs.getString("id"), rs.getString("student_id"), rs.getString("title"), rs.getString("issuer"), rs.getDate("completion_date"));
                c.setFilePath(rs.getString("file_path"));
                certs.add(c);
            }
        }
        return certs;
    }

    public int getCertificationCount(String studentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM student_certifications WHERE student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public boolean exists(String certificationId) throws SQLException {
        String sql = "SELECT 1 FROM student_certifications WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, certificationId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }
}
