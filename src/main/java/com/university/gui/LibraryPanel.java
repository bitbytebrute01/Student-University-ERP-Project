package com.university.gui;

import com.university.erp.security.SessionManager;
import com.university.erp.security.User;
import com.university.erp.security.UserRole;
import com.university.library.LibraryManager;
import com.university.models.Book;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class LibraryPanel extends JPanel {
    private JTable bookTable;
    private DefaultTableModel tableModel;
    private LibraryManager libraryManager = new LibraryManager();

    public LibraryPanel() {
        setLayout(new BorderLayout());

        JLabel title = new JLabel("Digital Library", JLabel.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        String[] columnNames = {"Book ID", "Title", "Author", "Availability"};
        tableModel = new DefaultTableModel(columnNames, 0);
        bookTable = new JTable(tableModel);
        refreshTable();

        JScrollPane scrollPane = new JScrollPane(bookTable);
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        JButton borrowBtn = new JButton("Borrow Book");
        JButton returnBtn = new JButton("Return Book");
        JButton refreshBtn = new JButton("Refresh");

        buttonPanel.add(borrowBtn);
        buttonPanel.add(returnBtn);
        buttonPanel.add(refreshBtn);
        add(buttonPanel, BorderLayout.SOUTH);

        borrowBtn.addActionListener(e -> borrowBook());
        returnBtn.addActionListener(e -> returnBook());
        refreshBtn.addActionListener(e -> refreshTable());
    }

    private void refreshTable() {
        tableModel.setRowCount(0);
        for (Book book : libraryManager.getAllBooks()) {
            tableModel.addRow(new Object[]{
                    book.getBookId(),
                    book.getTitle(),
                    book.getAuthor(),
                    book.isAvailable() ? "Available" : "Borrowed"
            });
        }
    }

    private void borrowBook() {
        String bookId = JOptionPane.showInputDialog(this, "Enter Book ID to borrow:");
        String studentId = resolveStudentId();
        if (bookId != null && !bookId.isBlank() && studentId != null) {
            try {
                libraryManager.borrowBook(bookId.trim(), studentId);
                JOptionPane.showMessageDialog(this, "Book borrowed!");
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    private void returnBook() {
        String bookId = JOptionPane.showInputDialog(this, "Enter Book ID to return:");
        String studentId = resolveStudentId();
        if (bookId != null && !bookId.isBlank() && studentId != null) {
            libraryManager.returnBook(bookId.trim(), studentId);
            JOptionPane.showMessageDialog(this, "Book returned!");
            refreshTable();
        }
    }

    private String resolveStudentId() {
        User user = SessionManager.getCurrentUser();
        if (user != null && user.getRole() == UserRole.STUDENT && user.getRefId() != null && !user.getRefId().isBlank()) {
            return user.getRefId();
        }

        String studentId = JOptionPane.showInputDialog(this, "Enter Student ID:");
        if (studentId == null || studentId.isBlank()) {
            return null;
        }
        return studentId.trim();
    }
}
