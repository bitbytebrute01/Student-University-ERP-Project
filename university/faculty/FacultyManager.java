package com.university.faculty;

import com.university.interfaces.Searchable;
import com.university.models.Faculty;
import com.university.utils.FileHandler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class FacultyManager implements Searchable<Faculty> {
    private HashMap<String, Faculty> facultyMap;
    private static final String FILE_PATH = "faculty.dat";

    public FacultyManager() {
        loadFaculty();
    }

    @SuppressWarnings("unchecked")
    private void loadFaculty() {
        Object data = FileHandler.loadFromFile(FILE_PATH);
        if (data instanceof HashMap) {
            facultyMap = (HashMap<String, Faculty>) data;
        } else {
            facultyMap = new HashMap<>();
        }
    }

    public void saveFaculty() {
        FileHandler.saveToFile(FILE_PATH, facultyMap);
    }

    public void addFaculty(Faculty faculty) {
        facultyMap.put(faculty.getId(), faculty);
        saveFaculty();
    }

    public void removeFaculty(String facultyId) {
        facultyMap.remove(facultyId);
        saveFaculty();
    }

    @Override
    public Faculty searchById(String id) {
        return facultyMap.get(id);
    }

    @Override
    public List<Faculty> searchByName(String name) {
        return facultyMap.values().stream()
                .filter(f -> f.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    public void displayAllFaculty() {
        for (Faculty f : facultyMap.values()) {
            f.printDetails();
            System.out.println("-----------------");
        }
    }

    public List<Faculty> getFacultySortedByExperience() {
        List<Faculty> list = new ArrayList<>(facultyMap.values());
        list.sort(Comparator.comparingInt(Faculty::getYearsOfExperience).reversed());
        return list;
    }
}
