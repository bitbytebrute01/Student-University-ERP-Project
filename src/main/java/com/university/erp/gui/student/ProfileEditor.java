package com.university.erp.gui.student;

import com.university.models.Student;
import com.university.students.StudentManager;
import com.university.erp.security.StudentContext;
import com.university.utils.MediaManager;
import com.university.erp.gui.theme.ThemeManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.util.function.Consumer;

public class ProfileEditor extends JPanel {
    private StudentManager studentManager = new StudentManager();
    private Student student;
    private Consumer<Student> onProfileUpdated = updated -> {};

    public ProfileEditor() {
        this(StudentContext.requireCurrentStudent());
    }

    public ProfileEditor(Student student) {
        this(student, updated -> {});
    }

    public ProfileEditor(Student student, Consumer<Student> onProfileUpdated) {
        this.student = student;
        this.onProfileUpdated = onProfileUpdated == null ? updated -> {} : onProfileUpdated;
        if (this.student == null) {
            throw new IllegalStateException("ProfileEditor requires a valid Student.");
        }

        setLayout(new MigLayout("ins 30, wrap 1, fillx", "[grow]", "[]20[]"));
        ThemeManager.stylePage(this);

        JLabel title = new JLabel("Edit Personal Profile");
        title.setFont(new Font("Inter", Font.BOLD, 24));
        add(title);

        JPanel card = ThemeManager.createGlassCard();
        card.setLayout(new MigLayout("ins 20, wrap 2, fillx", "[grow 30][grow 70]", "[]20[]20[]20[]"));

        JTextField nameField = new JTextField(student.getName());
        JTextField emailField = new JTextField(student.getEmail());
        JTextField phoneField = new JTextField(student.getPhone());
        JTextArea bioArea = new JTextArea(student.getBio(), 4, 20);
        bioArea.setLineWrap(true);
        bioArea.setWrapStyleWord(true);

        JLabel photoPreview = new JLabel(loadProfileIcon(88));
        photoPreview.setHorizontalAlignment(SwingConstants.CENTER);
        photoPreview.setBorder(BorderFactory.createLineBorder(ThemeManager.border()));
        photoPreview.setOpaque(true);
        photoPreview.setBackground(ThemeManager.surfaceAlt());
        JButton uploadPhotoBtn = new JButton("Update Profile Photo");
        
        card.add(new JLabel("Full Name:"));
        card.add(nameField, "growx");

        card.add(new JLabel("Email:"));
        card.add(emailField, "growx");

        card.add(new JLabel("Phone:"));
        card.add(phoneField, "growx");

        card.add(new JLabel("Bio:"));
        card.add(new JScrollPane(bioArea), "growx");

        card.add(new JLabel("Profile Identity:"));
        JPanel photoRow = new JPanel(new MigLayout("ins 0, gap 12", "[] []", "[]"));
        photoRow.setOpaque(false);
        photoRow.add(photoPreview, "w 88!, h 88!");
        photoRow.add(uploadPhotoBtn, "h 38!");
        card.add(photoRow, "growx");

        JButton saveBtn = new JButton("Save Changes");
        saveBtn.setBackground(ThemeManager.SUCCESS_GREEN);
        saveBtn.setForeground(Color.WHITE);
        
        saveBtn.addActionListener(e -> {
            try {
                student.setName(nameField.getText());
                student.setEmail(emailField.getText());
                student.setPhone(phoneField.getText());
                student.setBio(bioArea.getText());
                studentManager.updateStudent(student);
                this.onProfileUpdated.accept(student);
                JOptionPane.showMessageDialog(this, "Profile updated successfully!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Update Error: " + ex.getMessage());
            }
        });

        uploadPhotoBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Images (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    String path = MediaManager.saveImage(chooser.getSelectedFile(), "profiles");
                    student.setProfilePicturePath(path);
                    studentManager.updateStudent(student);
                    photoPreview.setIcon(loadProfileIcon(88));
                    this.onProfileUpdated.accept(student);
                    JOptionPane.showMessageDialog(this, "Profile photo updated successfully.");
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Upload Error: " + ex.getMessage());
                }
            }
        });

        add(card, "growx");
        add(saveBtn, "h 45!, w 200!");
    }

    private Icon loadProfileIcon(int size) {
        String path = student.getProfilePicturePath();
        if (path != null && !path.isBlank()) {
            ImageIcon image = new ImageIcon(path);
            if (image.getIconWidth() > 0 && image.getIconHeight() > 0) {
                Image scaled = image.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
                return new ImageIcon(scaled);
            }
        }
        return org.kordamp.ikonli.swing.FontIcon.of(
                org.kordamp.ikonli.materialdesign2.MaterialDesignA.ACCOUNT_CIRCLE,
                size,
                new Color(156, 166, 176)
        );
    }
}
