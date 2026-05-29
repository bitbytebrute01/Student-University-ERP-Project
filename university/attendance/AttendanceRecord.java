package com.university.attendance;

import java.io.Serializable;
import java.util.Date;

public class AttendanceRecord implements Serializable {
    private String studentId;
    private String courseId;
    private Date date;
    private boolean isPresent;

    public AttendanceRecord(String studentId, String courseId, boolean isPresent) {
        this.studentId = studentId;
        this.courseId = courseId;
        this.isPresent = isPresent;
        this.date = new Date();
    }

    public String getStudentId() { return studentId; }
    public String getCourseId() { return courseId; }
    public Date getDate() { return date; }
    public boolean isPresent() { return isPresent; }
}
