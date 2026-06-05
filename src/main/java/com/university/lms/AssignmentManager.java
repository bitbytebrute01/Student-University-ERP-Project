package com.university.lms;

import com.university.db.DatabaseManager;
import com.university.erp.security.SessionManager;
import com.university.erp.security.User;
import com.university.models.Assignment;
import com.university.models.AssignmentNotification;
import com.university.models.Submission;
import com.university.utils.MediaManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class AssignmentManager {
    private static final String STATUS_PUBLISHED = "Published";
    private static final String STATUS_SUBMITTED = "Submitted";
    private static final String STATUS_LATE = "Late";
    private static final String STATUS_GRADED = "Graded";
    private static final String STATUS_PENDING = "Pending";
    private static final String STATUS_OVERDUE = "Overdue";
    private static final DateTimeFormatter DUE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter SQL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public AssignmentManager() {}

    public static class FacultyAssignmentMetrics {
        private final int totalAssignments;
        private final int pendingReviews;
        private final int lateSubmissions;
        private final int recentSubmissions;

        public FacultyAssignmentMetrics(int totalAssignments, int pendingReviews, int lateSubmissions, int recentSubmissions) {
            this.totalAssignments = totalAssignments;
            this.pendingReviews = pendingReviews;
            this.lateSubmissions = lateSubmissions;
            this.recentSubmissions = recentSubmissions;
        }

        public int getTotalAssignments() { return totalAssignments; }
        public int getPendingReviews() { return pendingReviews; }
        public int getLateSubmissions() { return lateSubmissions; }
        public int getRecentSubmissions() { return recentSubmissions; }
    }

    public void createAssignment(Assignment assignment) {
        validateAssignment(assignment);

        String assignmentId = assignment.getId().trim();
        Date deadline = assignment.getDeadline();
        Date createdAt = assignment.getCreatedAt() == null ? new Date() : assignment.getCreatedAt();
        String dueTime = normalizeDueTime(assignment.getDueTime(), deadline);
        String createdBy = resolveCreator(assignment);
        String status = isBlank(assignment.getStatus()) ? STATUS_PUBLISHED : assignment.getStatus().trim();

        String sql = "INSERT INTO assignments (" +
                "id, assignment_id, course_id, created_by, title, description, deadline, due_date, due_time, " +
                "created_at, status, max_marks, attachment_path, type, materials) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(id) DO UPDATE SET " +
                "assignment_id = excluded.assignment_id, " +
                "course_id = excluded.course_id, " +
                "created_by = excluded.created_by, " +
                "title = excluded.title, " +
                "description = excluded.description, " +
                "deadline = excluded.deadline, " +
                "due_date = excluded.due_date, " +
                "due_time = excluded.due_time, " +
                "status = excluded.status, " +
                "max_marks = excluded.max_marks, " +
                "attachment_path = excluded.attachment_path, " +
                "type = excluded.type, " +
                "materials = excluded.materials";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!courseExists(conn, assignment.getCourseId())) {
                    throw new IllegalStateException("Course not found: " + assignment.getCourseId());
                }

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, assignmentId);
                    pstmt.setString(2, assignmentId);
                    pstmt.setString(3, assignment.getCourseId());
                    pstmt.setString(4, createdBy);
                    pstmt.setString(5, assignment.getTitle().trim());
                    pstmt.setString(6, assignment.getDescription());
                    pstmt.setString(7, formatSqlDateTime(deadline));
                    pstmt.setString(8, formatDueDate(deadline));
                    pstmt.setString(9, dueTime);
                    pstmt.setString(10, formatSqlDateTime(createdAt));
                    pstmt.setString(11, status);
                    pstmt.setDouble(12, assignment.getMaxMarks());
                    pstmt.setString(13, assignment.getAttachmentPath());
                    pstmt.setString(14, assignment.getAssignmentType());
                    pstmt.setString(15, assignment.getAdditionalMaterialPath());
                    pstmt.executeUpdate();
                }

                createNotificationsForAssignment(conn, assignmentId, assignment.getCourseId(),
                        assignment.getTitle().trim(), deadline);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                if (e instanceof SQLException sqlException) {
                    throw sqlException;
                }
                throw new IllegalStateException("Error creating assignment: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error creating assignment: " + e.getMessage(), e);
        }

        assignment.setCreatedBy(createdBy);
        assignment.setDueTime(dueTime);
        assignment.setCreatedAt(createdAt);
        assignment.setStatus(status);
    }

    public void submitAssignment(Submission submission) {
        if (submission == null) {
            throw new IllegalArgumentException("Submission is required.");
        }
        validateSubmissionAllowed(submission.getStudentId(), submission.getAssignmentId());
        String submissionText = normalizeSubmissionText(submission.getSubmissionText());
        List<String> attachmentPaths = normalizeAttachmentPaths(submission);
        validateSubmissionContent(submissionText, attachmentPaths);

        Assignment assignment = getAssignmentById(submission.getAssignmentId());
        if (assignment == null) {
            throw new IllegalStateException("Assignment not found: " + submission.getAssignmentId());
        }

        Date submittedAt = submission.getSubmissionDate() == null ? new Date() : submission.getSubmissionDate();
        String status = determineSubmissionStatus(assignment, submittedAt);
        String submittedAtSql = formatSqlDateTime(submittedAt);

        String sql = "INSERT INTO submissions (" +
                "id, submission_id, assignment_id, student_id, submission_text, file_path, submission_date, submitted_at, status, marks, feedback, is_graded) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, 0) " +
                "ON CONFLICT(assignment_id, student_id) DO UPDATE SET " +
                "id = excluded.id, " +
                "submission_id = excluded.submission_id, " +
                "submission_text = excluded.submission_text, " +
                "file_path = excluded.file_path, " +
                "submission_date = excluded.submission_date, " +
                "submitted_at = excluded.submitted_at, " +
                "status = excluded.status, " +
                "marks = NULL, " +
                "feedback = NULL, " +
                "is_graded = 0";
        boolean committed = false;
        List<String> finalizedPaths = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String submissionId = resolveSubmissionId(conn, submission);

                // Finalize any temp uploads to permanent location before inserting records
                for (String p : attachmentPaths) {
                    if (MediaManager.isTempPath(p)) {
                        try {
                            String finalPath = MediaManager.finalizeUpload(p, "assignments/" + submission.getAssignmentId() + "/submissions/" + submissionId);
                            finalizedPaths.add(finalPath);
                        } catch (IOException io) {
                            throw new IllegalStateException("Failed to finalize uploaded file: " + p + " -> " + io.getMessage(), io);
                        }
                    } else {
                        finalizedPaths.add(p);
                    }
                }

                String encodedFilePaths = encodeFilePaths(finalizedPaths);

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, submissionId);
                    pstmt.setString(2, submissionId);
                    pstmt.setString(3, submission.getAssignmentId());
                    pstmt.setString(4, submission.getStudentId());
                    pstmt.setString(5, submissionText);
                    pstmt.setString(6, encodedFilePaths);
                    pstmt.setString(7, submittedAtSql);
                    pstmt.setString(8, submittedAtSql);
                    pstmt.setString(9, status);
                    pstmt.executeUpdate();
                }
                upsertAssignmentSubmissionRecord(conn, submissionId, submission.getAssignmentId(),
                        submission.getStudentId(), submissionText, encodedFilePaths, submittedAtSql, status);
                replaceSubmissionFiles(conn, submissionId, finalizedPaths);
                markAssignmentNotificationRead(conn, submission.getStudentId(), submission.getAssignmentId());
                conn.commit();
                committed = true;
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                if (e instanceof SQLException sqlException) {
                    throw sqlException;
                }
                throw new IllegalStateException("Error submitting assignment: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            // If DB write failed, attempt to remove any attachment files saved earlier to avoid orphans
            try {
                if (!committed && finalizedPaths != null) {
                    for (String p : finalizedPaths) {
                        try {
                            java.nio.file.Files.deleteIfExists(java.nio.file.Path.of(p));
                        } catch (Exception ex) {
                            // log and continue
                            System.err.println("Failed to cleanup attachment after failed submission: " + p + " -> " + ex.getMessage());
                        }
                    }
                }
            } catch (Exception ignored) {}
            throw new IllegalStateException("Error submitting assignment: " + e.getMessage(), e);
        }

        submission.setSubmissionText(submissionText);
        submission.setAttachmentPaths(attachmentPaths);
        submission.setSubmissionDate(submittedAt);
        submission.setStatus(status);
        submission.setGraded(false);
    }

    public Assignment getAssignmentById(String assignmentId) {
        String sql = "SELECT * FROM assignments WHERE id = ? OR assignment_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, assignmentId);
            pstmt.setString(2, assignmentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return mapResultSetToAssignment(rs);
        } catch (SQLException e) {
            throw new IllegalStateException("Error loading assignment: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Assignment> getAssignmentsByCourse(String courseId) {
        List<Assignment> list = new ArrayList<>();
        String sql = "SELECT * FROM assignments WHERE course_id = ? ORDER BY deadline ASC, title ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToAssignment(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error getting assignments: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Assignment> getAssignmentsForStudent(String studentId) {
        List<Assignment> list = new ArrayList<>();
        String sql = "SELECT a.* FROM assignments a " +
                "JOIN enrollments e ON e.course_id = a.course_id " +
                "WHERE e.student_id = ? AND COALESCE(a.status, ?) = ? " +
                "ORDER BY a.deadline ASC, a.title ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, STATUS_PUBLISHED);
            pstmt.setString(3, STATUS_PUBLISHED);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToAssignment(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error getting student assignments: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Submission> getSubmissionsByAssignment(String assignmentId) {
        refreshLateStatusForAssignment(assignmentId);

        List<Submission> list = new ArrayList<>();
        String sql = "SELECT * FROM submissions WHERE assignment_id = ? ORDER BY COALESCE(submitted_at, submission_date) DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, assignmentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                list.add(mapResultSetToSubmission(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error getting submissions: " + e.getMessage(), e);
        }
        return list;
    }

    public List<Submission> getSubmissionOverviewByAssignment(String assignmentId) {
        refreshLateStatusForAssignment(assignmentId);

        Assignment assignment = getAssignmentById(assignmentId);
        if (assignment == null) {
            throw new IllegalStateException("Assignment not found: " + assignmentId);
        }

        List<Submission> list = new ArrayList<>();
        String sql = "SELECT e.student_id AS enrolled_student_id, s.* " +
                "FROM assignments a " +
                "JOIN enrollments e ON e.course_id = a.course_id " +
                "LEFT JOIN submissions s ON s.assignment_id = a.id AND s.student_id = e.student_id " +
                "WHERE a.id = ? " +
                "ORDER BY e.student_id ASC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, assignmentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                if (rs.getString("id") == null) {
                    Submission pending = new Submission(null, assignmentId, rs.getString("enrolled_student_id"), null);
                    pending.setSubmissionDate(null);
                    pending.setStatus(isDeadlinePassed(assignment) ? STATUS_OVERDUE : STATUS_PENDING);
                    pending.setGraded(false);
                    list.add(pending);
                } else {
                    list.add(mapResultSetToSubmission(rs));
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error getting submission overview: " + e.getMessage(), e);
        }
        return list;
    }

    public Submission getStudentSubmission(String studentId, String assignmentId) {
        refreshLateStatusForAssignment(assignmentId);

        String sql = "SELECT * FROM submissions WHERE student_id = ? AND assignment_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, assignmentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToSubmission(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error getting student submission: " + e.getMessage(), e);
        }
        return null;
    }

    public Submission getSubmissionById(String submissionId) {
        String sql = "SELECT * FROM submissions WHERE id = ? OR submission_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, submissionId);
            pstmt.setString(2, submissionId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToSubmission(rs);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error getting submission: " + e.getMessage(), e);
        }
        return null;
    }

    public void gradeSubmission(String submissionId, double marks, String feedback) {
        if (marks < 0) {
            throw new IllegalArgumentException("Marks cannot be negative.");
        }

        String sql = "UPDATE submissions SET marks = ?, feedback = ?, is_graded = 1, status = ? WHERE id = ? OR submission_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, marks);
            pstmt.setString(2, feedback);
            pstmt.setString(3, STATUS_GRADED);
            pstmt.setString(4, submissionId);
            pstmt.setString(5, submissionId);
            if (pstmt.executeUpdate() == 0) {
                throw new IllegalStateException("Submission not found: " + submissionId);
            }
            updateAssignmentSubmissionGrade(conn, submissionId, marks, feedback);
        } catch (SQLException e) {
            throw new IllegalStateException("Error grading submission: " + e.getMessage(), e);
        }
    }

    public Path downloadSubmission(String submissionId, File destinationDirectory) {
        List<Path> downloadedFiles = downloadSubmissionFiles(submissionId, destinationDirectory);
        return downloadedFiles.get(0);
    }

    public List<Path> downloadSubmissionFiles(String submissionId, File destinationDirectory) {
        Submission submission = getSubmissionById(submissionId);
        if (submission == null) {
            throw new IllegalStateException("Submission not found: " + submissionId);
        }
        List<String> attachmentPaths = submission.getAttachmentPaths();
        if (attachmentPaths.isEmpty()) {
            throw new IllegalStateException("No submitted file is available for this submission.");
        }

        Path destinationDir = destinationDirectory.toPath().toAbsolutePath().normalize();
        try {
            if (Files.exists(destinationDir) && !Files.isDirectory(destinationDir)) {
                throw new IOException("Download destination must be a directory.");
            }
            Files.createDirectories(destinationDir);
            List<Path> downloaded = new ArrayList<>();
            for (String path : attachmentPaths) {
                Path source = Path.of(path).normalize();
                if (!Files.isRegularFile(source)) {
                    throw new IllegalStateException("Submitted file is missing: " + source);
                }
                Path destination = destinationDir.resolve(source.getFileName()).normalize();
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                downloaded.add(destination);
            }
            return downloaded;
        } catch (IOException e) {
            throw new IllegalStateException("Error downloading submission: " + e.getMessage(), e);
        }
    }

    public List<AssignmentNotification> getUnreadAssignmentNotifications(String studentId) {
        List<AssignmentNotification> notifications = new ArrayList<>();
        String sql = "SELECT * FROM assignment_notifications " +
                "WHERE student_id = ? AND read_at IS NULL " +
                "ORDER BY " + sqliteDateTimeExpression("created_at") + " DESC";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                notifications.add(mapResultSetToNotification(rs));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error loading assignment notifications: " + e.getMessage(), e);
        }
        return notifications;
    }

    public int getUnreadAssignmentNotificationCount(String studentId) {
        String sql = "SELECT COUNT(*) FROM assignment_notifications WHERE student_id = ? AND read_at IS NULL";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Error counting assignment notifications: " + e.getMessage(), e);
        }
    }

    public void markAssignmentNotificationRead(String studentId, String assignmentId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            markAssignmentNotificationRead(conn, studentId, assignmentId);
        } catch (SQLException e) {
            throw new IllegalStateException("Error updating assignment notification: " + e.getMessage(), e);
        }
    }

    public void createNotificationsForStudentCourse(String studentId, String courseId) {
        String sql = "SELECT id, course_id, title, deadline FROM assignments " +
                "WHERE course_id = ? AND COALESCE(status, ?) = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseId);
            pstmt.setString(2, STATUS_PUBLISHED);
            pstmt.setString(3, STATUS_PUBLISHED);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                insertAssignmentNotification(conn,
                        studentId,
                        rs.getString("id"),
                        rs.getString("course_id"),
                        rs.getString("title"),
                        getOptionalTimestampAsDate(rs, "deadline"));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error creating assignment notifications for enrollment: " + e.getMessage(), e);
        }
    }

    public void createNotificationsForStudentCourse(Connection conn, String studentId, String courseId) throws SQLException {
        String sql = "SELECT id, course_id, title, deadline FROM assignments " +
                "WHERE course_id = ? AND COALESCE(status, ?) = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseId);
            pstmt.setString(2, STATUS_PUBLISHED);
            pstmt.setString(3, STATUS_PUBLISHED);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                insertAssignmentNotification(conn,
                        studentId,
                        rs.getString("id"),
                        rs.getString("course_id"),
                        rs.getString("title"),
                        getOptionalTimestampAsDate(rs, "deadline"));
            }
        }
    }

    public FacultyAssignmentMetrics getFacultyAssignmentMetrics(String facultyId, boolean adminMode) {
        return new FacultyAssignmentMetrics(
                countFacultyAssignments(facultyId, adminMode),
                countFacultySubmissions(facultyId, adminMode,
                        "(s.file_path IS NOT NULL OR s.submission_text IS NOT NULL) AND COALESCE(s.is_graded, 0) = 0"),
                countFacultySubmissions(facultyId, adminMode,
                        "s.status = '" + STATUS_LATE + "'"),
                countFacultySubmissions(facultyId, adminMode,
                        sqliteDateTimeExpression("COALESCE(s.submitted_at, s.submission_date)") + " >= datetime('now', '-7 days')")
        );
    }

    public String getStudentAssignmentStatus(String studentId, Assignment assignment) {
        Submission submission = getStudentSubmission(studentId, assignment.getId());
        if (submission == null) {
            return isDeadlinePassed(assignment) ? STATUS_OVERDUE : STATUS_PENDING;
        }
        if (submission.isGraded()) {
            return STATUS_GRADED;
        }
        return isBlank(submission.getStatus()) ? STATUS_SUBMITTED : submission.getStatus();
    }

    public String getDeadlineCountdown(Assignment assignment) {
        if (assignment == null || assignment.getDeadline() == null) {
            return "No deadline";
        }

        Duration remaining = Duration.between(new Date().toInstant(), assignment.getDeadline().toInstant());
        if (remaining.isNegative() || remaining.isZero()) {
            return STATUS_OVERDUE;
        }

        long days = remaining.toDays();
        long hours = remaining.minusDays(days).toHours();
        long minutes = remaining.minusDays(days).minusHours(hours).toMinutes();
        if (days > 0) {
            return "Due in " + days + (days == 1 ? " day" : " days");
        }
        if (hours > 0) {
            return "Due in " + hours + (hours == 1 ? " hour" : " hours");
        }
        return "Due in " + Math.max(1, minutes) + " minutes";
    }

    public boolean isDeadlinePassed(Assignment assignment) {
        return assignment != null
                && assignment.getDeadline() != null
                && new Date().after(assignment.getDeadline());
    }

    private void validateAssignment(Assignment assignment) {
        if (assignment == null) {
            throw new IllegalArgumentException("Assignment is required.");
        }
        if (isBlank(assignment.getId())) {
            throw new IllegalArgumentException("Assignment ID is required.");
        }
        if (isBlank(assignment.getCourseId())) {
            throw new IllegalArgumentException("Course is required.");
        }
        if (isBlank(assignment.getTitle())) {
            throw new IllegalArgumentException("Assignment title is required.");
        }
        if (assignment.getDeadline() == null) {
            throw new IllegalArgumentException("Assignment deadline is required.");
        }
        if (assignment.getMaxMarks() <= 0) {
            throw new IllegalArgumentException("Max marks must be greater than zero.");
        }
    }

    private void validateSubmissionAllowed(String studentId, String assignmentId) {
        if (isBlank(studentId)) {
            throw new IllegalArgumentException("Student is required.");
        }
        if (isBlank(assignmentId)) {
            throw new IllegalArgumentException("Assignment is required.");
        }

        String sql = "SELECT 1 FROM assignments a " +
                "JOIN enrollments e ON e.course_id = a.course_id " +
                "WHERE (a.id = ? OR a.assignment_id = ?) AND e.student_id = ? AND COALESCE(a.status, ?) = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, assignmentId);
            pstmt.setString(2, assignmentId);
            pstmt.setString(3, studentId);
            pstmt.setString(4, STATUS_PUBLISHED);
            pstmt.setString(5, STATUS_PUBLISHED);
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                throw new IllegalStateException("Student is not enrolled in the assignment course.");
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error validating assignment submission: " + e.getMessage(), e);
        }
    }

    private void validateSubmissionContent(String submissionText, List<String> attachmentPaths) {
        if (isBlank(submissionText) && attachmentPaths.isEmpty()) {
            throw new IllegalArgumentException("Submission text or at least one attachment is required.");
        }
        for (String filePath : attachmentPaths) {
            Path path = Path.of(filePath).normalize();
            if (!Files.isRegularFile(path)) {
                throw new IllegalArgumentException("Submission file was not saved correctly: " + path);
            }
            String extension = fileExtension(path.getFileName().toString());
            if (!"pdf".equals(extension) && !"docx".equals(extension) && !"zip".equals(extension)) {
                throw new IllegalArgumentException("Unsupported submission file format: " + extension);
            }
        }
    }

    private void refreshLateStatusForAssignment(String assignmentId) {
        if (isBlank(assignmentId)) {
            return;
        }
        String sql = "UPDATE submissions SET status = ? " +
                "WHERE assignment_id = ? " +
                "AND is_graded = 0 " +
                "AND (file_path IS NOT NULL OR submission_text IS NOT NULL) " +
                "AND " + sqliteDateTimeExpression("COALESCE(submitted_at, submission_date)") + " > " +
                "(SELECT " + sqliteDateTimeExpression("deadline") + " FROM assignments WHERE id = ? OR assignment_id = ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, STATUS_LATE);
            pstmt.setString(2, assignmentId);
            pstmt.setString(3, assignmentId);
            pstmt.setString(4, assignmentId);
            pstmt.executeUpdate();
            refreshLateAssignmentSubmissionRecords(conn, assignmentId);
        } catch (SQLException e) {
            throw new IllegalStateException("Error refreshing late submission status: " + e.getMessage(), e);
        }
    }

    private void upsertAssignmentSubmissionRecord(Connection conn, String submissionId, String assignmentId,
                                                  String studentId, String submissionText, String filePath,
                                                  String submittedAt, String status) throws SQLException {
        String sql = "INSERT INTO assignment_submissions " +
                "(submission_id, assignment_id, student_id, submission_text, file_path, submitted_at, status, marks, feedback, is_graded) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, NULL, NULL, 0) " +
                "ON CONFLICT(assignment_id, student_id) DO UPDATE SET " +
                "submission_id = excluded.submission_id, " +
                "submission_text = excluded.submission_text, " +
                "file_path = excluded.file_path, " +
                "submitted_at = excluded.submitted_at, " +
                "status = excluded.status, " +
                "marks = NULL, " +
                "feedback = NULL, " +
                "is_graded = 0";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, submissionId);
            pstmt.setString(2, assignmentId);
            pstmt.setString(3, studentId);
            pstmt.setString(4, submissionText);
            pstmt.setString(5, filePath);
            pstmt.setString(6, submittedAt);
            pstmt.setString(7, status);
            pstmt.executeUpdate();
        }
    }

    private void replaceSubmissionFiles(Connection conn, String submissionId, List<String> attachmentPaths) throws SQLException {
        try (PreparedStatement delete = conn.prepareStatement("DELETE FROM assignment_submission_files WHERE submission_id = ?")) {
            delete.setString(1, submissionId);
            delete.executeUpdate();
        }
        String sql = "INSERT INTO assignment_submission_files (id, submission_id, file_path) VALUES (?, ?, ?)";
        try (PreparedStatement insert = conn.prepareStatement(sql)) {
            for (String filePath : attachmentPaths) {
                insert.setString(1, "ASF-" + UUID.randomUUID());
                insert.setString(2, submissionId);
                insert.setString(3, filePath);
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private void updateAssignmentSubmissionGrade(Connection conn, String submissionId, double marks, String feedback) throws SQLException {
        String sql = "UPDATE assignment_submissions SET marks = ?, feedback = ?, is_graded = 1, status = ? " +
                "WHERE submission_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, marks);
            pstmt.setString(2, feedback);
            pstmt.setString(3, STATUS_GRADED);
            pstmt.setString(4, submissionId);
            pstmt.executeUpdate();
        }
    }

    private void refreshLateAssignmentSubmissionRecords(Connection conn, String assignmentId) throws SQLException {
        String sql = "UPDATE assignment_submissions SET status = ? " +
                "WHERE assignment_id = ? " +
                "AND is_graded = 0 " +
                "AND (file_path IS NOT NULL OR submission_text IS NOT NULL) " +
                "AND " + sqliteDateTimeExpression("submitted_at") + " > " +
                "(SELECT " + sqliteDateTimeExpression("deadline") + " FROM assignments WHERE id = ? OR assignment_id = ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, STATUS_LATE);
            pstmt.setString(2, assignmentId);
            pstmt.setString(3, assignmentId);
            pstmt.setString(4, assignmentId);
            pstmt.executeUpdate();
        }
    }

    private boolean courseExists(Connection conn, String courseId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT 1 FROM courses WHERE course_id = ?")) {
            pstmt.setString(1, courseId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }

    private void createNotificationsForAssignment(Connection conn, String assignmentId, String courseId,
                                                  String title, Date deadline) throws SQLException {
        String sql = "SELECT student_id FROM enrollments WHERE course_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                insertAssignmentNotification(conn, rs.getString("student_id"), assignmentId, courseId, title, deadline);
            }
        }
    }

    private void insertAssignmentNotification(Connection conn, String studentId, String assignmentId,
                                              String courseId, String title, Date deadline) throws SQLException {
        String courseName = courseName(conn, courseId);
        String deadlineText = deadline == null
                ? "No deadline"
                : DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                        .format(LocalDateTime.ofInstant(deadline.toInstant(), ZoneId.systemDefault()));
        String message = "New Assignment Assigned: " + title + " | Course: " + courseName + " | Deadline: " + deadlineText;
        String sql = "INSERT OR IGNORE INTO assignment_notifications " +
                "(id, student_id, assignment_id, course_id, title, message) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "N-" + UUID.randomUUID());
            pstmt.setString(2, studentId);
            pstmt.setString(3, assignmentId);
            pstmt.setString(4, courseId);
            pstmt.setString(5, title);
            pstmt.setString(6, message);
            pstmt.executeUpdate();
        }
    }

    private void markAssignmentNotificationRead(Connection conn, String studentId, String assignmentId) throws SQLException {
        String sql = "UPDATE assignment_notifications SET read_at = CURRENT_TIMESTAMP " +
                "WHERE student_id = ? AND assignment_id = ? AND read_at IS NULL";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, assignmentId);
            pstmt.executeUpdate();
        }
    }

    private String courseName(Connection conn, String courseId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT course_name FROM courses WHERE course_id = ?")) {
            pstmt.setString(1, courseId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getString("course_name") : courseId;
        }
    }

    private int countFacultyAssignments(String facultyId, boolean adminMode) {
        String sql = "SELECT COUNT(*) FROM assignments a JOIN courses c ON c.course_id = a.course_id " +
                (adminMode ? "" : "WHERE c.faculty_id = ?");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (!adminMode) {
                pstmt.setString(1, facultyId);
            }
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Error counting assignments: " + e.getMessage(), e);
        }
    }

    private int countFacultySubmissions(String facultyId, boolean adminMode, String condition) {
        String sql = "SELECT COUNT(*) FROM submissions s " +
                "JOIN assignments a ON a.id = s.assignment_id " +
                "JOIN courses c ON c.course_id = a.course_id " +
                "WHERE " + condition + (adminMode ? "" : " AND c.faculty_id = ?");
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (!adminMode) {
                pstmt.setString(1, facultyId);
            }
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("Error counting submissions: " + e.getMessage(), e);
        }
    }

    private String determineSubmissionStatus(Assignment assignment, Date submittedAt) {
        if (assignment.getDeadline() != null && submittedAt.after(assignment.getDeadline())) {
            return STATUS_LATE;
        }
        return STATUS_SUBMITTED;
    }

    private String resolveCreator(Assignment assignment) {
        if (!isBlank(assignment.getCreatedBy())) {
            return assignment.getCreatedBy().trim();
        }
        User user = SessionManager.getCurrentUser();
        return user == null ? "system" : user.getUsername();
    }

    private String normalizeDueTime(String dueTime, Date deadline) {
        if (!isBlank(dueTime)) {
            LocalTime.parse(dueTime.trim(), DUE_TIME_FORMATTER);
            return dueTime.trim();
        }
        LocalDateTime dateTime = LocalDateTime.ofInstant(deadline.toInstant(), ZoneId.systemDefault());
        return dateTime.toLocalTime().format(DUE_TIME_FORMATTER);
    }

    private String formatDueDate(Date deadline) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(deadline.toInstant(), ZoneId.systemDefault());
        return dateTime.toLocalDate().toString();
    }

    private String formatSqlDateTime(Date date) {
        return SQL_DATE_TIME_FORMATTER.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
    }

    private String resolveSubmissionId(Connection conn, Submission submission) throws SQLException {
        String existingId = findExistingSubmissionId(conn, submission.getAssignmentId(), submission.getStudentId());
        if (!isBlank(existingId)) {
            return existingId;
        }
        return isBlank(submission.getId()) ? "SUB-" + UUID.randomUUID() : submission.getId().trim();
    }

    private String findExistingSubmissionId(Connection conn, String assignmentId, String studentId) throws SQLException {
        String sql = "SELECT id FROM submissions WHERE assignment_id = ? AND student_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, assignmentId);
            pstmt.setString(2, studentId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() ? rs.getString("id") : null;
        }
    }

    private String normalizeSubmissionText(String submissionText) {
        return isBlank(submissionText) ? null : submissionText.trim();
    }

    private List<String> normalizeAttachmentPaths(Submission submission) {
        List<String> paths = submission.getAttachmentPaths();
        if (paths.isEmpty() && !isBlank(submission.getFilePath())) {
            paths.add(submission.getFilePath().trim());
        }
        List<String> normalized = new ArrayList<>();
        for (String path : paths) {
            if (!isBlank(path)) {
                normalized.add(path.trim());
            }
        }
        return normalized;
    }

    private String encodeFilePaths(List<String> attachmentPaths) {
        return attachmentPaths == null || attachmentPaths.isEmpty()
                ? null
                : String.join(Submission.FILE_PATH_SEPARATOR, attachmentPaths);
    }

    private String fileExtension(String fileName) {
        int dot = fileName == null ? -1 : fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase();
    }

    private Assignment mapResultSetToAssignment(ResultSet rs) throws SQLException {
        String id = getOptionalString(rs, "id");
        if (isBlank(id)) {
            id = getOptionalString(rs, "assignment_id");
        }

        Date deadline = getOptionalTimestampAsDate(rs, "deadline");
        String dueTime = getOptionalString(rs, "due_time");
        LocalDate dueDate = getOptionalLocalDate(rs, "due_date");
        if (deadline == null && dueDate != null) {
            deadline = combineDueDateAndTime(dueDate, dueTime);
        }

        Assignment assignment = new Assignment(
                id,
                getOptionalString(rs, "course_id"),
                getOptionalString(rs, "title"),
                getOptionalString(rs, "description"),
                deadline,
                getOptionalDouble(rs, "max_marks")
        );
        assignment.setCreatedBy(getOptionalString(rs, "created_by"));
        assignment.setDueTime(isBlank(dueTime) && deadline != null ? normalizeDueTime(null, deadline) : dueTime);
        assignment.setCreatedAt(getOptionalTimestampAsDate(rs, "created_at"));
        assignment.setStatus(defaultText(getOptionalString(rs, "status"), STATUS_PUBLISHED));
        assignment.setAttachmentPath(getOptionalString(rs, "attachment_path"));
        assignment.setAssignmentType(defaultText(getOptionalString(rs, "type"), "Homework"));
        assignment.setAdditionalMaterialPath(getOptionalString(rs, "materials"));
        return assignment;
    }

    private Submission mapResultSetToSubmission(ResultSet rs) throws SQLException {
        Submission submission = new Submission(
                getOptionalString(rs, "id"),
                getOptionalString(rs, "assignment_id"),
                getOptionalString(rs, "student_id"),
                getOptionalString(rs, "file_path")
        );
        submission.setSubmissionDate(firstNonNull(
                getOptionalTimestampAsDate(rs, "submitted_at"),
                getOptionalTimestampAsDate(rs, "submission_date")
        ));
        submission.setSubmissionText(getOptionalString(rs, "submission_text"));
        submission.setMarks(getOptionalDouble(rs, "marks"));
        submission.setFeedback(getOptionalString(rs, "feedback"));
        submission.setGraded(getOptionalBoolean(rs, "is_graded"));
        submission.setStatus(defaultText(getOptionalString(rs, "status"), submission.isGraded() ? STATUS_GRADED : STATUS_SUBMITTED));
        return submission;
    }

    private AssignmentNotification mapResultSetToNotification(ResultSet rs) throws SQLException {
        return new AssignmentNotification(
                getOptionalString(rs, "id"),
                getOptionalString(rs, "student_id"),
                getOptionalString(rs, "assignment_id"),
                getOptionalString(rs, "course_id"),
                getOptionalString(rs, "title"),
                getOptionalString(rs, "message"),
                getOptionalTimestampAsDate(rs, "created_at"),
                getOptionalTimestampAsDate(rs, "read_at")
        );
    }

    private Date combineDueDateAndTime(LocalDate localDate, String dueTime) {
        LocalTime localTime = isBlank(dueTime) ? LocalTime.of(23, 59) : LocalTime.parse(dueTime, DUE_TIME_FORMATTER);
        return Date.from(LocalDateTime.of(localDate, localTime).atZone(ZoneId.systemDefault()).toInstant());
    }

    private String getOptionalString(ResultSet rs, String column) throws SQLException {
        if (!hasColumn(rs, column)) {
            return null;
        }
        return rs.getString(column);
    }

    private double getOptionalDouble(ResultSet rs, String column) throws SQLException {
        if (!hasColumn(rs, column)) {
            return 0.0;
        }
        double value = rs.getDouble(column);
        return rs.wasNull() ? 0.0 : value;
    }

    private boolean getOptionalBoolean(ResultSet rs, String column) throws SQLException {
        if (!hasColumn(rs, column)) {
            return false;
        }
        boolean value = rs.getBoolean(column);
        return !rs.wasNull() && value;
    }

    private Date getOptionalTimestampAsDate(ResultSet rs, String column) throws SQLException {
        if (!hasColumn(rs, column)) {
            return null;
        }
        Object raw = rs.getObject(column);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return new Date(number.longValue());
        }

        String value = raw.toString();
        if (isBlank(value)) {
            return null;
        }

        try {
            return new Date(Long.parseLong(value.trim()));
        } catch (NumberFormatException ignored) {
            // Not a legacy epoch-millis value.
        }

        try {
            return Date.from(Timestamp.valueOf(value.trim()).toInstant());
        } catch (IllegalArgumentException ignored) {
            // Not a JDBC timestamp string.
        }

        try {
            LocalDate localDate = LocalDate.parse(value.length() >= 10 ? value.substring(0, 10) : value);
            return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        } catch (DateTimeParseException parseError) {
            throw new SQLException("Error parsing timestamp: " + value, parseError);
        }
    }

    private LocalDate getOptionalLocalDate(ResultSet rs, String column) throws SQLException {
        if (!hasColumn(rs, column)) {
            return null;
        }
        String value = rs.getString(column);
        if (isBlank(value)) {
            return null;
        }
        try {
            return LocalDate.parse(value.length() >= 10 ? value.substring(0, 10) : value);
        } catch (Exception parseError) {
            try {
                long millis = Long.parseLong(value);
                return LocalDateTime.ofInstant(new Date(millis).toInstant(), ZoneId.systemDefault()).toLocalDate();
            } catch (NumberFormatException ignored) {
                throw new SQLException("Error parsing due date: " + value, parseError);
            }
        }
    }

    private boolean hasColumn(ResultSet rs, String column) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            if (column.equalsIgnoreCase(metaData.getColumnName(i))) {
                return true;
            }
        }
        return false;
    }

    private Date firstNonNull(Date first, Date second) {
        return first != null ? first : second;
    }

    private String defaultText(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private String sqliteDateTimeExpression(String expression) {
        String textExpression = "TRIM(CAST(" + expression + " AS TEXT))";
        return "CASE " +
                "WHEN " + expression + " IS NULL THEN NULL " +
                "WHEN typeof(" + expression + ") IN ('integer','real') THEN datetime(" + expression + " / 1000, 'unixepoch') " +
                "WHEN " + textExpression + " <> '' AND " + textExpression + " NOT GLOB '*[^0-9]*' THEN datetime(CAST(" + expression + " AS INTEGER) / 1000, 'unixepoch') " +
                "ELSE datetime(" + expression + ") END";
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
