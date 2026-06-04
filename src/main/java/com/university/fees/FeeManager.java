package com.university.fees;

import com.university.exceptions.FeePendingException;
import com.university.interfaces.Payable;
import com.university.interfaces.ReceiptGenerator;
import com.university.models.Student;
import com.university.students.StudentManager;

import java.util.UUID;

public class FeeManager implements Payable, ReceiptGenerator {
    private StudentManager studentManager;
    private double baseFee = 50000.0;

    public FeeManager(StudentManager studentManager) {
        this.studentManager = studentManager;
    }

    @Override
    public double calculateAmount() {
        return baseFee;
    }

    @Override
    public boolean processPayment(double amount) {
        return amount >= baseFee;
    }

    @Override
    public String getPaymentStatus() {
        return "Processed";
    }

    public void payFee(String studentId, double amount) throws Exception {
        Student s = studentManager.searchById(studentId);
        if (s == null) throw new Exception("Student not found");
        
        if (processPayment(amount)) {
            s.setFeeStatus("Paid");
            studentManager.updateStudent(s);
            System.out.println(generateReceipt(UUID.randomUUID().toString(), studentId, amount));
        } else {
            throw new FeePendingException("Insufficient amount paid.");
        }
    }

    @Override
    public String generateReceipt(String transactionId, String studentId, double amount) {
        return "--- FEE RECEIPT ---\nTxn ID: " + transactionId + "\nStudent: " + studentId + "\nAmount: " + amount + "\nStatus: Paid";
    }
}
