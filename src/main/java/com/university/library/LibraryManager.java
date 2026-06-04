package com.university.library;

import com.university.db.DatabaseManager;
import com.university.exceptions.BookUnavailableException;
import com.university.interfaces.Searchable;
import com.university.models.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LibraryManager implements Searchable<Book> {

    public LibraryManager() {}

    public void addBook(Book book) {
        String sql = "INSERT INTO books (book_id, title, author, is_available) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, book.getBookId());
            pstmt.setString(2, book.getTitle());
            pstmt.setString(3, book.getAuthor());
            pstmt.setBoolean(4, book.isAvailable());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error adding book: " + e.getMessage());
        }
    }

    public void borrowBook(String bookId, String studentId) throws BookUnavailableException {
        Book b = searchById(bookId);
        if (b == null || !b.isAvailable()) {
            throw new BookUnavailableException("Book is unavailable or does not exist: " + bookId);
        }
        
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update availability
                String updateSql = "UPDATE books SET is_available = 0 WHERE book_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setString(1, bookId);
                updateStmt.executeUpdate();

                // Create issue record
                String issueSql = "INSERT INTO book_issues (issue_id, book_id, student_id) VALUES (?, ?, ?)";
                PreparedStatement issueStmt = conn.prepareStatement(issueSql);
                issueStmt.setString(1, UUID.randomUUID().toString());
                issueStmt.setString(2, bookId);
                issueStmt.setString(3, studentId);
                issueStmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error borrowing book: " + e.getMessage());
        }
    }

    public void returnBook(String bookId, String studentId) {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Update availability
                String updateSql = "UPDATE books SET is_available = 1 WHERE book_id = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                updateStmt.setString(1, bookId);
                updateStmt.executeUpdate();

                // Update issue record
                String issueSql = "UPDATE book_issues SET return_date = CURRENT_DATE WHERE book_id = ? AND student_id = ? AND return_date IS NULL";
                PreparedStatement issueStmt = conn.prepareStatement(issueSql);
                issueStmt.setString(1, bookId);
                issueStmt.setString(2, studentId);
                issueStmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("Error returning book: " + e.getMessage());
        }
    }

    @Override
    public Book searchById(String id) {
        String sql = "SELECT * FROM books WHERE book_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return mapResultSetToBook(rs);
        } catch (SQLException e) {
            System.err.println("Error searching book: " + e.getMessage());
        }
        return null;
    }

    @Override
    public List<Book> searchByName(String name) {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE title LIKE ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, "%" + name + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(mapResultSetToBook(rs));
        } catch (SQLException e) {
            System.err.println("Error searching books by name: " + e.getMessage());
        }
        return list;
    }

    public List<Book> getAllBooks() {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY title";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapResultSetToBook(rs));
        } catch (SQLException e) {
            System.err.println("Error loading books: " + e.getMessage());
        }
        return list;
    }

    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        Book b = new Book(
                rs.getString("book_id"),
                rs.getString("title"),
                rs.getString("author")
        );
        b.setAvailable(rs.getBoolean("is_available"));
        return b;
    }
}
