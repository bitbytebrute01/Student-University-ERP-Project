package com.university.students;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.university.db.DatabaseManager;
import com.university.exceptions.StudentNotFoundException;
import com.university.interfaces.Searchable;
import com.university.models.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class StudentManager implements Searchable<Student> {
    private static final Gson gson = new Gson();

    public StudentManager() {}

    public void addStudent(Student student) {
        String sql = "INSERT INTO students (id, name, email, phone, department, semester, cgpa, attendance, fee_status, " +
                "skills, projects, certifications, bio, resume_path, profile_pic, cover_pic, linkedin_url, github_url, " +
                "portfolio_url, languages, interests, research_papers, experience, xp_points, level) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, student.getId());
            pstmt.setString(2, student.getName());
            pstmt.setString(3, student.getEmail());
            pstmt.setString(4, student.getPhone());
            pstmt.setString(5, student.getDepartment());
            pstmt.setInt(6, student.getSemester());
            pstmt.setDouble(7, student.getCgpa());
            pstmt.setDouble(8, student.getAttendancePercentage());
            pstmt.setString(9, student.getFeeStatus());
            pstmt.setString(10, gson.toJson(student.getSkills()));
            pstmt.setString(11, gson.toJson(student.getProjects()));
            pstmt.setString(12, gson.toJson(student.getCertifications()));
            pstmt.setString(13, student.getBio());
            pstmt.setString(14, student.getResumePath());
            pstmt.setString(15, student.getProfilePicturePath());
            pstmt.setString(16, student.getCoverPhotoPath());
            pstmt.setString(17, student.getLinkedInUrl());
            pstmt.setString(18, student.getGithubUrl());
            pstmt.setString(19, student.getPortfolioUrl());
            pstmt.setString(20, gson.toJson(student.getLanguages()));
            pstmt.setString(21, gson.toJson(student.getInterests()));
            pstmt.setString(22, gson.toJson(student.getResearchPapers()));
            pstmt.setString(23, gson.toJson(student.getWorkExperience()));
            pstmt.setInt(24, student.getXpPoints());
            pstmt.setInt(25, student.getLevel());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding student: " + e.getMessage());
        }
    }

    public void updateStudent(Student student) throws StudentNotFoundException {
        String sql = "UPDATE students SET name=?, email=?, phone=?, department=?, semester=?, cgpa=?, attendance=?, " +
                "fee_status=?, skills=?, projects=?, certifications=?, bio=?, resume_path=?, profile_pic=?, cover_pic=?, " +
                "linkedin_url=?, github_url=?, portfolio_url=?, languages=?, interests=?, research_papers=?, " +
                "experience=?, xp_points=?, level=? WHERE id=?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, student.getName());
            pstmt.setString(2, student.getEmail());
            pstmt.setString(3, student.getPhone());
            pstmt.setString(4, student.getDepartment());
            pstmt.setInt(5, student.getSemester());
            pstmt.setDouble(6, student.getCgpa());
            pstmt.setDouble(7, student.getAttendancePercentage());
            pstmt.setString(8, student.getFeeStatus());
            pstmt.setString(9, gson.toJson(student.getSkills()));
            pstmt.setString(10, gson.toJson(student.getProjects()));
            pstmt.setString(11, gson.toJson(student.getCertifications()));
            pstmt.setString(12, student.getBio());
            pstmt.setString(13, student.getResumePath());
            pstmt.setString(14, student.getProfilePicturePath());
            pstmt.setString(15, student.getCoverPhotoPath());
            pstmt.setString(16, student.getLinkedInUrl());
            pstmt.setString(17, student.getGithubUrl());
            pstmt.setString(18, student.getPortfolioUrl());
            pstmt.setString(19, gson.toJson(student.getLanguages()));
            pstmt.setString(20, gson.toJson(student.getInterests()));
            pstmt.setString(21, gson.toJson(student.getResearchPapers()));
            pstmt.setString(22, gson.toJson(student.getWorkExperience()));
            pstmt.setInt(23, student.getXpPoints());
            pstmt.setInt(24, student.getLevel());
            pstmt.setString(25, student.getId());
            if (pstmt.executeUpdate() == 0) throw new StudentNotFoundException("Student not found.");
        } catch (SQLException e) {
            System.err.println("Error updating student: " + e.getMessage());
        }
    }

    public void deleteStudent(String studentId) throws StudentNotFoundException {
        String sql = "DELETE FROM students WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            if (pstmt.executeUpdate() == 0) throw new StudentNotFoundException("Student not found.");
        } catch (SQLException e) {
            System.err.println("Error deleting student: " + e.getMessage());
        }
    }

    @Override
    public Student searchById(String id) {
        String sql = "SELECT * FROM students WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToStudent(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error searching student: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Student> searchByName(String name) {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM students WHERE name LIKE ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + name + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapResultSetToStudent(rs));
        } catch (SQLException e) {
            System.err.println("Error searching student by name: " + e.getMessage());
        }
        return list;
    }

    public void displayAllStudents() {
        for (Student s : getStudentMap().values()) {
            s.printDetails();
            System.out.println("-----------------");
        }
    }

    public List<Student> getStudentsSortedByCgpa() {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT * FROM students ORDER BY cgpa DESC";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSetToStudent(rs));
        } catch (SQLException e) {
            System.err.println("Error getting sorted students: " + e.getMessage());
        }
        return list;
    }

    public HashMap<String, Student> getStudentMap() {
        HashMap<String, Student> map = new HashMap<>();
        String sql = "SELECT * FROM students";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Student s = mapResultSetToStudent(rs);
                map.put(s.getId(), s);
            }
        } catch (SQLException e) {
            System.err.println("Error getting student map: " + e.getMessage());
        }
        return map;
    }

    public int getAssignmentCount(String studentId) {
        String sql = "SELECT COUNT(*) FROM enrollments e JOIN assignments a ON e.course_id = a.course_id WHERE e.student_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    public int getPendingAssignmentCount(String studentId) {
        String sql = "SELECT COUNT(*) FROM enrollments e JOIN assignments a ON e.course_id = a.course_id " +
                     "WHERE e.student_id = ? AND a.id NOT IN (SELECT assignment_id FROM submissions WHERE student_id = ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, studentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private Student mapResultSetToStudent(ResultSet rs) throws SQLException {
        Student s = new Student(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("department"),
                rs.getInt("semester")
        );
        s.setCgpa(rs.getDouble("cgpa"));
        s.setAttendancePercentage(rs.getDouble("attendance"));
        s.setFeeStatus(rs.getString("fee_status"));
        s.setBio(rs.getString("bio"));
        s.setSkills(gson.fromJson(rs.getString("skills"), new TypeToken<List<String>>(){}.getType()));
        s.setProjects(gson.fromJson(rs.getString("projects"), new TypeToken<List<String>>(){}.getType()));
        s.setCertifications(gson.fromJson(rs.getString("certifications"), new TypeToken<List<String>>(){}.getType()));
        s.setResumePath(rs.getString("resume_path"));
        s.setLinkedInUrl(rs.getString("linkedin_url"));
        s.setGithubUrl(rs.getString("github_url"));
        s.setPortfolioUrl(rs.getString("portfolio_url"));
        s.setLanguages(gson.fromJson(rs.getString("languages"), new TypeToken<List<String>>(){}.getType()));
        s.setInterests(gson.fromJson(rs.getString("interests"), new TypeToken<List<String>>(){}.getType()));
        s.setResearchPapers(gson.fromJson(rs.getString("research_papers"), new TypeToken<List<String>>(){}.getType()));
        s.setWorkExperience(gson.fromJson(rs.getString("experience"), new TypeToken<List<String>>(){}.getType()));
        s.setProfilePicturePath(rs.getString("profile_pic"));
        s.setCoverPhotoPath(rs.getString("cover_pic"));
        s.setXpPoints(rs.getInt("xp_points"));
        s.setLevel(rs.getInt("level"));
        
        if (s.getSkills() == null) s.setSkills(new ArrayList<>());
        if (s.getProjects() == null) s.setProjects(new ArrayList<>());
        if (s.getCertifications() == null) s.setCertifications(new ArrayList<>());
        if (s.getLanguages() == null) s.setLanguages(new ArrayList<>());
        if (s.getInterests() == null) s.setInterests(new ArrayList<>());
        if (s.getResearchPapers() == null) s.setResearchPapers(new ArrayList<>());
        if (s.getWorkExperience() == null) s.setWorkExperience(new ArrayList<>());
        return s;
    }
}
