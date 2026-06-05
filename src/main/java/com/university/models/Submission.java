package com.university.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class Submission implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String FILE_PATH_SEPARATOR = "\n";

    private String id;
    private String assignmentId;
    private String studentId;
    private String submissionText;
    private String filePath;
    private List<String> attachmentPaths = new ArrayList<>();
    private Date submissionDate;
    private String status;
    private double marks;
    private String feedback;
    private boolean graded;

    public Submission(String id, String assignmentId, String studentId, String filePath) {
        this.id = id;
        this.assignmentId = assignmentId;
        this.studentId = studentId;
        setFilePath(filePath);
        this.submissionDate = new Date();
        this.status = "Submitted";
        this.graded = false;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getAssignmentId() { return assignmentId; }
    public String getStudentId() { return studentId; }
    public String getSubmissionText() { return submissionText; }
    public void setSubmissionText(String submissionText) { this.submissionText = submissionText; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) {
        this.filePath = filePath;
        this.attachmentPaths = parseFilePaths(filePath);
    }
    public List<String> getAttachmentPaths() { return new ArrayList<>(attachmentPaths); }
    public void setAttachmentPaths(List<String> attachmentPaths) {
        this.attachmentPaths = attachmentPaths == null ? new ArrayList<>() : attachmentPaths.stream()
                .filter(path -> path != null && !path.isBlank())
                .collect(Collectors.toCollection(ArrayList::new));
        this.filePath = String.join(FILE_PATH_SEPARATOR, this.attachmentPaths);
    }
    public boolean hasAttachments() { return !attachmentPaths.isEmpty(); }
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

    private List<String> parseFilePaths(String filePath) {
        List<String> paths = new ArrayList<>();
        if (filePath == null || filePath.isBlank()) {
            return paths;
        }
        for (String path : filePath.split(FILE_PATH_SEPARATOR)) {
            if (path != null && !path.isBlank()) {
                paths.add(path.trim());
            }
        }
        return paths;
    }
}
