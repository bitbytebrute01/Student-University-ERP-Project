package com.university.erp.security;

public class SessionManager {
    private static User currentUser;

    public static synchronized void startSession(User user) {
        if (currentUser != null) {
            System.out.println("Warning: starting a new session while another session is active for: " + currentUser.getUsername());
        }
        currentUser = user;
    }

    public static synchronized User getCurrentUser() {
        return currentUser;
    }

    public static synchronized void endSession() {
        if (currentUser != null) {
            try {
                currentUser.logout();
            } catch (Exception ignored) {}
            currentUser = null;
        }
    }

    public static synchronized boolean hasActiveSession() {
        return currentUser != null && currentUser.isAuthenticated();
    }
}
