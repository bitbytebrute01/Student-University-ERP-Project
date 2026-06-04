package com.university.library;

import java.io.Serializable;
import java.util.Date;

public class IssueRecord implements Serializable {
    private String issueId;
    private String bookId;
    private String studentId;
    private Date issueDate;
    private Date returnDate;

    public IssueRecord(String issueId, String bookId, String studentId) {
        this.issueId = issueId;
        this.bookId = bookId;
        this.studentId = studentId;
        this.issueDate = new Date();
    }

    public String getIssueId() { return issueId; }
    public String getBookId() { return bookId; }
    public String getStudentId() { return studentId; }
    public Date getIssueDate() { return issueDate; }
    public Date getReturnDate() { return returnDate; }
    public void setReturnDate(Date returnDate) { this.returnDate = returnDate; }
}
