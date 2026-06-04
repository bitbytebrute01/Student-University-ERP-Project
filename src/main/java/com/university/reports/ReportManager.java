package com.university.reports;

import com.university.models.Student;
import com.university.students.StudentManager;

import java.util.List;

public class ReportManager {
    
    public static String generateOverallStudentReport(StudentManager studentManager) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("================ OVERALL STUDENT REPORT ================\n");
        List<Student> students = studentManager.getStudentsSortedByCgpa();
        for (Student s : students) {
            buffer.append(String.format("%-10s | %-20s | CGPA: %-5.2f | Attendance: %-5.2f%%\n",
                    s.getId(), s.getName(), s.getCgpa(), s.getAttendancePercentage()));
        }
        buffer.append("========================================================\n");
        return buffer.toString();
    }
}
