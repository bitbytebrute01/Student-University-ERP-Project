package com.university.erp.security;

public class AdminUser extends User {
    public AdminUser(String username, String password) {
        super(username, password);
    }

    public AdminUser(String username, String password, String refId) {
        super(username, password, refId);
    }

    @Override
    public UserRole getRole() {
        return UserRole.ADMIN;
    }
}
