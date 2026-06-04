package com.university.models;

import com.university.interfaces.Storable;

public class Course implements Storable, Comparable<Course> {
    private String courseId;
    private String courseName;
    private int credits;
    private String assignedFacultyId;

    public Course(String courseId, String courseName, int credits) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.credits = credits;
    }

    public String getCourseId() { return courseId; }
    public String getCourseName() { return courseName; }
    public int getCredits() { return credits; }
    public String getAssignedFacultyId() { return assignedFacultyId; }
    public void setAssignedFacultyId(String assignedFacultyId) { this.assignedFacultyId = assignedFacultyId; }

    @Override
    public String getStorageKey() {
        return courseId;
    }

    @Override
    public int compareTo(Course other) {
        return this.courseName.compareToIgnoreCase(other.courseName);
    }
}
