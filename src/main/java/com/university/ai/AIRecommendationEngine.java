package com.university.ai;

import com.university.models.Course;
import com.university.models.Student;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AIRecommendationEngine {

    public double predictCGPA(Student student) {
        // Heuristic: Past attendance directly influences CGPA prediction.
        // Base assuming they maintain their current standing, adjusted by attendance.
        double currentCgpa = student.getCgpa() == 0 ? 5.0 : student.getCgpa();
        double attendanceFactor = student.getAttendancePercentage() / 100.0;
        
        double predicted = currentCgpa + (attendanceFactor * 1.5) - 0.5;
        if (predicted > 10.0) predicted = 10.0;
        if (predicted < 0) predicted = 0;
        
        return Math.round(predicted * 100.0) / 100.0;
    }

    public boolean predictPlacement(Student student) {
        // Heuristic: Above 7.5 CGPA and 80% attendance is highly likely to be placed.
        return student.getCgpa() >= 7.5 && student.getAttendancePercentage() >= 80.0;
    }

    public double calculatePlacementReadiness(Student student) {
        double score = 0;
        if (student.getCgpa() >= 8.5) score += 40;
        else if (student.getCgpa() >= 7.5) score += 30;
        else if (student.getCgpa() >= 6.0) score += 20;

        if (student.getAttendancePercentage() >= 85) score += 20;
        else if (student.getAttendancePercentage() >= 75) score += 15;

        if (student.getSkills() != null) {
            score += Math.min(student.getSkills().size() * 5, 20); // Max 20 for skills
        }
        if (student.getProjects() != null) {
            score += Math.min(student.getProjects().size() * 10, 20); // Max 20 for projects
        }

        return Math.min(score, 100.0);
    }

    public String getCareerRecommendation(Student student) {
        if (student.getSkills().contains("Java") || student.getSkills().contains("Python")) {
            return "Software Developer / Engineer";
        } else if (student.getSkills().contains("Design") || student.getSkills().contains("UI")) {
            return "UI/UX Designer";
        }
        return "General Technology Role";
    }

    public List<String> getSkillGapAnalysis(Student student) {
        List<String> requiredSkills = Arrays.asList("Java", "SQL", "Spring Boot", "Git", "Cloud Computing");
        List<String> gap = new ArrayList<>();
        for (String skill : requiredSkills) {
            if (!student.getSkills().stream().anyMatch(s -> s.equalsIgnoreCase(skill))) {
                gap.add(skill);
            }
        }
        return gap;
    }

    public String getAcademicPerformanceRisk(Student student) {
        if (student.getAttendancePercentage() < 75 && student.getCgpa() < 6.0) return "HIGH RISK: Academic probation likely.";
        if (student.getAttendancePercentage() < 75 || student.getCgpa() < 7.0) return "MEDIUM RISK: Improvement needed.";
        return "LOW RISK: On track for excellence.";
    }

    public String getPersonalizedStudyPlan(Student student) {
        if (student.getCgpa() < 7.0) return "Focus 4 hours daily on Core Subjects and solve 10 practice problems.";
        if (student.getSkills().size() < 3) return "Dedicate 2 hours daily to learn a new Technical Skill (e.g., Python or SQL).";
        return "Focus on building advanced Projects and contributing to Open Source.";
    }

    public List<Course> recommendCourses(Student student, List<Course> availableCourses) {
        // Heuristic: Recommend courses based on current semester (e.g., student semester + 1).
        List<Course> recommendations = new ArrayList<>();
        for (Course c : availableCourses) {
            // Very simple heuristic: pick first 2 available courses.
            if (recommendations.size() < 2) {
                recommendations.add(c);
            }
        }
        return recommendations;
    }

    public List<Student> detectRiskStudents(List<Student> students) {
        List<Student> atRisk = new ArrayList<>();
        for (Student s : students) {
            // Heuristic: Low attendance and low CGPA = Risk
            if (s.getAttendancePercentage() > 0 && s.getAttendancePercentage() < 60.0 && s.getCgpa() < 5.0) {
                atRisk.add(s);
            }
        }
        return atRisk;
    }
}
