package com.university.erp.security;

import com.university.db.DatabaseManager;
import com.university.exceptions.UnauthorizedAccessException;
import com.university.students.StudentManager;
import com.university.utils.AuditLogger;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;

public class AuthenticationManager {
    
    public AuthenticationManager() {}

    public void registerUser(User user, String plaintextPassword) {
        registerUser(user, plaintextPassword, null);
    }

    public void registerUser(User user, String plaintextPassword, String refId) {
        String hash = BCrypt.hashpw(plaintextPassword, BCrypt.gensalt());
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO users (username, password_hash, role, ref_id) VALUES (?, ?, ?, ?) " +
                             "ON CONFLICT(username) DO UPDATE SET " +
                             "password_hash = excluded.password_hash, " +
                             "role = excluded.role, " +
                             "ref_id = excluded.ref_id")) {
            pstmt.setString(1, normalizeUsername(user.getUsername()));
            pstmt.setString(2, hash);
            pstmt.setString(3, user.getRole().getLabel());
            pstmt.setString(4, refId);
            pstmt.executeUpdate();
            AuditLogger.log("SYSTEM", "User Registered: " + user.getUsername() + " as " + user.getRole().getLabel());
        } catch (SQLException e) {
            System.err.println("Error registering user: " + e.getMessage());
        }
    }

    public void ensureUser(User user, String plaintextPassword, String refId) {
        if (!userExists(user.getUsername())) {
            registerUser(user, plaintextPassword, refId);
        } else {
            updateUserReference(user, refId);
        }
    }

    public boolean userExists(String username) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT 1 FROM users WHERE username = ?")) {
            pstmt.setString(1, normalizeUsername(username));
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            System.err.println("User lookup error: " + e.getMessage());
            return false;
        }
    }

    public User authenticate(String username, String password) throws UnauthorizedAccessException {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.isEmpty() || password == null || password.isEmpty()) {
            throw new UnauthorizedAccessException("Username and password are required.");
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT password_hash, role, ref_id FROM users WHERE username = ?")) {
            pstmt.setString(1, normalizedUsername);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                String hash = rs.getString("password_hash");
                String roleStr = rs.getString("role");
                String refId = rs.getString("ref_id");
                
                if (passwordMatches(password, hash)) {
                    User user = createUser(normalizedUsername, hash, roleStr, refId);
                    
                    if (!user.login(normalizedUsername, password)) {
                        throw new UnauthorizedAccessException("Invalid credentials.");
                    }

                    if (user.getRole() == UserRole.STUDENT) {
                        StudentContext.requireStudentForUser(user, new StudentManager());
                    }

                    if (!isBCryptHash(hash)) {
                        upgradePasswordHash(normalizedUsername, password);
                    }

                    AuditLogger.log(normalizedUsername, "Login Successful");
                    return user;
                }
            }
        } catch (SQLException e) {
            System.err.println("Authentication error: " + e.getMessage());
        }
        throw new UnauthorizedAccessException("Invalid credentials.");
    }

    private User createUser(String username, String passwordHash, String roleStr, String refId) {
        UserRole role = UserRole.fromString(roleStr);
        return switch (role) {
            case ADMIN -> new AdminUser(username, passwordHash, refId);
            case FACULTY -> new FacultyUser(username, passwordHash, refId);
            case STUDENT -> new StudentUser(username, passwordHash, refId);
        };
    }

    private boolean passwordMatches(String plaintextPassword, String storedPassword) {
        if (storedPassword == null) {
            return false;
        }

        try {
            boolean match = BCrypt.checkpw(plaintextPassword, storedPassword);
            return match;
        } catch (IllegalArgumentException e) {
            return storedPassword.equals(plaintextPassword);
        }
    }

    private boolean isBCryptHash(String password) {
        return password != null &&
                (password.startsWith("$2a$") || password.startsWith("$2b$") || password.startsWith("$2y$"));
    }

    private void upgradePasswordHash(String username, String plaintextPassword) {
        String hash = BCrypt.hashpw(plaintextPassword, BCrypt.gensalt());
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE users SET password_hash = ? WHERE username = ?")) {
            pstmt.setString(1, hash);
            pstmt.setString(2, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Password upgrade error: " + e.getMessage());
        }
    }

    private void updateUserReference(User user, String refId) {
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "UPDATE users SET role = ?, ref_id = ? WHERE username = ?")) {
            pstmt.setString(1, user.getRole().getLabel());
            pstmt.setString(2, refId);
            pstmt.setString(3, normalizeUsername(user.getUsername()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("User reference update error: " + e.getMessage());
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }
}
