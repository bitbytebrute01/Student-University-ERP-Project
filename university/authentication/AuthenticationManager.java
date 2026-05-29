package com.university.authentication;

import com.university.exceptions.UnauthorizedAccessException;
import com.university.utils.FileHandler;

import java.util.HashMap;

public class AuthenticationManager {
    private HashMap<String, User> users;
    private static final String USERS_FILE = "users.dat";

    public AuthenticationManager() {
        loadUsers();
        // Create default admin if empty
        if (users.isEmpty()) {
            registerUser(new AdminUser("admin", "admin123"));
        }
    }

    @SuppressWarnings("unchecked")
    private void loadUsers() {
        Object data = FileHandler.loadFromFile(USERS_FILE);
        if (data instanceof HashMap) {
            users = (HashMap<String, User>) data;
        } else {
            users = new HashMap<>();
        }
    }

    private void saveUsers() {
        FileHandler.saveToFile(USERS_FILE, users);
    }

    public void registerUser(User user) {
        users.put(user.getUsername(), user);
        saveUsers();
    }

    public User authenticate(String username, String password) throws UnauthorizedAccessException {
        User u = users.get(username);
        if (u != null && u.login(username, password)) {
            return u;
        }
        throw new UnauthorizedAccessException("Invalid credentials.");
    }
}
