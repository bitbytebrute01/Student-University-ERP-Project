package com.university.authentication;

public class FacultyUser extends User {
    public FacultyUser(String username, String password) {
        super(username, password);
    }

    @Override
    public String getRole() {
        return "Faculty";
    }
}
