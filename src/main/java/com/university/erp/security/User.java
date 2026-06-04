package com.university.erp.security;

import com.university.interfaces.Authenticatable;
import java.io.Serializable;

public abstract class User implements Authenticatable, Serializable {
    protected String username;
    protected String password;
    protected String refId;
    protected boolean authenticated;

    public User(String username, String password) {
        this(username, password, null);
    }

    public User(String username, String password, String refId) {
        this.username = username;
        this.password = password;
        this.refId = refId;
        this.authenticated = false;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRefId() { return refId; }

    @Override
    public boolean login(String inputUsername, String inputPassword) {
        if (this.username.equals(inputUsername)) {
            // The stored 'password' field actually contains the hash
            boolean match;
            try {
                match = org.mindrot.jbcrypt.BCrypt.checkpw(inputPassword, this.password);
            } catch (IllegalArgumentException e) {
                match = this.password != null && this.password.equals(inputPassword);
            }
            if (match) {
                this.authenticated = true;
                return true;
            }
        }
        return false;
    }

    @Override
    public void logout() {
        this.authenticated = false;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    public abstract UserRole getRole();
}
