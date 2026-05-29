package com.university.ai;

import com.university.models.Course;
import com.university.models.Student;

import java.util.ArrayList;
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
