package com.university.rmi.shared;

import com.university.models.Book;
import com.university.models.Student;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface UniversityService extends Remote {
    Student fetchStudentRecord(String studentId) throws RemoteException;
    double checkAttendance(String studentId) throws RemoteException;
    List<Book> viewLibraryBooks() throws RemoteException;
    String checkPlacementStatus(String studentId) throws RemoteException;
}
