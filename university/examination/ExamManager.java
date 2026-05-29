package com.university.examination;

import com.university.exceptions.InvalidGradeException;
import com.university.interfaces.ResultGenerator;
import com.university.models.Student;
import com.university.students.StudentManager;
import com.university.utils.FileHandler;

import java.util.HashMap;

public class ExamManager implements ResultGenerator {
    private StudentManager studentManager;
    // Map of studentId -> (courseId -> marks)
    private HashMap<String, HashMap<String, Double>> studentMarks;
    private static final String MARKS_FILE = "marks.dat";

    public ExamManager(StudentManager studentManager) {
        this.studentManager = studentManager;
        loadMarks();
    }

    @SuppressWarnings("unchecked")
    private void loadMarks() {
        Object data = FileHandler.loadFromFile(MARKS_FILE);
        if (data instanceof HashMap) {
            studentMarks = (HashMap<String, HashMap<String, Double>>) data;
        } else {
            studentMarks = new HashMap<>();
        }
    }

    private void saveMarks() {
        FileHandler.saveToFile(MARKS_FILE, studentMarks);
    }

    public void uploadMarks(String studentId, String courseId, double marks) throws InvalidGradeException {
        if (marks < 0 || marks > 100) throw new InvalidGradeException("Invalid marks");
        studentMarks.putIfAbsent(studentId, new HashMap<>());
        studentMarks.get(studentId).put(courseId, marks);
        saveMarks();
        updateStudentCgpa(studentId);
    }

    private void updateStudentCgpa(String studentId) {
        HashMap<String, Double> marksMap = studentMarks.get(studentId);
        if (marksMap != null && !marksMap.isEmpty()) {
            double totalGpa = 0;
            for (double marks : marksMap.values()) {
                try {
                    totalGpa += GradeCalculator.calculateGPA(marks);
                } catch (InvalidGradeException ignored) {}
            }
            double cgpa = totalGpa / marksMap.size();
            Student s = studentManager.searchById(studentId);
            if (s != null) {
                s.setCgpa(cgpa);
                try {
                    studentManager.updateStudent(s);
                } catch (Exception ignored) {}
            }
        }
    }

    @Override
    public String generateResult(String studentId) {
        Student s = studentManager.searchById(studentId);
        if (s == null) return "Student not found.";
        
        StringBuilder sb = new StringBuilder();
        sb.append("--- RESULT FOR ").append(s.getName()).append(" ---\n");
        sb.append("CGPA: ").append(s.getCgpa()).append("\n");
        
        HashMap<String, Double> marksMap = studentMarks.get(studentId);
        if (marksMap != null) {
            for (String courseId : marksMap.keySet()) {
                try {
                    sb.append("Course ").append(courseId).append(": ")
                      .append(marksMap.get(courseId)).append(" (")
                      .append(GradeCalculator.getGrade(marksMap.get(courseId))).append(")\n");
                } catch (InvalidGradeException e) {
                    sb.append("Error calculating grade for ").append(courseId).append("\n");
                }
            }
        }
        return sb.toString();
    }
}
