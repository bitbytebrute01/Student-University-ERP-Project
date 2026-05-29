package com.university.models;

import com.university.core.Person;

public class Faculty extends Person {
    private String department;
    private String designation;
    private int yearsOfExperience;

    public Faculty(String id, String name, String email, String phone, String department, String designation, int yearsOfExperience) {
        super(id, name, email, phone);
        this.department = department;
        this.designation = designation;
        this.yearsOfExperience = yearsOfExperience;
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public int getYearsOfExperience() { return yearsOfExperience; }
    public void setYearsOfExperience(int yearsOfExperience) { this.yearsOfExperience = yearsOfExperience; }

    @Override
    public void displayProfile() {
        System.out.println("Faculty Profile:");
        System.out.println("ID: " + id);
        System.out.println("Name: " + name);
        System.out.println("Department: " + department);
        System.out.println("Designation: " + designation);
    }

    @Override
    public String generateReport() {
        return "Faculty Report: " + name + " (" + designation + ") - " + department;
    }

    @Override
    public void exportReport(String filePath) {
        // Impl later
    }
}
