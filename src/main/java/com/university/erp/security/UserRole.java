package com.university.erp.security;

public enum UserRole {
    ADMIN("Admin"),
    FACULTY("Faculty"),
    STUDENT("Student");

    private final String label;

    UserRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static UserRole fromString(String text) {
        for (UserRole role : UserRole.values()) {
            if (role.label.equalsIgnoreCase(text)) {
                return role;
            }
        }
        return STUDENT; // Default
    }
}
