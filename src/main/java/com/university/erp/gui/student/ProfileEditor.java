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
import java.io.IOException;
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

                // If the profile picture is a temp upload, finalize it first
                String currentPath = student.getProfilePicturePath();
                String finalizedPath = null;
                if (currentPath != null && MediaManager.isTempPath(currentPath)) {
                    try {
                        // finalize into profile-specific folder
                        finalizedPath = MediaManager.finalizeUpload(currentPath, "profiles/" + student.getId());
                        student.setProfilePicturePath(finalizedPath);
                    } catch (IOException io) {
                        JOptionPane.showMessageDialog(this, "Failed to finalize profile photo: " + io.getMessage());
                        return;
                    }
                }

                try {
                    studentManager.updateStudent(student);
                    this.onProfileUpdated.accept(student);
                    // update profile_updated_at for cross-process polling
                    try (java.sql.Connection _c = com.university.db.DatabaseManager.getConnection();
                         java.sql.PreparedStatement _p = _c.prepareStatement("UPDATE students SET profile_updated_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                        _p.setString(1, student.getId());
                        _p.executeUpdate();
                    } catch (java.sql.SQLException _e) {
                        // Non-fatal for UI; log and continue
                        System.err.println("Failed to update profile_updated_at: " + _e.getMessage());
                    }
                    // notify UI components about profile update
                    com.university.erp.gui.UIEventBus.publish("PROFILE_UPDATED", student);
                    JOptionPane.showMessageDialog(this, "Profile updated successfully!");
                } catch (Exception ex) {
                    // rollback finalized file if DB update failed
                    if (finalizedPath != null) {
                        MediaManager.deleteFile(finalizedPath);
                        student.setProfilePicturePath(null);
                    }
                    JOptionPane.showMessageDialog(this, "Update Error: " + ex.getMessage());
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Update Error: " + ex.getMessage());
            }
        });

        uploadPhotoBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileFilter(new FileNameExtensionFilter("Images (*.png, *.jpg, *.jpeg)", "png", "jpg", "jpeg"));
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    // save to temp; finalize happens on Save
                    String tempPath = MediaManager.saveTempImage(chooser.getSelectedFile());
                    student.setProfilePicturePath(tempPath);
                    photoPreview.setIcon(loadProfileIcon(88));
                    this.onProfileUpdated.accept(student);
                    // publish staged profile so other UI components (preview areas) update immediately
                    com.university.erp.gui.UIEventBus.publish("PROFILE_STAGED", student);
                    JOptionPane.showMessageDialog(this, "Profile photo staged. Click Save to persist.");
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
