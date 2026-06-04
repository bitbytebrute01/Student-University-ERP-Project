package com.university.models;

import java.io.Serializable;
import java.util.Date;

public class Certification implements Serializable {
    private String id;
    private String studentId;
    private String title;
    private String issuer;
    private Date completionDate;
    private String filePath;

    public Certification(String id, String studentId, String title, String issuer, Date completionDate) {
        this.id = id;
        this.studentId = studentId;
        this.title = title;
        this.issuer = issuer;
        this.completionDate = completionDate;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public Date getCompletionDate() { return completionDate; }
    public void setCompletionDate(Date completionDate) { this.completionDate = completionDate; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
