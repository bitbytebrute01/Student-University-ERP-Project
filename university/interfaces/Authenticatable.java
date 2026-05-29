package com.university.interfaces;

public interface Authenticatable {
    boolean login(String username, String password);
    void logout();
    boolean isAuthenticated();
}
