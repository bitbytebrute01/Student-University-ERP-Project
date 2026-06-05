package com.university.models;

import java.io.Serializable;
import java.util.Date;

public class AssignmentNotification implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String studentId;
    private final String assignmentId;
    private final String courseId;
    private final String title;
    private final String message;
    private final Date createdAt;
    private final Date readAt;

    public AssignmentNotification(String id, String studentId, String assignmentId, String courseId,
                                  String title, String message, Date createdAt, Date readAt) {
        this.id = id;
        this.studentId = studentId;
        this.assignmentId = assignmentId;
        this.courseId = courseId;
        this.title = title;
        this.message = message;
        this.createdAt = createdAt;
        this.readAt = readAt;
    }

    public String getId() { return id; }
    public String getStudentId() { return studentId; }
    public String getAssignmentId() { return assignmentId; }
    public String getCourseId() { return courseId; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public Date getCreatedAt() { return createdAt; }
    public Date getReadAt() { return readAt; }
    public boolean isUnread() { return readAt == null; }
}
