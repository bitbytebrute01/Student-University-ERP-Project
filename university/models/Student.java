package com.university.models;

import com.university.core.Person;

public class Student extends Person {
    private String department;
    private int semester;
    private double cgpa;
    private double attendancePercentage;
    private String feeStatus;

    public Student(String id, String name, String email, String phone, String department, int semester) {
        super(id, name, email, phone);
        this.department = department;
        this.semester = semester;
        this.cgpa = 0.0;
        this.attendancePercentage = 0.0;
        this.feeStatus = "Pending";
    }

    // Getters and Setters
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    
    public int getSemester() { return semester; }
    public void setSemester(int semester) { this.semester = semester; }

    public double getCgpa() { return cgpa; }
    public void setCgpa(double cgpa) { this.cgpa = cgpa; }

    public double getAttendancePercentage() { return attendancePercentage; }
    public void setAttendancePercentage(double attendancePercentage) { this.attendancePercentage = attendancePercentage; }

    public String getFeeStatus() { return feeStatus; }
    public void setFeeStatus(String feeStatus) { this.feeStatus = feeStatus; }

    @Override
    public void displayProfile() {
        System.out.println("Student Profile:");
        System.out.println("ID: " + id);
        System.out.println("Name: " + name);
        System.out.println("Email: " + email);
        System.out.println("Department: " + department);
        System.out.println("Semester: " + semester);
        System.out.println("CGPA: " + cgpa);
        System.out.println("Attendance: " + attendancePercentage + "%");
    }

    @Override
    public String generateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("Student Report - ").append(name).append("\n");
        sb.append("ID: ").append(id).append("\n");
        sb.append("CGPA: ").append(cgpa).append("\n");
        sb.append("Attendance: ").append(attendancePercentage).append("%\n");
        sb.append("Fee Status: ").append(feeStatus).append("\n");
        return sb.toString();
    }

    @Override
    public void exportReport(String filePath) {
        // Implementation later
    }
}
