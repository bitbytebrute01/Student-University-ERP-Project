package com.university.threads;

import com.university.models.Student;
import com.university.students.StudentManager;

public class FeeProcessingThread extends Thread {
    private StudentManager studentManager;

    public FeeProcessingThread(StudentManager studentManager) {
        this.studentManager = studentManager;
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("[FeeProcessingThread] Processing pending fees...");
                for (Student s : studentManager.getStudentMap().values()) {
                    if ("Pending".equalsIgnoreCase(s.getFeeStatus())) {
                        System.out.println("[REMINDER] Fee pending for student: " + s.getName());
                    }
                }
                Thread.sleep(120000); // Check every 2 minutes
            } catch (InterruptedException e) {
                System.out.println("Fee processing thread interrupted.");
                break;
            }
        }
    }
}
