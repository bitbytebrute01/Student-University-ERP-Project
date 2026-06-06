package com.university.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DEFAULT_DB_URL = "jdbc:sqlite:university_erp.db";

    private DatabaseManager() {}

    public static Connection getConnection() throws SQLException {
        Connection connection = DriverManager.getConnection(getDatabaseUrl());
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA journal_mode = WAL;");
        }
        return connection;
    }

    private static String getDatabaseUrl() {
        return System.getProperty("university.db.url", DEFAULT_DB_URL);
    }

    public static void initializeSchema() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // USERS & ROLES
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY, " +
                    "password_hash TEXT NOT NULL, " +
                    "role TEXT NOT NULL, " +
                    "ref_id TEXT)");
            addColumnIfMissing(conn, "users", "ref_id", "TEXT");

            // STUDENTS
            stmt.execute("CREATE TABLE IF NOT EXISTS students (" +
                    "id TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "email TEXT UNIQUE, " +
                    "phone TEXT, " +
                    "department TEXT, " +
                    "semester INTEGER, " +
                    "cgpa REAL DEFAULT 0.0, " +
                    "attendance REAL DEFAULT 0.0, " +
                    "fee_status TEXT DEFAULT 'Pending', " +
                    "skills TEXT, " + // Stored as JSON
                    "projects TEXT, " + // Stored as JSON
                    "certifications TEXT, " + // Stored as JSON
                    "bio TEXT, " +
                    "resume_path TEXT, " +
                    "profile_pic TEXT, " +
                    "cover_pic TEXT, " +
                    "linkedin_url TEXT, " +
                    "github_url TEXT, " +
                    "portfolio_url TEXT, " +
                    "languages TEXT, " + // JSON
                    "interests TEXT, " + // JSON
                    "research_papers TEXT, " + // JSON
                    "experience TEXT)"); // JSON

            // FACULTY
            stmt.execute("CREATE TABLE IF NOT EXISTS faculty (" +
                    "id TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "email TEXT UNIQUE, " +
                    "phone TEXT, " +
                    "department TEXT, " +
                    "designation TEXT, " +
                    "experience INTEGER)");

            // COURSES
            stmt.execute("CREATE TABLE IF NOT EXISTS courses (" +
                    "course_id TEXT PRIMARY KEY, " +
                    "course_name TEXT NOT NULL, " +
                    "credits INTEGER, " +
                    "faculty_id TEXT, " +
                    "FOREIGN KEY(faculty_id) REFERENCES faculty(id))");

            // ENROLLMENTS
            stmt.execute("CREATE TABLE IF NOT EXISTS enrollments (" +
                    "course_id TEXT, " +
                    "student_id TEXT, " +
                    "enrollment_date DATE DEFAULT CURRENT_DATE, " +
                    "PRIMARY KEY(course_id, student_id), " +
                    "FOREIGN KEY(course_id) REFERENCES courses(course_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE)");

            // ATTENDANCE
            stmt.execute("CREATE TABLE IF NOT EXISTS attendance (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "student_id TEXT, " +
                    "course_id TEXT, " +
                    "date DATE DEFAULT CURRENT_DATE, " +
                    "is_present BOOLEAN, " +
                    "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY(course_id) REFERENCES courses(course_id) ON DELETE CASCADE)");
            addColumnIfMissing(conn, "attendance", "updated_at", "DATETIME");

            // PERSISTENT ATTENDANCE CODES (legacy) and ATTENDANCE SESSIONS/AUDIT
            stmt.execute("CREATE TABLE IF NOT EXISTS attendance_codes (" +
                    "id TEXT PRIMARY KEY, " +
                    "course_id TEXT NOT NULL, " +
                    "code TEXT NOT NULL, " +
                    "created_by TEXT, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "expires_at DATETIME, " +
                    "active INTEGER DEFAULT 1, " +
                    "FOREIGN KEY(course_id) REFERENCES courses(course_id) ON DELETE CASCADE)");

            // New attendance session table implementing secure one-time code sessions
            stmt.execute("CREATE TABLE IF NOT EXISTS attendance_sessions (" +
                    "session_id TEXT PRIMARY KEY, " +
                    "course_id TEXT NOT NULL, " +
                    "faculty_id TEXT NOT NULL, " +
                    "attendance_code TEXT NOT NULL, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "expires_at DATETIME, " +
                    "status TEXT DEFAULT 'ACTIVE', " +
                    "FOREIGN KEY(course_id) REFERENCES courses(course_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY(faculty_id) REFERENCES faculty(id) ON DELETE CASCADE)");

            // Audit log for attendance marks (one row per student per session)
            stmt.execute("CREATE TABLE IF NOT EXISTS attendance_audit (" +
                    "audit_id TEXT PRIMARY KEY, " +
                    "session_id TEXT NOT NULL, " +
                    "student_id TEXT NOT NULL, " +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "ip_or_device_identifier TEXT, " +
                    "status TEXT, " +
                    "FOREIGN KEY(session_id) REFERENCES attendance_sessions(session_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE)");

            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_attendance_audit_session_student " +
                    "ON attendance_audit(session_id, student_id)");

            // LMS: ASSIGNMENTS
            stmt.execute("CREATE TABLE IF NOT EXISTS assignments (" +
                    "id TEXT PRIMARY KEY, " +
                    "assignment_id TEXT, " +
                    "course_id TEXT, " +
                    "created_by TEXT, " +
                    "title TEXT NOT NULL, " +
                    "description TEXT, " +
                    "deadline DATE, " +
                    "due_date DATE, " +
                    "due_time TEXT, " +
                    "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "status TEXT DEFAULT 'Published', " +
                    "max_marks REAL, " +
                    "attachment_path TEXT, " +
                    "type TEXT DEFAULT 'Homework', " +
                    "materials TEXT, " +
                    "FOREIGN KEY(course_id) REFERENCES courses(course_id) ON DELETE CASCADE)");

            // LMS: SUBMISSIONS
            stmt.execute("CREATE TABLE IF NOT EXISTS submissions (" +
                    "id TEXT PRIMARY KEY, " +
                    "submission_id TEXT, " +
                    "assignment_id TEXT, " +
                    "student_id TEXT, " +
                    "submission_text TEXT, " +
                    "file_path TEXT, " +
                    "submission_date DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "status TEXT DEFAULT 'Submitted', " +
                    "marks REAL, " +
                    "feedback TEXT, " +
                    "is_graded BOOLEAN DEFAULT 0, " +
                    "FOREIGN KEY(assignment_id) REFERENCES assignments(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE)");

            // LIBRARY
            stmt.execute("CREATE TABLE IF NOT EXISTS books (" +
                    "book_id TEXT PRIMARY KEY, " +
                    "title TEXT NOT NULL, " +
                    "author TEXT, " +
                    "is_available BOOLEAN DEFAULT 1)");

            stmt.execute("CREATE TABLE IF NOT EXISTS book_issues (" +
                    "issue_id TEXT PRIMARY KEY, " +
                    "book_id TEXT, " +
                    "student_id TEXT, " +
                    "issue_date DATE DEFAULT CURRENT_DATE, " +
                    "return_date DATE, " +
                    "FOREIGN KEY(book_id) REFERENCES books(book_id) ON DELETE CASCADE, " +
                    "FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE)");

            // HOSTEL
            stmt.execute("CREATE TABLE IF NOT EXISTS rooms (" +
                    "room_key TEXT PRIMARY KEY, " + // hostel-number
                    "room_number TEXT, " +
                    "hostel_name TEXT, " +
                    "capacity INTEGER, " +
                    "occupied INTEGER DEFAULT 0)");

            // PLACEMENTS
            stmt.execute("CREATE TABLE IF NOT EXISTS companies (" +
                    "company_id TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "required_cgpa REAL, " +
                    "job_role TEXT)");

            // EXAMS
            stmt.execute("CREATE TABLE IF NOT EXISTS marks (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "student_id TEXT, " +
                    "course_id TEXT, " +
                    "marks REAL, " +
                    "FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE, " +
                    "FOREIGN KEY(course_id) REFERENCES courses(course_id) ON DELETE CASCADE)");

            // AUDIT LOGS
            stmt.execute("CREATE TABLE IF NOT EXISTS audit_logs (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT, " +
                    "action TEXT, " +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

            addColumnIfMissing(conn, "students", "certifications", "TEXT");
            addColumnIfMissing(conn, "students", "resume_path", "TEXT");
            addColumnIfMissing(conn, "students", "profile_pic", "TEXT");
            addColumnIfMissing(conn, "students", "cover_pic", "TEXT");
            addColumnIfMissing(conn, "students", "linkedin_url", "TEXT");
            addColumnIfMissing(conn, "students", "github_url", "TEXT");
            addColumnIfMissing(conn, "students", "portfolio_url", "TEXT");
            addColumnIfMissing(conn, "students", "languages", "TEXT");
            addColumnIfMissing(conn, "students", "interests", "TEXT");
            addColumnIfMissing(conn, "students", "research_papers", "TEXT");
            addColumnIfMissing(conn, "students", "experience", "TEXT");
            addColumnIfMissing(conn, "students", "xp_points", "INTEGER DEFAULT 0");
            addColumnIfMissing(conn, "students", "level", "INTEGER DEFAULT 1");
            addColumnIfMissing(conn, "assignments", "assignment_id", "TEXT");
            addColumnIfMissing(conn, "assignments", "created_by", "TEXT");
            addColumnIfMissing(conn, "assignments", "due_date", "DATE");
            addColumnIfMissing(conn, "assignments", "due_time", "TEXT");
            addColumnIfMissing(conn, "assignments", "created_at", "DATETIME");
            addColumnIfMissing(conn, "assignments", "status", "TEXT DEFAULT 'Published'");
            addColumnIfMissing(conn, "submissions", "submission_id", "TEXT");
            addColumnIfMissing(conn, "submissions", "submission_text", "TEXT");
            addColumnIfMissing(conn, "submissions", "submitted_at", "DATETIME");
            addColumnIfMissing(conn, "submissions", "status", "TEXT DEFAULT 'Submitted'");

            migrateAssignmentMetadata(stmt);
            
            // FORUMS
            stmt.execute("CREATE TABLE IF NOT EXISTS forum_posts (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "course_id TEXT, " +
                    "user_id TEXT, " +
                    "message TEXT, " +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

            // PLACEMENTS: JOBS
            stmt.execute("CREATE TABLE IF NOT EXISTS job_postings (" +
                    "id TEXT PRIMARY KEY, " +
                    "company_name TEXT, " +
                    "role TEXT, " +
                    "min_cgpa REAL, " +
                    "deadline DATE)");

            // STUDENT PROJECTS
            stmt.execute("CREATE TABLE IF NOT EXISTS student_projects (" +
                    "id TEXT PRIMARY KEY, " +
                    "student_id TEXT, " +
                    "title TEXT NOT NULL, " +
                    "description TEXT, " +
                    "screenshot_path TEXT, " +
                    "report_path TEXT, " +
                    "zip_path TEXT, " +
                    "status TEXT DEFAULT 'Pending', " +
                    "faculty_feedback TEXT, " +
                    "FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE)");

            // STUDENT CERTIFICATIONS
            stmt.execute("CREATE TABLE IF NOT EXISTS student_certifications (" +
                    "id TEXT PRIMARY KEY, " +
                    "student_id TEXT, " +
                    "title TEXT NOT NULL, " +
                    "issuer TEXT, " +
                    "completion_date DATE, " +
                    "file_path TEXT, " +
                    "FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE)");

            // USER PREFERENCES
            stmt.execute("CREATE TABLE IF NOT EXISTS user_preferences (" +
                    "username TEXT PRIMARY KEY, " +
                    "theme TEXT DEFAULT 'LIGHT', " +
                    "last_login DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "dashboard_layout TEXT, " +
                    "FOREIGN KEY(username) REFERENCES users(username) ON DELETE CASCADE)");

            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_submissions_assignment_student " +
                    "ON submissions(assignment_id, student_id)");
            stmt.execute("CREATE UNIQUE INDEX IF NOT EXISTS idx_assignments_assignment_id " +
                    "ON assignments(assignment_id)");
            ensureAssignmentSubmissionsTable(conn, stmt);
            ensureAssignmentNotificationTables(stmt);
            stmt.execute("CREATE VIEW IF NOT EXISTS student_profiles AS " +
                    "SELECT id AS student_id, name, email, phone, department, semester, bio, resume_path, " +
                    "profile_pic, cover_pic, linkedin_url, github_url, portfolio_url FROM students");

            System.out.println("Relational Database Schema Initialized successfully.");

        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }

    private static void migrateAssignmentMetadata(Statement stmt) throws SQLException {
        String deadlineDateTime = sqliteDateTimeExpression("deadline");
        String createdAtDateTime = sqliteDateTimeExpression("created_at");
        String submissionDateTime = sqliteDateTimeExpression("submission_date");
        String submittedAtDateTime = sqliteDateTimeExpression("submitted_at");

        stmt.executeUpdate("UPDATE assignments SET assignment_id = id WHERE assignment_id IS NULL OR assignment_id = ''");
        stmt.executeUpdate("UPDATE assignments SET deadline = " + deadlineDateTime + " " +
                "WHERE deadline IS NOT NULL AND " + deadlineDateTime + " IS NOT NULL");
        stmt.executeUpdate("UPDATE assignments SET due_date = date(" + deadlineDateTime + ") WHERE deadline IS NOT NULL AND due_date IS NULL");
        stmt.executeUpdate("UPDATE assignments SET due_time = COALESCE(NULLIF(strftime('%H:%M', " + deadlineDateTime + "), '00:00'), '23:59') " +
                "WHERE due_time IS NULL OR due_time = ''");
        stmt.executeUpdate("UPDATE assignments SET created_at = CURRENT_TIMESTAMP WHERE created_at IS NULL");
        stmt.executeUpdate("UPDATE assignments SET created_at = " + createdAtDateTime + " " +
                "WHERE created_at IS NOT NULL AND " + createdAtDateTime + " IS NOT NULL");
        stmt.executeUpdate("UPDATE assignments SET status = 'Published' WHERE status IS NULL OR status = ''");

        stmt.executeUpdate("UPDATE submissions SET submission_id = id WHERE submission_id IS NULL OR submission_id = ''");
        stmt.executeUpdate("UPDATE submissions SET submitted_at = submission_date WHERE submitted_at IS NULL AND submission_date IS NOT NULL");
        stmt.executeUpdate("UPDATE submissions SET submitted_at = CURRENT_TIMESTAMP WHERE submitted_at IS NULL");
        stmt.executeUpdate("UPDATE submissions SET submission_date = " + submissionDateTime + " " +
                "WHERE submission_date IS NOT NULL AND " + submissionDateTime + " IS NOT NULL");
        stmt.executeUpdate("UPDATE submissions SET submitted_at = " + submittedAtDateTime + " " +
                "WHERE submitted_at IS NOT NULL AND " + submittedAtDateTime + " IS NOT NULL");
        stmt.executeUpdate("UPDATE submissions SET status = CASE WHEN is_graded = 1 THEN 'Graded' ELSE 'Submitted' END " +
                "WHERE status IS NULL OR status = ''");
    }

    private static void ensureAssignmentSubmissionsTable(Connection conn, Statement stmt) throws SQLException {
        String objectType = null;
        try (ResultSet rs = stmt.executeQuery("SELECT type FROM sqlite_master WHERE name = 'assignment_submissions'")) {
            if (rs.next()) {
                objectType = rs.getString("type");
            }
        }

        if ("view".equalsIgnoreCase(objectType)) {
            stmt.execute("DROP VIEW assignment_submissions");
        }

        stmt.execute("CREATE TABLE IF NOT EXISTS assignment_submissions (" +
                "submission_id TEXT PRIMARY KEY, " +
                "assignment_id TEXT NOT NULL, " +
                "student_id TEXT NOT NULL, " +
                "submission_text TEXT, " +
                "file_path TEXT, " +
                "submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "status TEXT DEFAULT 'Submitted', " +
                "marks REAL, " +
                "feedback TEXT, " +
                "is_graded BOOLEAN DEFAULT 0, " +
                "UNIQUE(assignment_id, student_id), " +
                "FOREIGN KEY(assignment_id) REFERENCES assignments(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE)");
        addColumnIfMissing(conn, "assignment_submissions", "submission_text", "TEXT");
            // Track grading timestamp for cross-process polling
            addColumnIfMissing(conn, "assignment_submissions", "graded_at", "DATETIME");
            // Track profile updates on students table for cross-process profile photo refresh
            addColumnIfMissing(conn, "students", "profile_updated_at", "DATETIME");

        stmt.executeUpdate("INSERT OR REPLACE INTO assignment_submissions " +
                "(submission_id, assignment_id, student_id, submission_text, file_path, submitted_at, status, marks, feedback, is_graded) " +
                "SELECT COALESCE(submission_id, id), assignment_id, student_id, submission_text, file_path, " +
                "COALESCE(submitted_at, submission_date), " +
                "COALESCE(status, CASE WHEN is_graded = 1 THEN 'Graded' ELSE 'Submitted' END), " +
                "marks, feedback, is_graded " +
                "FROM submissions " +
                "WHERE assignment_id IS NOT NULL AND student_id IS NOT NULL AND COALESCE(submission_id, id) IS NOT NULL");

        String submittedAtDateTime = sqliteDateTimeExpression("submitted_at");
        stmt.executeUpdate("UPDATE assignment_submissions SET submitted_at = " + submittedAtDateTime + " " +
                "WHERE submitted_at IS NOT NULL AND " + submittedAtDateTime + " IS NOT NULL");
    }

    private static void ensureAssignmentNotificationTables(Statement stmt) throws SQLException {
        stmt.execute("CREATE TABLE IF NOT EXISTS assignment_submission_files (" +
                "id TEXT PRIMARY KEY, " +
                "submission_id TEXT NOT NULL, " +
                "file_path TEXT NOT NULL, " +
                "uploaded_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY(submission_id) REFERENCES submissions(id) ON DELETE CASCADE)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_assignment_submission_files_submission " +
                "ON assignment_submission_files(submission_id)");

        stmt.executeUpdate("INSERT OR IGNORE INTO assignment_submission_files (id, submission_id, file_path) " +
                "SELECT COALESCE(submission_id, id) || '-file-1', id, file_path " +
                "FROM submissions WHERE file_path IS NOT NULL AND file_path <> ''");

        stmt.execute("CREATE TABLE IF NOT EXISTS assignment_notifications (" +
                "id TEXT PRIMARY KEY, " +
                "student_id TEXT NOT NULL, " +
                "assignment_id TEXT NOT NULL, " +
                "course_id TEXT NOT NULL, " +
                "title TEXT NOT NULL, " +
                "message TEXT NOT NULL, " +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "read_at DATETIME, " +
                "UNIQUE(student_id, assignment_id), " +
                "FOREIGN KEY(student_id) REFERENCES students(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(assignment_id) REFERENCES assignments(id) ON DELETE CASCADE, " +
                "FOREIGN KEY(course_id) REFERENCES courses(course_id) ON DELETE CASCADE)");
        stmt.execute("CREATE INDEX IF NOT EXISTS idx_assignment_notifications_student_unread " +
                "ON assignment_notifications(student_id, read_at, created_at)");

        String createdAtDateTime = sqliteDateTimeExpression("created_at");
        stmt.executeUpdate("UPDATE assignment_notifications SET created_at = " + createdAtDateTime + " " +
                "WHERE created_at IS NOT NULL AND " + createdAtDateTime + " IS NOT NULL");
    }

    private static void addColumnIfMissing(Connection conn, String tableName, String columnName, String definition) throws SQLException {
        if (columnExists(conn, tableName, columnName)) {
            return;
        }

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")")) {
            while (rs.next()) {
                if (columnName.equalsIgnoreCase(rs.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static String sqliteDateTimeExpression(String expression) {
        String textExpression = "TRIM(CAST(" + expression + " AS TEXT))";
        return "CASE " +
                "WHEN " + expression + " IS NULL THEN NULL " +
                "WHEN typeof(" + expression + ") IN ('integer','real') THEN datetime(" + expression + " / 1000, 'unixepoch') " +
                "WHEN " + textExpression + " <> '' AND " + textExpression + " NOT GLOB '*[^0-9]*' THEN datetime(CAST(" + expression + " AS INTEGER) / 1000, 'unixepoch') " +
                "ELSE datetime(" + expression + ") END";
    }
}
