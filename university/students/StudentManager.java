package com.university.students;

import com.university.exceptions.StudentNotFoundException;
import com.university.interfaces.Searchable;
import com.university.models.Student;
import com.university.utils.FileHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class StudentManager implements Searchable<Student> {
    private HashMap<String, Student> studentMap;
    private static final String FILE_PATH = "students.dat";

    public StudentManager() {
        loadStudents();
    }

    @SuppressWarnings("unchecked")
    private void loadStudents() {
        Object data = FileHandler.loadFromFile(FILE_PATH);
        if (data instanceof HashMap) {
            studentMap = (HashMap<String, Student>) data;
        } else {
            studentMap = new HashMap<>();
        }
    }

    public void saveStudents() {
        FileHandler.saveToFile(FILE_PATH, studentMap);
    }

    public void addStudent(Student student) {
        studentMap.put(student.getId(), student);
        saveStudents();
    }

    public void updateStudent(Student student) throws StudentNotFoundException {
        if (!studentMap.containsKey(student.getId())) {
            throw new StudentNotFoundException("Student with ID " + student.getId() + " not found.");
        }
        studentMap.put(student.getId(), student);
        saveStudents();
    }

    public void deleteStudent(String studentId) throws StudentNotFoundException {
        if (studentMap.remove(studentId) == null) {
            throw new StudentNotFoundException("Student with ID " + studentId + " not found.");
        }
        saveStudents();
    }

    @Override
    public Student searchById(String id) {
        return studentMap.get(id);
    }

    @Override
    public List<Student> searchByName(String name) {
        return studentMap.values().stream()
                .filter(s -> s.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    public void displayAllStudents() {
        for (Student s : studentMap.values()) {
            s.printDetails();
            System.out.println("-----------------");
        }
    }

    public List<Student> getStudentsSortedByCgpa() {
        List<Student> list = new ArrayList<>(studentMap.values());
        list.sort(Comparator.comparingDouble(Student::getCgpa).reversed());
        return list;
    }

    public HashMap<String, Student> getStudentMap() {
        return studentMap;
    }
}
