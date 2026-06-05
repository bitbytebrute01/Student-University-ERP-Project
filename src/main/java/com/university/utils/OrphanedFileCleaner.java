package com.university.utils;

import com.university.db.DatabaseManager;

import java.io.IOException;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class OrphanedFileCleaner {
    // Clean files under upload root that are older than thresholdHours and not referenced in DB
    public static void cleanOrphanedFiles(long thresholdHours) {
        Path uploadRoot = Paths.get(System.getProperty("university.upload.dir", "uploads")).toAbsolutePath().normalize();
        if (!Files.exists(uploadRoot)) return;

        try {
            Set<String> referenced = collectReferencedPaths();

            Instant cutoff = ZonedDateTime.now(ZoneId.systemDefault()).toInstant().minusSeconds(TimeUnit.HOURS.toSeconds(thresholdHours));

            Files.walk(uploadRoot)
                    .filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Instant lastModified = Files.getLastModifiedTime(path).toInstant();
                            String normalized = path.toAbsolutePath().normalize().toString();
                            if (lastModified.isBefore(cutoff) && !referenced.contains(normalized)) {
                                try {
                                    Files.deleteIfExists(path);
                                    System.out.println("OrphanedFileCleaner: deleted orphaned file: " + normalized);
                                } catch (IOException e) {
                                    System.err.println("OrphanedFileCleaner: error deleting file " + normalized + ": " + e.getMessage());
                                }
                            }
                        } catch (IOException e) {
                            // ignore individual file errors
                        }
                    });
        } catch (IOException e) {
            System.err.println("OrphanedFileCleaner: error walking upload root: " + e.getMessage());
        }
    }

    private static Set<String> collectReferencedPaths() {
        Set<String> set = new HashSet<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            collectColumnPaths(conn, set, "students", "resume_path");
            collectColumnPaths(conn, set, "students", "profile_pic");
            collectColumnPaths(conn, set, "students", "cover_pic");
            collectColumnPaths(conn, set, "submissions", "file_path");
            collectColumnPaths(conn, set, "assignment_submission_files", "file_path");
            collectColumnPaths(conn, set, "student_projects", "screenshot_path");
            collectColumnPaths(conn, set, "student_projects", "report_path");
            collectColumnPaths(conn, set, "student_projects", "zip_path");
            collectColumnPaths(conn, set, "student_certifications", "file_path");
            collectColumnPaths(conn, set, "student_certifications", "file_path");
            // Add more as needed
        } catch (SQLException e) {
            System.err.println("OrphanedFileCleaner: DB error collecting references: " + e.getMessage());
        }
        return set;
    }

    private static void collectColumnPaths(Connection conn, Set<String> set, String table, String column) {
        String sql = "SELECT " + column + " FROM " + table + " WHERE " + column + " IS NOT NULL AND TRIM(" + column + ") <> ''";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String value = rs.getString(1);
                if (value == null || value.isBlank()) continue;
                // submissions.file_path may contain multiple paths separated by newline
                for (String part : value.split("\\n")) {
                    if (part == null) continue;
                    Path p = Paths.get(part.trim()).toAbsolutePath().normalize();
                    set.add(p.toString());
                }
            }
        } catch (SQLException e) {
            // ignore table/column not existing
        }
    }
}
