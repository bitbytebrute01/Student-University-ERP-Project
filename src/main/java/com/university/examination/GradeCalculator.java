package com.university.examination;

import com.university.exceptions.InvalidGradeException;

public class GradeCalculator {
    public static double calculateGPA(double marks) throws InvalidGradeException {
        if (marks < 0 || marks > 100) {
            throw new InvalidGradeException("Marks must be between 0 and 100");
        }
        if (marks >= 90) return 10.0;
        if (marks >= 80) return 9.0;
        if (marks >= 70) return 8.0;
        if (marks >= 60) return 7.0;
        if (marks >= 50) return 6.0;
        return 0.0;
    }

    public static String getGrade(double marks) throws InvalidGradeException {
        if (marks < 0 || marks > 100) {
            throw new InvalidGradeException("Marks must be between 0 and 100");
        }
        if (marks >= 90) return "S";
        if (marks >= 80) return "A";
        if (marks >= 70) return "B";
        if (marks >= 60) return "C";
        if (marks >= 50) return "D";
        return "F";
    }
}
