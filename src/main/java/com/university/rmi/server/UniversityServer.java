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
        return libraryManager.getAllBooks();
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
            try {
                Registry registry = LocateRegistry.createRegistry(1099);
                registry.rebind("UniversityService", server);
                System.out.println("University RMI Server is running (created registry on 1099)...");
            } catch (java.rmi.server.ExportException ee) {
                // Port already in use: try to use existing registry instead of failing
                System.out.println("RMI registry on port 1099 already exists — attaching to existing registry.");
                Registry registry = LocateRegistry.getRegistry(1099);
                try {
                    registry.rebind("UniversityService", server);
                    System.out.println("University RMI Server bound to existing registry.");
                } catch (Exception rebindEx) {
                    System.err.println("Failed to bind UniversityService to existing registry: " + rebindEx);
                    rebindEx.printStackTrace();
                    throw rebindEx;
                }
            }
        } catch (Exception e) {
            System.err.println("UniversityServer exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
