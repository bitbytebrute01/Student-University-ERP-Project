package com.university.interfaces;

public interface Payable {
    double calculateAmount();
    boolean processPayment(double amount);
    String getPaymentStatus();
}
