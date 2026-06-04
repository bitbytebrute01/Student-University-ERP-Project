package com.university.erp.security;

public class StudentUser extends User {
    public StudentUser(String username, String password) {
        super(username, password);
    }

    public StudentUser(String username, String password, String refId) {
        super(username, password, refId);
    }

    @Override
    public UserRole getRole() {
        return UserRole.STUDENT;
    }
}
