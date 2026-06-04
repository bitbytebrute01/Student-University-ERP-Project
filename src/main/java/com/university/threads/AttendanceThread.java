package com.university.threads;

import com.university.models.Student;
import com.university.students.StudentManager;

public class AttendanceThread extends Thread {
    private StudentManager studentManager;

    public AttendanceThread(StudentManager studentManager) {
        this.studentManager = studentManager;
    }

    @Override
    public void run() {
        while (true) {
            try {
                System.out.println("[AttendanceThread] Checking for low attendance...");
                for (Student s : studentManager.getStudentMap().values()) {
                    if (s.getAttendancePercentage() > 0 && s.getAttendancePercentage() < 75.0) {
                        System.out.println("[WARNING] Low attendance for student: " + s.getName() + " (" + s.getAttendancePercentage() + "%)");
                    }
                }
                Thread.sleep(60000); // Check every minute
            } catch (InterruptedException e) {
                System.out.println("Attendance thread interrupted.");
                break;
            }
        }
    }
}
