package com.university.gui;

import com.university.authentication.AuthenticationManager;
import com.university.authentication.SessionManager;
import com.university.authentication.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginFrame extends JFrame {
    private JTextField userField;
    private JPasswordField passField;
    private JButton loginButton;
    private AuthenticationManager authManager;

    public LoginFrame(AuthenticationManager authManager) {
        this.authManager = authManager;
        setTitle("University ERP - Login");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Title
        JLabel titleLabel = new JLabel("University ERP Login", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(titleLabel, gbc);

        // Username
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Username:"), gbc);

        userField = new JTextField(15);
        gbc.gridx = 1;
        add(userField, gbc);

        // Password
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(new JLabel("Password:"), gbc);

        passField = new JPasswordField(15);
        gbc.gridx = 1;
        add(passField, gbc);

        // Login Button
        loginButton = new JButton("Login");
        loginButton.setBackground(new Color(70, 130, 180));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        add(loginButton, gbc);

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleLogin();
            }
        });

        // Add Enter key listener
        ActionListener loginAction = e -> handleLogin();
        userField.addActionListener(loginAction);
        passField.addActionListener(loginAction);
    }

    private void handleLogin() {
        String username = userField.getText();
        String password = new String(passField.getPassword());

        try {
            User user = authManager.authenticate(username, password);
            SessionManager.startSession(user);
            JOptionPane.showMessageDialog(this, "Login Successful! Welcome " + user.getUsername());
            
            // Open Dashboard
            new MainDashboard().setVisible(true);
            this.dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Login Failed: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
