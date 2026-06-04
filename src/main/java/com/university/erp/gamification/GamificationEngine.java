package com.university.erp.gamification;

import com.university.db.DatabaseManager;
import com.university.utils.AuditLogger;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class GamificationEngine {
    private static final int XP_LOGIN = 10;
    private static final int XP_SUBMISSION = 50;
    private static final int XP_ATTENDANCE = 5;

    public static void awardXP(String studentId, String activity) {
        int amount = switch (activity.toUpperCase()) {
            case "LOGIN" -> XP_LOGIN;
            case "SUBMISSION" -> XP_SUBMISSION;
            case "ATTENDANCE" -> XP_ATTENDANCE;
            default -> 0;
        };

        if (amount == 0) return;

        String sql = "UPDATE students SET xp_points = xp_points + ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, amount);
            pstmt.setString(2, studentId);
            pstmt.executeUpdate();
            
            updateLevel(studentId);
            AuditLogger.log(studentId, "Awarded " + amount + " XP for " + activity);
        } catch (SQLException e) {
            System.err.println("XP Award Error: " + e.getMessage());
        }
    }

    private static void updateLevel(String studentId) {
        String sql = "UPDATE students SET level = (xp_points / 500) + 1 WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Level Update Error: " + e.getMessage());
        }
    }

    public static Map<String, Integer> getLeaderboard() {
        Map<String, Integer> board = new HashMap<>();
        String sql = "SELECT name, xp_points FROM students ORDER BY xp_points DESC LIMIT 10";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                board.put(rs.getString("name"), rs.getInt("xp_points"));
            }
        } catch (SQLException e) {
            System.err.println("Leaderboard Fetch Error: " + e.getMessage());
        }
        return board;
    }
}
