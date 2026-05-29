package com.university.main;

import com.university.ai.AIRecommendationEngine;
import com.university.attendance.AttendanceManager;
import com.university.authentication.*;
import com.university.courses.CourseManager;
import com.university.examination.ExamManager;
import com.university.faculty.FacultyManager;
import com.university.fees.FeeManager;
import com.university.hostel.HostelManager;
import com.university.library.LibraryManager;
import com.university.models.Course;
import com.university.models.Student;
import com.university.placement.PlacementManager;
import com.university.rmi.server.UniversityServer;
import com.university.students.StudentManager;
import com.university.threads.AttendanceThread;
import com.university.threads.FeeProcessingThread;
import com.university.threads.NotificationThread;

import java.util.Scanner;

import com.university.gui.LoginFrame;
import javax.swing.*;

public class UniversityERP {
    private static Scanner scanner = new Scanner(System.in);

    // Managers
    private static AuthenticationManager authManager = new AuthenticationManager();
    private static StudentManager studentManager = new StudentManager();
    private static FacultyManager facultyManager = new FacultyManager();
    private static CourseManager courseManager = new CourseManager();
    private static LibraryManager libraryManager = new LibraryManager();
    private static HostelManager hostelManager = new HostelManager();
    private static PlacementManager placementManager = new PlacementManager();
    
    private static AttendanceManager attendanceManager = new AttendanceManager(studentManager);
    private static ExamManager examManager = new ExamManager(studentManager);
    private static FeeManager feeManager = new FeeManager(studentManager);
    private static AIRecommendationEngine aiEngine = new AIRecommendationEngine();

    public static void main(String[] args) {
        System.out.println("Starting AI-Powered Distributed University ERP (GUI Mode)...");

        // Set Look and Feel
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, fall back to default
        }

        // Start background threads
        new AttendanceThread(studentManager).start();
        new FeeProcessingThread(studentManager).start();
        NotificationThread notifThread = new NotificationThread("Global");
        notifThread.setDaemon(true); // Make it daemon so it doesn't prevent JVM shutdown
        notifThread.start();

        // Start RMI Server in background
        new Thread(() -> UniversityServer.startServer(studentManager, libraryManager, placementManager)).start();

        // Seed data if empty
        seedData();

        // Launch GUI
        SwingUtilities.invokeLater(() -> {
            new LoginFrame(authManager).setVisible(true);
        });
    }

    private static void seedData() {
        if (studentManager.getStudentMap().isEmpty()) {
            Student s1 = new Student("S101", "Alice", "alice@univ.edu", "1234", "CS", 1);
            s1.setCgpa(8.5);
            s1.setAttendancePercentage(85.0);
            studentManager.addStudent(s1);
            authManager.registerUser(new StudentUser("alice", "pass"));
            
            Course c1 = new Course("C101", "Java Programming", 4);
            courseManager.addCourse(c1);
            try { courseManager.enrollStudent("C101", "S101"); } catch (Exception e){}
            
            System.out.println("Seeded initial data.");
        }
    }

    private static void showLoginMenu() {
        System.out.println("\n--- LOGIN ---");
        System.out.print("Username: ");
        String user = scanner.nextLine();
        System.out.print("Password: ");
        String pass = scanner.nextLine();

        try {
            User loggedInUser = authManager.authenticate(user, pass);
            SessionManager.startSession(loggedInUser);
            System.out.println("Login successful! Welcome " + loggedInUser.getUsername());
        } catch (Exception e) {
            System.out.println("Login failed: " + e.getMessage());
        }
    }

    private static void showAdminMenu() {
        System.out.println("\n--- ADMIN DASHBOARD ---");
        System.out.println("1. Add Student");
        System.out.println("2. View All Students");
        System.out.println("3. AI: Detect At-Risk Students");
        System.out.println("4. Logout");
        System.out.print("Choice: ");
        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                System.out.print("Enter ID: "); String id = scanner.nextLine();
                System.out.print("Enter Name: "); String name = scanner.nextLine();
                Student s = new Student(id, name, name+"@univ.edu", "000", "CS", 1);
                studentManager.addStudent(s);
                System.out.println("Student added.");
                break;
            case "2":
                studentManager.displayAllStudents();
                break;
            case "3":
                System.out.println("At-Risk Students:");
                for (Student r : aiEngine.detectRiskStudents(new java.util.ArrayList<>(studentManager.getStudentMap().values()))) {
                    System.out.println(r.getName());
                }
                break;
            case "4":
                SessionManager.endSession();
                break;
            default:
                System.out.println("Invalid choice.");
        }
    }

    private static void showFacultyMenu() {
        System.out.println("\n--- FACULTY DASHBOARD ---");
        System.out.println("1. Logout");
        System.out.print("Choice: ");
        String choice = scanner.nextLine();
        if ("1".equals(choice)) SessionManager.endSession();
    }

    private static void showStudentMenu() {
        System.out.println("\n--- STUDENT DASHBOARD ---");
        System.out.println("1. View Profile");
        System.out.println("2. Logout");
        System.out.print("Choice: ");
        String choice = scanner.nextLine();
        if ("1".equals(choice)) System.out.println("Profile logic here...");
        else if ("2".equals(choice)) SessionManager.endSession();
    }
}
