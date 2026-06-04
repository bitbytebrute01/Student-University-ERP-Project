package com.university.gui;

import com.university.models.Student;
import com.university.students.StudentManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class AdminStudentPanel extends JPanel {
    private JTable studentTable;
    private DefaultTableModel tableModel;
    private StudentManager studentManager = new StudentManager(); // In a real app, this should be shared

    public AdminStudentPanel() {
        setLayout(new BorderLayout());

        // Title
        JLabel title = new JLabel("Manage Students", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        // Table
        String[] columnNames = {"ID", "Name", "Email", "Department", "Semester", "CGPA"};
        tableModel = new DefaultTableModel(columnNames, 0);
        studentTable = new JTable(tableModel);
        refreshTable();

        JScrollPane scrollPane = new JScrollPane(studentTable);
        add(scrollPane, BorderLayout.CENTER);

        // Buttons
        JPanel buttonPanel = new JPanel();
        JButton addButton = new JButton("Add Student");
        JButton deleteButton = new JButton("Delete Student");
        JButton refreshButton = new JButton("Refresh");

        buttonPanel.add(addButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);

        addButton.addActionListener(e -> showAddStudentDialog());
        deleteButton.addActionListener(e -> deleteSelectedStudent());
        refreshButton.addActionListener(e -> refreshTable());
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        List<Student> students = new java.util.ArrayList<>(studentManager.getStudentMap().values());
        for (Student s : students) {
            tableModel.addRow(new Object[]{s.getId(), s.getName(), s.getEmail(), s.getDepartment(), s.getSemester(), s.getCgpa()});
        }
    }

    private void showAddStudentDialog() {
        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField deptField = new JTextField();
        JTextField semField = new JTextField();

        Object[] message = {
            "ID:", idField,
            "Name:", nameField,
            "Department:", deptField,
            "Semester:", semField
        };

        int option = JOptionPane.showConfirmDialog(null, message, "Add Student", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                int sem = Integer.parseInt(semField.getText());
                Student s = new Student(idField.getText(), nameField.getText(), nameField.getText().toLowerCase() + "@univ.edu", "000", deptField.getText(), sem);
                studentManager.addStudent(s);
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error adding student: " + ex.getMessage());
            }
        }
    }

    private void deleteSelectedStudent() {
        int selectedRow = studentTable.getSelectedRow();
        if (selectedRow != -1) {
            String id = (String) tableModel.getValueAt(selectedRow, 0);
            try {
                studentManager.deleteStudent(id);
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error deleting student: " + ex.getMessage());
            }
        } else {
            JOptionPane.showMessageDialog(this, "Please select a student to delete.");
        }
    }
}
