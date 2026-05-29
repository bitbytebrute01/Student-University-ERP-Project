package com.university.courses;

import com.university.exceptions.CourseNotFoundException;
import com.university.exceptions.StudentNotFoundException;
import com.university.interfaces.Searchable;
import com.university.models.Course;
import com.university.models.Student;
import com.university.students.StudentManager;
import com.university.utils.FileHandler;

import java.util.*;
import java.util.stream.Collectors;

public class CourseManager implements Searchable<Course> {
    private HashMap<String, Course> courseMap;
    // Maps courseId to a list of enrolled student IDs
    private HashMap<String, ArrayList<String>> enrollmentMap; 
    
    private static final String COURSE_FILE = "courses.dat";
    private static final String ENROLLMENT_FILE = "enrollments.dat";

    public CourseManager() {
        loadCourses();
        loadEnrollments();
    }

    @SuppressWarnings("unchecked")
    private void loadCourses() {
        Object data = FileHandler.loadFromFile(COURSE_FILE);
        if (data instanceof HashMap) {
            courseMap = (HashMap<String, Course>) data;
        } else {
            courseMap = new HashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadEnrollments() {
        Object data = FileHandler.loadFromFile(ENROLLMENT_FILE);
        if (data instanceof HashMap) {
            enrollmentMap = (HashMap<String, ArrayList<String>>) data;
        } else {
            enrollmentMap = new HashMap<>();
        }
    }

    public void saveAll() {
        FileHandler.saveToFile(COURSE_FILE, courseMap);
        FileHandler.saveToFile(ENROLLMENT_FILE, enrollmentMap);
    }

    public void addCourse(Course course) {
        courseMap.put(course.getCourseId(), course);
        enrollmentMap.putIfAbsent(course.getCourseId(), new ArrayList<>());
        saveAll();
    }

    public void assignFacultyToCourse(String courseId, String facultyId) throws CourseNotFoundException {
        Course c = courseMap.get(courseId);
        if (c == null) throw new CourseNotFoundException("Course not found: " + courseId);
        c.setAssignedFacultyId(facultyId);
        saveAll();
    }

    public void enrollStudent(String courseId, String studentId) throws CourseNotFoundException {
        if (!courseMap.containsKey(courseId)) throw new CourseNotFoundException("Course not found");
        ArrayList<String> students = enrollmentMap.get(courseId);
        if (students == null) {
            students = new ArrayList<>();
            enrollmentMap.put(courseId, students);
        }
        if (!students.contains(studentId)) {
            students.add(studentId);
            saveAll();
        }
    }

    public void dropCourse(String courseId, String studentId) {
        ArrayList<String> students = enrollmentMap.get(courseId);
        if (students != null) {
            students.remove(studentId);
            saveAll();
        }
    }

    @Override
    public Course searchById(String id) {
        return courseMap.get(id);
    }

    @Override
    public List<Course> searchByName(String name) {
        return courseMap.values().stream()
                .filter(c -> c.getCourseName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    public TreeSet<Course> getCoursesAlphabetically() {
        return new TreeSet<>(courseMap.values());
    }

    public void displayAllCourses() {
        for (Course c : getCoursesAlphabetically()) {
            System.out.println("Course: " + c.getCourseName() + " (" + c.getCourseId() + "), Credits: " + c.getCredits() + ", Faculty: " + c.getAssignedFacultyId());
        }
    }
}
