package com.university.erp.lms;

import com.university.db.DatabaseManager;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ForumManager {
    public static void postMessage(String courseId, String userId, String message) {
        String sql = "INSERT INTO forum_posts (course_id, user_id, message) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseId);
            pstmt.setString(2, userId);
            pstmt.setString(3, message);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static List<String> getPosts(String courseId) {
        List<String> posts = new ArrayList<>();
        String sql = "SELECT user_id, message, timestamp FROM forum_posts WHERE course_id = ? ORDER BY timestamp DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                posts.add("[" + rs.getString("timestamp") + "] " + rs.getString("user_id") + ": " + rs.getString("message"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return posts;
    }
}
