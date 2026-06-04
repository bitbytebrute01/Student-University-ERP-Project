package com.university.rmi.client;

import com.university.models.Student;
import com.university.rmi.shared.UniversityService;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class StudentClient {
    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);
            UniversityService stub = (UniversityService) registry.lookup("UniversityService");

            System.out.println("Connected to University Server via RMI.");
            
            // Example usage:
            // Student s = stub.fetchStudentRecord("S101");
            // System.out.println(s != null ? s.getName() : "Not found");

        } catch (Exception e) {
            System.err.println("StudentClient exception: " + e.toString());
            e.printStackTrace();
        }
    }
}
