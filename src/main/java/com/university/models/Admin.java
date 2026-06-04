package com.university.models;

import com.university.core.Person;

public class Admin extends Person {
    private String role;

    public Admin(String id, String name, String email, String phone, String role) {
        super(id, name, email, phone);
        this.role = role;
    }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    @Override
    public void displayProfile() {
        System.out.println("Admin Profile:");
        System.out.println("Name: " + name);
        System.out.println("Role: " + role);
    }

    @Override
    public String generateReport() {
        return "Admin actions report for " + name;
    }

    @Override
    public void exportReport(String filePath) {
        // Impl later
    }
}
