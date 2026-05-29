package com.university.authentication;

import com.university.interfaces.Authenticatable;
import java.io.Serializable;

public abstract class User implements Authenticatable, Serializable {
    protected String username;
    protected String password;
    protected boolean authenticated;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.authenticated = false;
    }

    public String getUsername() { return username; }

    @Override
    public boolean login(String inputUsername, String inputPassword) {
        if (this.username.equals(inputUsername) && this.password.equals(inputPassword)) {
            this.authenticated = true;
            return true;
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

    public abstract String getRole();
}
