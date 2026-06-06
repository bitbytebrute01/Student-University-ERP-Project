package com.university.erp.gui.student;

import com.university.erp.dao.CertificationDAO;
import com.university.erp.security.StudentContext;
import com.university.models.Certification;
import com.university.models.Student;
import com.university.utils.MediaManager;
import com.university.erp.gui.theme.ThemeManager;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.sql.SQLException;
import java.util.Date;
import java.util.UUID;

public class CertificationHub extends JPanel {
    private CertificationDAO certificationDAO = new CertificationDAO();
    private JTable certTable;
    private DefaultTableModel tableModel;
    private String studentId;

    public CertificationHub() {
        this(StudentContext.requireCurrentStudent());
    }

    public CertificationHub(Student student) {
        if (student == null) {
            throw new IllegalStateException("CertificationHub requires a valid Student.");
        }
        studentId = student.getId();
        
        setLayout(new MigLayout("ins 30, wrap 1, fillx", "[grow]", "[]20[grow]20[]"));
        ThemeManager.stylePage(this);

        JLabel title = new JLabel("Digital Credentials & Certifications");
        title.setFont(new Font("Inter", Font.BOLD, 24));
        add(title);

        String[] cols = {"ID", "Certification Title", "Issuer", "Date"};
        tableModel = new DefaultTableModel(cols, 0);
        certTable = new JTable(tableModel);
        refreshTable();
        add(new JScrollPane(certTable), "grow");

        JButton addBtn = new JButton("Add Certification");
        addBtn.setBackground(ThemeManager.SUCCESS_GREEN);
        addBtn.setForeground(Color.WHITE);
        addBtn.addActionListener(e -> showAddDialog());
        add(addBtn, "h 40!");
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        try {
            for (Certification c : certificationDAO.getCertificationsByStudent(studentId)) {
                tableModel.addRow(new Object[]{c.getId(), c.getTitle(), c.getIssuer(), c.getCompletionDate()});
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void showAddDialog() {
        JTextField titleField = new JTextField();
        JTextField issuerField = new JTextField();
        JButton fileBtn = new JButton("Select Certificate (PDF/Image)");
        JLabel filePath = new JLabel("None");
        fileBtn.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                filePath.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });

        Object[] message = {
            "Certification Title:", titleField,
            "Issuer:", issuerField,
            "File:", fileBtn, filePath
        };

        int option = JOptionPane.showConfirmDialog(null, message, "Add Certification", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                Certification c = new Certification(UUID.randomUUID().toString().substring(0, 8), studentId, titleField.getText(), issuerField.getText(), new Date());
                if (!filePath.getText().equals("None")) {
                    c.setFilePath(MediaManager.saveTemp(new File(filePath.getText())));
                }
                certificationDAO.addCertification(c);
                JOptionPane.showMessageDialog(this, "Certification added!");
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }
}
