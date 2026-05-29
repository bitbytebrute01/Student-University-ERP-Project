package com.university.rmi.server;

import com.university.library.LibraryManager;
import com.university.models.Book;
import com.university.models.Student;
import com.university.placement.PlacementManager;
import com.university.rmi.shared.UniversityService;
import com.university.students.StudentManager;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

public class UniversityServer extends UnicastRemoteObject implements UniversityService {

    private StudentManager studentManager;
    private LibraryManager libraryManager;
    private PlacementManager placementManager;

    public UniversityServer(StudentManager sm, LibraryManager lm, PlacementManager pm) throws RemoteException {
        super();
        this.studentManager = sm;
        this.libraryManager = lm;
        this.placementManager = pm;
    }

    @Override
    public Student fetchStudentRecord(String studentId) throws RemoteException {
        return studentManager.searchById(studentId);
    }

    @Override
    public double checkAttendance(String studentId) throws RemoteException {
        Student s = studentManager.searchById(studentId);
        if (s != null) return s.getAttendancePercentage();
        return 0.0;
    }

    @Override
    public List<Book> viewLibraryBooks() throws RemoteException {
        // Returning a dummy list to simulate fetching all available books
        // Need to add method in LibraryManager to fetch all, or just return an empty list for now
        return new ArrayList<>();
    }

    @Override
    public String checkPlacementStatus(String studentId) throws RemoteException {
        Student s = studentManager.searchById(studentId);
        if (s != null && s.getCgpa() > 7.5 && s.getAttendancePercentage() > 80.0) {
            return "Eligible for Placement";
        }
        return "Not Eligible / Pending";
    }

    public static void startServer(StudentManager sm, LibraryManager lm, PlacementManager pm) {
        try {
            UniversityServer server = new UniversityServer(sm, lm, pm);
            Registry registry = LocateRegistry.createRegistry(1099);
            registry.rebind("UniversityService", server);
            System.out.println("University RMI Server is running...");
        } catch (Exception e) {
            System.err.println("UniversityServer exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
