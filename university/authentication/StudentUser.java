package com.university.authentication;

public class StudentUser extends User {
    public StudentUser(String username, String password) {
        super(username, password);
    }

    @Override
    public String getRole() {
        return "Student";
    }
}
