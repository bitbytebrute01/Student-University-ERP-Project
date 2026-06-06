package com.university.test;

import com.university.attendance.AttendanceManager;
import com.university.courses.CourseManager;
import com.university.faculty.FacultyManager;
import com.university.lms.AssignmentManager;
import com.university.models.Assignment;
import com.university.models.Course;
import com.university.models.Student;
import com.university.students.StudentManager;
import com.university.erp.security.FacultyUser;
import com.university.erp.security.SessionManager;
import com.university.erp.security.StudentUser;
import com.university.sync.DBPoller;

public class TwoJvmSimulation {
    public static void main(String[] args) throws Exception {
        if (args == null || args.length == 0) {
            System.out.println("Usage: TwoJvmSimulation <student|faculty>");
            return;
        }
        String role = args[0].trim().toLowerCase();
        // initialize DB schema
        com.university.db.DatabaseManager.initializeSchema();

        if ("student".equals(role)) {
            runStudentWatcher();
        } else if ("faculty".equals(role)) {
            runFacultyActor();
        } else {
            System.out.println("Unknown role: " + role);
        }
    }

    private static void runStudentWatcher() throws Exception {
        System.out.println("[STUDENT] Starting watcher instance");
        // ensure student exists
        StudentManager sm = new StudentManager();
        Student s = sm.searchById("SIM-S001");
        if (s == null) {
            s = new Student("SIM-S001", "Sim Student", "sim@student.local", "999", "CS", 1);
            sm.addStudent(s);
        }
        // start session
        SessionManager.startSession(new StudentUser("sim-student", "", s.getId()));

        // subscribe to UI events
        com.university.erp.gui.UIEventBus.subscribe("ASSIGNMENT_PUBLISHED", payload -> System.out.println("[STUDENT] Event ASSIGNMENT_PUBLISHED received"));
        com.university.erp.gui.UIEventBus.subscribe("ASSIGNMENT_GRADED", payload -> System.out.println("[STUDENT] Event ASSIGNMENT_GRADED received"));
        com.university.erp.gui.UIEventBus.subscribe("ATTENDANCE_UPDATED", payload -> System.out.println("[STUDENT] Event ATTENDANCE_UPDATED received"));
        com.university.erp.gui.UIEventBus.subscribe("NOTIFICATION_CREATED", payload -> System.out.println("[STUDENT] Event NOTIFICATION_CREATED received"));

        // start DB poller
        DBPoller poller = new DBPoller(3);
        poller.start();

        System.out.println("[STUDENT] Poller started, waiting up to 20s for events...");
        // wait sufficient time
        Thread.sleep(20000);

        System.out.println("[STUDENT] Watcher exiting");
        poller.stop();
        SessionManager.endSession();
    }

    private static void runFacultyActor() throws Exception {
        System.out.println("[FACULTY] Starting actor instance");
        // create faculty and student/course if missing
        FacultyManager fm = new FacultyManager();
        StudentManager sm = new StudentManager();
        CourseManager cm = new CourseManager();
        AttendanceManager amgr = new AttendanceManager(sm);
        AssignmentManager asm = new AssignmentManager();

        if (fm.searchById("SIM-F001") == null) {
            fm.addFaculty(new com.university.models.Faculty("SIM-F001", "Sim Faculty", "sim@fac", "111", "CS", "Professor", 5));
        }
        Student s = sm.searchById("SIM-S001");
        if (s == null) {
            s = new Student("SIM-S001", "Sim Student", "sim@student.local", "999", "CS", 1);
            sm.addStudent(s);
        }
        if (cm.searchById("SIM-C101") == null) {
            Course c = new Course("SIM-C101", "Sim Course", 3);
            c.setAssignedFacultyId("SIM-F001");
            cm.addCourse(c);
        }
        // enroll
        try { cm.enrollStudent("SIM-C101", "SIM-S001"); } catch (Exception ignored) {}

        // start session as faculty
        SessionManager.startSession(new FacultyUser("sim-faculty", "", "SIM-F001"));

        // small wait to let watcher start
        System.out.println("[FACULTY] Sleeping 3s before creating assignment and marking attendance");
        Thread.sleep(3000);

        // create assignment
        Assignment a = new Assignment("SIM-A-1", "SIM-C101", "Sim Assignment", "Please submit.", new java.util.Date(System.currentTimeMillis() + 3600_000), 100);
        a.setCreatedBy("sim-faculty");
        a.setStatus("Published");
        asm.createAssignment(a);
        System.out.println("[FACULTY] Created assignment SIM-A-1");

        // mark attendance
        amgr.markAttendance("SIM-S001", "SIM-C101", true);
        System.out.println("[FACULTY] Marked attendance for SIM-S001 in SIM-C101");

        System.out.println("[FACULTY] Actor finished actions and exiting");
        SessionManager.endSession();
    }
}
