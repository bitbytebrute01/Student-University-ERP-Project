package com.university.erp.gui.legacy;

import com.university.models.Student;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class DigitalIDCardPanel extends JPanel {
    private Student student;

    public DigitalIDCardPanel(Student student) {
        this.student = student;
        setPreferredSize(new Dimension(400, 250));
        setBackground(new Color(245, 245, 245));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = 380;
        int height = 230;
        int x = 10;
        int y = 10;

        // Card Background
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Double(x, y, width, height, 20, 20));
        
        // Card Border
        g2.setColor(new Color(41, 128, 185));
        g2.setStroke(new BasicStroke(3));
        g2.draw(new RoundRectangle2D.Double(x, y, width, height, 20, 20));

        // Header
        g2.setColor(new Color(41, 128, 185));
        g2.fillRect(x, y, width, 50);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Arial", Font.BOLD, 18));
        g2.drawString("UNIVERSITY STUDENT ID", x + 20, y + 32);

        // Photo Placeholder
        g2.setColor(Color.LIGHT_GRAY);
        g2.fillRect(x + 20, y + 70, 80, 100);
        g2.setColor(Color.GRAY);
        g2.drawRect(x + 20, y + 70, 80, 100);
        g2.setFont(new Font("Arial", Font.PLAIN, 10));
        g2.drawString("PHOTO", x + 40, y + 125);

        // Student Info
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Arial", Font.BOLD, 16));
        g2.drawString(student.getName().toUpperCase(), x + 120, y + 90);
        
        g2.setFont(new Font("Arial", Font.PLAIN, 12));
        g2.setColor(Color.DARK_GRAY);
        g2.drawString("ID: " + student.getId(), x + 120, y + 115);
        g2.drawString("Dept: " + student.getDepartment(), x + 120, y + 135);
        g2.drawString("Valid Thru: 2027", x + 120, y + 155);

        // QR Code Placeholder
        g2.setColor(Color.BLACK);
        g2.fillRect(width - 80, height - 80, 60, 60);
        g2.setColor(Color.WHITE);
        for(int i=0; i<60; i+=10) {
            for(int j=0; j<60; j+=10) {
                if((i+j)%20 == 0) g2.fillRect(width-80+i, height-80+j, 5, 5);
            }
        }
        
        // Footer
        g2.setColor(new Color(127, 140, 141));
        g2.setFont(new Font("Arial", Font.ITALIC, 10));
        g2.drawString("Smart AI University Ecosystem", x + 20, y + height - 10);
    }
    
    public static void showIDCard(Student student) {
        JFrame frame = new JFrame("Digital Student ID");
        frame.getContentPane().add(new DigitalIDCardPanel(student));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}
