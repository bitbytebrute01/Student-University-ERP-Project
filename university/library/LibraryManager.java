package com.university.library;

import com.university.exceptions.BookUnavailableException;
import com.university.interfaces.Searchable;
import com.university.models.Book;
import com.university.utils.FileHandler;

import java.util.*;
import java.util.stream.Collectors;

public class LibraryManager implements Searchable<Book> {
    private HashMap<String, Book> bookMap;
    private LinkedList<IssueRecord> issueRecords;

    private static final String BOOKS_FILE = "books.dat";
    private static final String ISSUES_FILE = "issues.dat";

    public LibraryManager() {
        loadLibrary();
    }

    @SuppressWarnings("unchecked")
    private void loadLibrary() {
        Object booksData = FileHandler.loadFromFile(BOOKS_FILE);
        if (booksData instanceof HashMap) {
            bookMap = (HashMap<String, Book>) booksData;
        } else {
            bookMap = new HashMap<>();
        }

        Object issuesData = FileHandler.loadFromFile(ISSUES_FILE);
        if (issuesData instanceof LinkedList) {
            issueRecords = (LinkedList<IssueRecord>) issuesData;
        } else {
            issueRecords = new LinkedList<>();
        }
    }

    private void saveLibrary() {
        FileHandler.saveToFile(BOOKS_FILE, bookMap);
        FileHandler.saveToFile(ISSUES_FILE, issueRecords);
    }

    public void addBook(Book book) {
        bookMap.put(book.getBookId(), book);
        saveLibrary();
    }

    public void borrowBook(String bookId, String studentId) throws BookUnavailableException {
        Book b = bookMap.get(bookId);
        if (b == null || !b.isAvailable()) {
            throw new BookUnavailableException("Book is unavailable or does not exist: " + bookId);
        }
        b.setAvailable(false);
        IssueRecord record = new IssueRecord(UUID.randomUUID().toString(), bookId, studentId);
        issueRecords.add(record);
        saveLibrary();
    }

    public void returnBook(String bookId, String studentId) {
        Book b = bookMap.get(bookId);
        if (b != null) {
            b.setAvailable(true);
            for (IssueRecord record : issueRecords) {
                if (record.getBookId().equals(bookId) && record.getStudentId().equals(studentId) && record.getReturnDate() == null) {
                    record.setReturnDate(new Date());
                    break;
                }
            }
            saveLibrary();
        }
    }

    @Override
    public Book searchById(String id) {
        return bookMap.get(id);
    }

    @Override
    public List<Book> searchByName(String name) {
        return bookMap.values().stream()
                .filter(b -> b.getTitle().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }
}
