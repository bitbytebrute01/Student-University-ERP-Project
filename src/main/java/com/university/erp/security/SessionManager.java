package com.university.erp.security;

public class SessionManager {
    private static User currentUser;

    public static void startSession(User user) {
        currentUser = user;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void endSession() {
        if (currentUser != null) {
            currentUser.logout();
            currentUser = null;
        }
    }

    public static boolean hasActiveSession() {
        return currentUser != null && currentUser.isAuthenticated();
    }
}
