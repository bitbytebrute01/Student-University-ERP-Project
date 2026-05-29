package com.university.attendance;

import com.university.exceptions.InvalidAttendanceException;
import com.university.models.Student;
import com.university.students.StudentManager;
import com.university.utils.FileHandler;

import java.util.ArrayList;

public class AttendanceManager {
    private ArrayList<AttendanceRecord> records;
    private StudentManager studentManager;
    private static final String ATTENDANCE_FILE = "attendance.dat";

    public AttendanceManager(StudentManager studentManager) {
        this.studentManager = studentManager;
        loadAttendance();
    }

    @SuppressWarnings("unchecked")
    private void loadAttendance() {
        Object data = FileHandler.loadFromFile(ATTENDANCE_FILE);
        if (data instanceof ArrayList) {
            records = (ArrayList<AttendanceRecord>) data;
        } else {
            records = new ArrayList<>();
        }
    }

    private void saveAttendance() {
        FileHandler.saveToFile(ATTENDANCE_FILE, records);
    }

    public void markAttendance(String studentId, String courseId, boolean isPresent) throws InvalidAttendanceException {
        if (studentManager.searchById(studentId) == null) {
            throw new InvalidAttendanceException("Cannot mark attendance for unknown student.");
        }
        records.add(new AttendanceRecord(studentId, courseId, isPresent));
        saveAttendance();
        updateStudentAttendancePercentage(studentId);
    }

    private void updateStudentAttendancePercentage(String studentId) {
        long total = records.stream().filter(r -> r.getStudentId().equals(studentId)).count();
        long present = records.stream().filter(r -> r.getStudentId().equals(studentId) && r.isPresent()).count();
        if (total > 0) {
            double percent = ((double) present / total) * 100;
            Student s = studentManager.searchById(studentId);
            if (s != null) {
                s.setAttendancePercentage(percent);
                try {
                    studentManager.updateStudent(s);
                } catch (Exception ignored) {}
            }
        }
    }
}
