package com.university.models;

import java.io.Serializable;
import java.util.Date;

public class Submission implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String assignmentId;
    private String studentId;
    private String filePath;
    private Date submissionDate;
    private String status;
    private double marks;
    private String feedback;
    private boolean graded;

    public Submission(String id, String assignmentId, String studentId, String filePath) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        this.filePath = filePath;
        this.submissionDate = new Date();
        this.status = "Submitted";
        this.graded = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getAssignmentId() { return assignmentId; }
    public String getStudentId() { return studentId; }
    public String getFilePath() { return filePath; }
    public Date getSubmissionDate() { return submissionDate; }
    public void setSubmissionDate(Date submissionDate) { this.submissionDate = submissionDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public double getMarks() { return marks; }
    public void setMarks(double marks) { this.marks = marks; }
    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
    public boolean isGraded() { return graded; }
    public void setGraded(boolean graded) { this.graded = graded; }
}
