package com.university.erp.security;

import com.university.models.Student;
import com.university.students.StudentManager;

public final class StudentContext {
    private StudentContext() {}

    public static Student requireCurrentStudent() {
        User user = SessionManager.getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("Student record not found for authenticated user: null");
        }
        return requireStudentForUser(user, new StudentManager());
    }

    public static Student requireStudentForUser(User user, StudentManager studentManager) {
        String username = user.getUsername();
        String studentId = resolveStudentId(user);
        Student student = studentManager.searchById(studentId);

        if (student == null) {
            throw new IllegalStateException(
                    "Student record not found for authenticated user: " + username
            );
        }

        return student;
    }

    private static String resolveStudentId(User user) {
        String refId = user.getRefId();
        if (refId != null && !refId.isBlank()) {
            return refId;
        }
        return user.getUsername();
    }
}
