package com.university.erp.security;

public class FacultyUser extends User {
    public FacultyUser(String username, String password) {
        super(username, password);
    }

    public FacultyUser(String username, String password, String refId) {
        super(username, password, refId);
    }

    @Override
    public UserRole getRole() {
        return UserRole.FACULTY;
    }
}
