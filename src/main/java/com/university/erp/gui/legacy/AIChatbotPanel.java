package com.university.erp.gui.legacy;

import com.university.ai.AIRecommendationEngine;
import com.university.erp.security.SessionManager;
import com.university.erp.security.StudentContext;
import com.university.erp.security.User;
import com.university.models.Student;
import com.university.students.StudentManager;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.List;

public class AIChatbotPanel extends JPanel {
    private JTextArea chatArea;
    private JTextField inputField;
    private StudentManager studentManager = new StudentManager();
    private AIRecommendationEngine aiEngine = new AIRecommendationEngine();
    private Student student;

    public AIChatbotPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(236, 240, 241));

        User user = SessionManager.getCurrentUser();
        student = findStudentForUser(user);

        JLabel title = new JLabel("University AI Assistant", JLabel.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));
        title.setForeground(new Color(41, 128, 185));
        title.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        add(title, BorderLayout.NORTH);

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        chatArea.setFont(new Font("SansSerif", Font.PLAIN, 14));
        chatArea.setBackground(Color.WHITE);
        chatArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        JButton sendBtn = new JButton("Ask AI");
        
        sendBtn.addActionListener(this::handleChat);
        inputField.addActionListener(this::handleChat);

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendBtn, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        chatArea.append("AI: Hello " + (student != null ? student.getName() : "there") + "! I am your University Academic Copilot. How can I help you today?\n");
        chatArea.append("Try asking: 'What is my CGPA?', 'Am I eligible for placement?', or 'Give me career advice'.\n");
    }

    private void handleChat(ActionEvent e) {
        String query = inputField.getText().toLowerCase();
        if (query.isEmpty()) return;

        chatArea.append("You: " + query + "\n");
        inputField.setText("");

        String response = getAIResponse(query);
        chatArea.append("AI: " + response + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private String getAIResponse(String query) {
        if (student == null) return "I can only help students with their academic data.";

        if (query.contains("cgpa") || query.contains("marks")) {
            return "Your current CGPA is " + String.format("%.2f", student.getCgpa()) + ". Keep up the hard work!";
        } else if (query.contains("attendance")) {
            return "Your attendance is at " + student.getAttendancePercentage() + "%. " + (student.getAttendancePercentage() < 75 ? "Warning: You should attend more classes!" : "Great job!");
        } else if (query.contains("placement") || query.contains("eligible")) {
            boolean eligible = aiEngine.predictPlacement(student);
            double readiness = aiEngine.calculatePlacementReadiness(student);
            return (eligible ? "Yes, you are eligible!" : "Not yet eligible.") + " Your readiness score is " + readiness + "%.";
        } else if (query.contains("career") || query.contains("job") || query.contains("advice")) {
            return "Based on your skills, I recommend a career as a " + aiEngine.getCareerRecommendation(student) + ".";
        } else if (query.contains("skill gap") || query.contains("gap")) {
            List<String> gaps = aiEngine.getSkillGapAnalysis(student);
            return "Skill Gap Analysis: You are missing " + String.join(", ", gaps) + ". I suggest focusing on these for better placements.";
        } else if (query.contains("risk") || query.contains("performance")) {
            return "Academic Status: " + aiEngine.getAcademicPerformanceRisk(student);
        } else if (query.contains("study plan") || query.contains("plan") || query.contains("improve")) {
            return "AI Study Recommendation: " + aiEngine.getPersonalizedStudyPlan(student);
        } else if (query.contains("hello") || query.contains("hi")) {
            return "Hello! I am your AI Mentor. I can analyze your skill gaps, predict academic risk, or suggest a study plan. How can I help?";
        }
        
        return "I'm sorry, I don't understand that yet. You can ask about your CGPA, attendance, placement, or career advice.";
    }

    private Student findStudentForUser(User user) {
        if (user == null) return null;
        try {
            return StudentContext.requireStudentForUser(user, studentManager);
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
