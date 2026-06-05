package com.university.courses;

import com.university.db.DatabaseManager;
import com.university.exceptions.CourseNotFoundException;
import com.university.interfaces.Searchable;
import com.university.lms.AssignmentManager;
import com.university.models.Course;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class CourseManager implements Searchable<Course> {

    public CourseManager() {}

    public void addCourse(Course course) {
        String sql = "INSERT INTO courses (course_id, course_name, credits, faculty_id) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(course_id) DO UPDATE SET " +
                "course_name = excluded.course_name, " +
                "credits = excluded.credits, " +
                "faculty_id = excluded.faculty_id";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, course.getCourseId());
            pstmt.setString(2, course.getCourseName());
            pstmt.setInt(3, course.getCredits());
            pstmt.setString(4, course.getAssignedFacultyId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Error adding course: " + e.getMessage(), e);
        }
    }

    public void assignFacultyToCourse(String courseId, String facultyId) throws CourseNotFoundException {
        String sql = "UPDATE courses SET faculty_id = ? WHERE course_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, facultyId);
            pstmt.setString(2, courseId);
            if (pstmt.executeUpdate() == 0) throw new CourseNotFoundException("Course not found: " + courseId);
        } catch (SQLException e) {
            System.err.println("Error assigning faculty: " + e.getMessage());
        }
    }

    public void enrollStudent(String courseId, String studentId) throws CourseNotFoundException {
        String sql = "INSERT OR IGNORE INTO enrollments (course_id, student_id) VALUES (?, ?)";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                if (!recordExists(conn, "courses", "course_id", courseId)) {
                    throw new CourseNotFoundException("Course not found: " + courseId);
                }
                if (!recordExists(conn, "students", "id", studentId)) {
                    throw new IllegalArgumentException("Student not found: " + studentId);
                }

                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    pstmt.setString(1, courseId);
                    pstmt.setString(2, studentId);
                    pstmt.executeUpdate();
                }
                new AssignmentManager().createNotificationsForStudentCourse(conn, studentId, courseId);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                if (e instanceof CourseNotFoundException courseNotFoundException) {
                    throw courseNotFoundException;
                }
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new IllegalStateException("Error enrolling student: " + e.getMessage(), e);
            }
        } catch (SQLException e) {
            throw new IllegalStateException("Error enrolling student: " + e.getMessage(), e);
        }
    }

    public void dropCourse(String courseId, String studentId) {
        String sql = "DELETE FROM enrollments WHERE course_id = ? AND student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseId);
            pstmt.setString(2, studentId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error dropping course: " + e.getMessage());
        }
    }

    @Override
    public Course searchById(String id) {
        String sql = "SELECT * FROM courses WHERE course_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return mapResultSetToCourse(rs);
        } catch (SQLException e) {
            System.err.println("Error searching course: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Course> searchByName(String name) {
        List<Course> list = new ArrayList<>();
        String sql = "SELECT * FROM courses WHERE course_name LIKE ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + name + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapResultSetToCourse(rs));
        } catch (SQLException e) {
            System.err.println("Error searching course by name: " + e.getMessage());
        }
        return list;
    }

    public TreeSet<Course> getCoursesAlphabetically() {
        TreeSet<Course> set = new TreeSet<>();
        String sql = "SELECT * FROM courses ORDER BY course_name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) set.add(mapResultSetToCourse(rs));
        } catch (SQLException e) {
            System.err.println("Error getting sorted courses: " + e.getMessage());
        }
        return set;
    }

    public List<Course> getCoursesByFaculty(String facultyId) {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT * FROM courses WHERE faculty_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, facultyId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) courses.add(mapResultSetToCourse(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return courses;
    }

    public List<Course> getAllCourses() {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT * FROM courses ORDER BY course_name ASC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) courses.add(mapResultSetToCourse(rs));
        } catch (SQLException e) {
            throw new IllegalStateException("Error getting courses: " + e.getMessage(), e);
        }
        return courses;
    }

    public List<Course> getEnrolledCourses(String studentId) {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT c.* FROM courses c JOIN enrollments e ON c.course_id = e.course_id WHERE e.student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) courses.add(mapResultSetToCourse(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return courses;
    }

    public void deleteCourse(String courseId) {
        String sql = "DELETE FROM courses WHERE course_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseId);
            pstmt.executeUpdate();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public List<String> getEnrolledStudents(String courseId) {
        List<String> students = new ArrayList<>();
        String sql = "SELECT student_id FROM enrollments WHERE course_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, courseId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) students.add(rs.getString("student_id"));
        } catch (SQLException e) { e.printStackTrace(); }
        return students;
    }

    private Course mapResultSetToCourse(ResultSet rs) throws SQLException {
        Course c = new Course(
                rs.getString("course_id"),
                rs.getString("course_name"),
                rs.getInt("credits")
        );
        c.setAssignedFacultyId(rs.getString("faculty_id"));
        return c;
    }

    private boolean recordExists(Connection conn, String table, String column, String value) throws SQLException {
        String sql = "SELECT 1 FROM " + table + " WHERE " + column + " = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, value);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }
}
