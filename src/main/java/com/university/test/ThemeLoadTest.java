package com.university.test;

import com.university.db.DatabaseManager;
import com.university.main.UniversityERP;
import com.university.erp.security.AuthenticationManager;
import com.university.erp.security.SessionManager;
import com.university.erp.security.User;
import com.university.erp.gui.theme.ThemeManager;

public class ThemeLoadTest {
    public static void main(String[] args) {
        System.out.println("ThemeLoadTest starting...");
        DatabaseManager.initializeSchema();
        // Register an admin user so we can test theme loading without calling UniversityERP.seedData()
        com.university.erp.security.AdminUser adminUser = new com.university.erp.security.AdminUser("admin", "admin123");
        AuthenticationManager authManager = new AuthenticationManager();
        authManager.registerUser(adminUser, "admin123");
        try {
            AuthenticationManager auth = new AuthenticationManager();
            User admin = auth.authenticate("admin", "admin123");
            System.out.println("Authenticated as: " + admin.getUsername());
            SessionManager.startSession(admin);
            System.out.println("Current session user: " + SessionManager.getCurrentUser().getUsername());
            try {
                ThemeManager.loadUserTheme();
                System.out.println("Theme loaded OK");
            } catch (Exception e) {
                System.err.println("ThemeManager.loadUserTheme threw:");
                e.printStackTrace();
            }

            // Now simulate GUI login flow via LoginFrame.handleLogin()
            try {
                javax.swing.SwingUtilities.invokeAndWait(() -> {}); // ensure EDT available
                com.university.gui.LoginFrame loginFrame = new com.university.gui.LoginFrame(auth);
                java.lang.reflect.Field userField = com.university.gui.LoginFrame.class.getDeclaredField("userField");
                java.lang.reflect.Field passField = com.university.gui.LoginFrame.class.getDeclaredField("passField");
                userField.setAccessible(true);
                passField.setAccessible(true);
                ((javax.swing.JTextField)userField.get(loginFrame)).setText("admin");
                ((javax.swing.JPasswordField)passField.get(loginFrame)).setText("admin123");

                java.lang.reflect.Method handle = com.university.gui.LoginFrame.class.getDeclaredMethod("handleLogin");
                handle.setAccessible(true);
                try {
                    handle.invoke(loginFrame);
                    System.out.println("LoginFrame.handleLogin invoked successfully");
                } catch (java.lang.reflect.InvocationTargetException ite) {
                    System.err.println("LoginFrame.handleLogin threw:");
                    ite.getCause().printStackTrace();
                }
            } catch (Exception e) {
                System.err.println("GUI simulation failed:");
                e.printStackTrace();
            }
        } catch (Exception e) {
            System.err.println("Authentication failed:");
            e.printStackTrace();
        }
    }
}
