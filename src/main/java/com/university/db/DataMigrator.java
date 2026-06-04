package com.university.db;

import com.google.gson.Gson;
import com.university.erp.security.User;
import com.university.models.*;
import com.university.utils.FileHandler;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class DataMigrator {
    private static final Gson gson = new Gson();

    public static void migrateAll() {
        System.out.println("Starting Data Migration from .dat to SQLite...");
        
        migrateUsers();
        migrateStudents();
        migrateFaculty();
        migrateCourses();
        migrateBooks();
        migrateRooms();
        
        System.out.println("Data Migration Completed.");
    }

    private static void migrateUsers() {
        Object data = FileHandler.loadFromFile("users.dat");
        if (data instanceof HashMap) {
            Map<String, User> users = (Map<String, User>) data;
            Map<String, Student> students = loadLegacyStudents();
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT OR REPLACE INTO users (username, password_hash, role, ref_id) VALUES (?, ?, ?, ?)")) {
                
                for (User u : users.values()) {
                    pstmt.setString(1, u.getUsername());
                    String legacyPassword = u.getPassword();
                    String hash;
                    
                    if (legacyPassword != null && legacyPassword.startsWith("$2a$")) {
                        hash = legacyPassword;
                    } else {
                        String plain = (legacyPassword != null) ? legacyPassword : "pass";
                        hash = BCrypt.hashpw(plain, BCrypt.gensalt());
                    }
                    
                    pstmt.setString(2, hash);
                    pstmt.setString(3, u.getRole().getLabel());
                    pstmt.setString(4, inferStudentRefId(u, students));
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                System.out.println("Users migrated successfully.");
            } catch (SQLException e) {
                System.err.println("User migration error: " + e.getMessage());
            }
        }
    }

    private static Map<String, Student> loadLegacyStudents() {
        Object data = FileHandler.loadFromFile("students.dat");
        if (data instanceof HashMap) {
            return (Map<String, Student>) data;
        }
        return new HashMap<>();
    }

    private static String inferStudentRefId(User user, Map<String, Student> students) {
        if (!"Student".equals(user.getRole().getLabel())) {
            return null;
        }

        String username = normalizeLookup(user.getUsername());
        for (Student student : students.values()) {
            if (student.getId().equalsIgnoreCase(user.getUsername())
                    || normalizeLookup(student.getName()).equals(username)) {
                return student.getId();
            }
        }

        if ("alice".equals(username) && students.containsKey("S001")) {
            return "S001";
        }

        if (username.startsWith("student")) {
            String suffix = username.substring("student".length());
            String seededId = "S10" + suffix;
            if (students.containsKey(seededId)) {
                return seededId;
            }
        }

        return null;
    }

    private static String normalizeLookup(String value) {
        return value == null ? "" : value.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private static void migrateStudents() {
        Object data = FileHandler.loadFromFile("students.dat");
        if (data instanceof HashMap) {
            Map<String, Student> students = (Map<String, Student>) data;
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT OR REPLACE INTO students (id, name, email, phone, department, semester, cgpa, attendance, fee_status, skills, projects, bio, languages, interests, research_papers, experience, profile_pic, cover_pic) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                
                for (Student s : students.values()) {
                    pstmt.setString(1, s.getId());
                    pstmt.setString(2, s.getName());
                    pstmt.setString(3, s.getEmail());
                    pstmt.setString(4, s.getPhone());
                    pstmt.setString(5, s.getDepartment());
                    pstmt.setInt(6, s.getSemester());
                    pstmt.setDouble(7, s.getCgpa());
                    pstmt.setDouble(8, s.getAttendancePercentage());
                    pstmt.setString(9, s.getFeeStatus());
                    pstmt.setString(10, gson.toJson(s.getSkills()));
                    pstmt.setString(11, gson.toJson(s.getProjects()));
                    pstmt.setString(12, s.getBio());
                    pstmt.setString(13, gson.toJson(s.getLanguages()));
                    pstmt.setString(14, gson.toJson(s.getInterests()));
                    pstmt.setString(15, gson.toJson(s.getResearchPapers()));
                    pstmt.setString(16, gson.toJson(s.getWorkExperience()));
                    pstmt.setString(17, s.getProfilePicturePath());
                    pstmt.setString(18, s.getCoverPhotoPath());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                System.out.println("Students migrated successfully.");
            } catch (SQLException e) {
                System.err.println("Student migration error: " + e.getMessage());
            }
        }
    }

    private static void migrateFaculty() {
        Object data = FileHandler.loadFromFile("faculty.dat");
        if (data instanceof HashMap) {
            Map<String, Faculty> faculty = (Map<String, Faculty>) data;
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT OR REPLACE INTO faculty (id, name, email, phone, department, designation, experience) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                for (Faculty f : faculty.values()) {
                    pstmt.setString(1, f.getId());
                    pstmt.setString(2, f.getName());
                    pstmt.setString(3, f.getEmail());
                    pstmt.setString(4, f.getPhone());
                    pstmt.setString(5, f.getDepartment());
                    pstmt.setString(6, f.getDesignation());
                    pstmt.setInt(7, f.getYearsOfExperience());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            } catch (SQLException e) {
                System.err.println("Faculty migration error: " + e.getMessage());
            }
        }
    }

    private static void migrateCourses() {
        Object data = FileHandler.loadFromFile("courses.dat");
        if (data instanceof HashMap) {
            Map<String, Course> courses = (Map<String, Course>) data;
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT OR REPLACE INTO courses (course_id, course_name, credits, faculty_id) VALUES (?, ?, ?, ?)")) {
                for (Course c : courses.values()) {
                    pstmt.setString(1, c.getCourseId());
                    pstmt.setString(2, c.getCourseName());
                    pstmt.setInt(3, c.getCredits());
                    pstmt.setString(4, c.getAssignedFacultyId());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            } catch (SQLException e) {
                System.err.println("Course migration error: " + e.getMessage());
            }
        }
    }

    private static void migrateBooks() {
        Object data = FileHandler.loadFromFile("books.dat");
        if (data instanceof HashMap) {
            Map<String, Book> books = (Map<String, Book>) data;
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT OR REPLACE INTO books (book_id, title, author, is_available) VALUES (?, ?, ?, ?)")) {
                for (Book b : books.values()) {
                    pstmt.setString(1, b.getBookId());
                    pstmt.setString(2, b.getTitle());
                    pstmt.setString(3, b.getAuthor());
                    pstmt.setBoolean(4, b.isAvailable());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            } catch (SQLException e) {
                System.err.println("Book migration error: " + e.getMessage());
            }
        }
    }

    private static void migrateRooms() {
        Object data = FileHandler.loadFromFile("hostel.dat");
        if (data instanceof HashMap) {
            Map<String, Room> rooms = (Map<String, Room>) data;
            try (Connection conn = DatabaseManager.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT OR REPLACE INTO rooms (room_key, room_number, hostel_name, capacity, occupied) VALUES (?, ?, ?, ?, ?)")) {
                for (Room r : rooms.values()) {
                    pstmt.setString(1, r.getStorageKey());
                    pstmt.setString(2, r.getRoomNumber());
                    pstmt.setString(3, r.getHostelName());
                    pstmt.setInt(4, r.getCapacity());
                    pstmt.setInt(5, r.getOccupied());
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
            } catch (SQLException e) {
                System.err.println("Room migration error: " + e.getMessage());
            }
        }
    }
}
