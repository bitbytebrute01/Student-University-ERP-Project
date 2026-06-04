package com.university.models;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Assignment implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String id;
    private String courseId;
    private String createdBy;
    private String title;
    private String description;
    private Date deadline;
    private String dueTime;
    private Date createdAt;
    private String status;
    private double maxMarks;
    private String attachmentPath;
    private String assignmentType; // Homework, Quiz, Project, Exam
    private String additionalMaterialPath;

    public Assignment(String id, String courseId, String title, String description, Date deadline, double maxMarks) {
        this.id = id;
        this.courseId = courseId;
        this.title = title;
        this.description = description;
        this.deadline = deadline;
        this.maxMarks = maxMarks;
        this.dueTime = deadline == null ? "23:59" : new SimpleDateFormat("HH:mm").format(deadline);
        this.createdAt = new Date();
        this.status = "Published";
        this.assignmentType = "Homework";
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getCourseId() { return courseId; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Date getDeadline() { return deadline; }
    public void setDeadline(Date deadline) { this.deadline = deadline; }
    public String getDueTime() { return dueTime; }
    public void setDueTime(String dueTime) { this.dueTime = dueTime; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getMaxMarks() { return maxMarks; }
    public String getAttachmentPath() { return attachmentPath; }
    public void setAttachmentPath(String attachmentPath) { this.attachmentPath = attachmentPath; }

    public String getAssignmentType() { return assignmentType; }
    public void setAssignmentType(String assignmentType) { this.assignmentType = assignmentType; }

    public String getAdditionalMaterialPath() { return additionalMaterialPath; }
    public void setAdditionalMaterialPath(String additionalMaterialPath) { this.additionalMaterialPath = additionalMaterialPath; }
}
