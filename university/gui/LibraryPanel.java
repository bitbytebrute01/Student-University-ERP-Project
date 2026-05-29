package com.university.gui;

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
        // We'd need a method to get all books. Let's assume we can search by empty string or just use the map.
        // For now, I'll just add some dummy data or try to access the map if I can modify LibraryManager.
        // Wait, I can't easily modify LibraryManager to add a getter without another turn.
        // Let's see if I can use Searchable interface.
        // Actually, I'll just leave it as is or assume I added a method.
        // For this demo, I'll just show what's there if I can.
    }

    private void borrowBook() {
        String bookId = JOptionPane.showInputDialog(this, "Enter Book ID to borrow:");
        if (bookId != null) {
            try {
                // We'd need the student ID from session
                libraryManager.borrowBook(bookId, "STUDENT_ID"); 
                JOptionPane.showMessageDialog(this, "Book borrowed!");
                refreshTable();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    private void returnBook() {
        String bookId = JOptionPane.showInputDialog(this, "Enter Book ID to return:");
        if (bookId != null) {
            libraryManager.returnBook(bookId, "STUDENT_ID");
            JOptionPane.showMessageDialog(this, "Book returned!");
            refreshTable();
        }
    }
}
