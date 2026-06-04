package com.university.models;

import com.university.core.Person;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Student extends Person {
    private static final long serialVersionUID = 1L;

    private String department;
    private int semester;
    private double cgpa;
    private double attendancePercentage;
    private String feeStatus;

    // Digital Identity Fields
    private List<String> skills;
    private List<String> certifications;
    private List<String> projects;
    private List<String> internships;
    private String resumePath;
    private String linkedInUrl;
    private String githubUrl;
    private String portfolioUrl;
    private List<String> achievements;
    private String profilePicturePath;
    private String coverPhotoPath;
    private String bio;
    private List<String> languages;
    private List<String> interests;
    private List<String> researchPapers;
    private List<String> workExperience;
    private int xpPoints;
    private int level;

    public Student(String id, String name, String email, String phone, String department, int semester) {
        super(id, name, email, phone);
        this.department = department;
        this.semester = semester;
        this.cgpa = 0.0;
        this.attendancePercentage = 0.0;
        this.feeStatus = "Pending";
        this.skills = new ArrayList<>();
        this.certifications = new ArrayList<>();
        this.projects = new ArrayList<>();
        this.internships = new ArrayList<>();
        this.achievements = new ArrayList<>();
        this.languages = new ArrayList<>();
        this.interests = new ArrayList<>();
        this.researchPapers = new ArrayList<>();
        this.workExperience = new ArrayList<>();
        this.bio = "Dedicated student at University.";
        this.level = 1;
    }

    // Getters and Setters
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public double getCgpa() { return cgpa; }
    public void setCgpa(double cgpa) { this.cgpa = cgpa; }

    public double getAttendancePercentage() { return attendancePercentage; }
    public void setAttendancePercentage(double attendancePercentage) { this.attendancePercentage = attendancePercentage; }

    public String getFeeStatus() { return feeStatus; }
    public void setFeeStatus(String feeStatus) { this.feeStatus = feeStatus; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }

    public List<String> getCertifications() { return certifications; }
    public void setCertifications(List<String> certifications) { this.certifications = certifications; }

    public List<String> getProjects() { return projects; }
    public void setProjects(List<String> projects) { this.projects = projects; }

    public List<String> getInternships() { return internships; }
    public void setInternships(List<String> internships) { this.internships = internships; }

    public String getResumePath() { return resumePath; }
    public void setResumePath(String resumePath) { this.resumePath = resumePath; }

    public String getLinkedInUrl() { return linkedInUrl; }
    public void setLinkedInUrl(String linkedInUrl) { this.linkedInUrl = linkedInUrl; }

    public String getGithubUrl() { return githubUrl; }
    public void setGithubUrl(String githubUrl) { this.githubUrl = githubUrl; }

    public String getPortfolioUrl() { return portfolioUrl; }
    public void setPortfolioUrl(String portfolioUrl) { this.portfolioUrl = portfolioUrl; }

    public List<String> getAchievements() { return achievements; }
    public void setAchievements(List<String> achievements) { this.achievements = achievements; }

    public String getProfilePicturePath() { return profilePicturePath; }
    public void setProfilePicturePath(String profilePicturePath) { this.profilePicturePath = profilePicturePath; }

    public String getCoverPhotoPath() { return coverPhotoPath; }
    public void setCoverPhotoPath(String coverPhotoPath) { this.coverPhotoPath = coverPhotoPath; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public List<String> getLanguages() { return languages; }
    public void setLanguages(List<String> languages) { this.languages = languages; }

    public List<String> getInterests() { return interests; }
    public void setInterests(List<String> interests) { this.interests = interests; }

    public List<String> getResearchPapers() { return researchPapers; }
    public void setResearchPapers(List<String> researchPapers) { this.researchPapers = researchPapers; }

    public List<String> getWorkExperience() { return workExperience; }
    public void setWorkExperience(List<String> workExperience) { this.workExperience = workExperience; }

    public int getXpPoints() { return xpPoints; }
    public void setXpPoints(int xpPoints) { this.xpPoints = xpPoints; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    @Override
    public void displayProfile() {
        System.out.println("Student Profile:");
        System.out.println("ID: " + id);
        System.out.println("Name: " + name);
        System.out.println("Email: " + email);
        System.out.println("Department: " + department);
        System.out.println("Semester: " + semester);
        System.out.println("CGPA: " + cgpa);
        System.out.println("Attendance: " + attendancePercentage + "%");
        System.out.println("Skills: " + skills);
    }

    @Override
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Student Report - ").append(name).append("\n");
        sb.append("ID: ").append(id).append("\n");
        sb.append("CGPA: ").append(cgpa).append("\n");
        sb.append("Attendance: ").append(attendancePercentage).append("%\n");
        sb.append("Fee Status: ").append(feeStatus).append("\n");
        sb.append("Skills: ").append(String.join(", ", skills)).append("\n");
        return sb.toString();
    }

    @Override
    public void exportReport(String filePath) {
        try {
            Files.writeString(Path.of(filePath), generateReport());
        } catch (IOException e) {
            throw new IllegalStateException("Unable to export student report: " + e.getMessage(), e);
        }
    }
}
