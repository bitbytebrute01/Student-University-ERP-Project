package com.university.interfaces;

public interface ReceiptGenerator {
    String generateReceipt(String transactionId, String studentId, double amount);
}
