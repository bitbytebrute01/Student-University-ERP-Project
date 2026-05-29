package com.university.placement;

import com.university.models.Company;
import com.university.models.Student;
import com.university.students.StudentManager;
import com.university.utils.FileHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlacementManager {
    private HashMap<String, Company> companyMap;
    private static final String PLACEMENT_FILE = "companies.dat";

    public PlacementManager() {
        loadCompanies();
    }

    @SuppressWarnings("unchecked")
    private void loadCompanies() {
        Object data = FileHandler.loadFromFile(PLACEMENT_FILE);
        if (data instanceof HashMap) {
            companyMap = (HashMap<String, Company>) data;
        } else {
            companyMap = new HashMap<>();
        }
    }

    private void saveCompanies() {
        FileHandler.saveToFile(PLACEMENT_FILE, companyMap);
    }

    public void registerCompany(Company company) {
        companyMap.put(company.getCompanyId(), company);
        saveCompanies();
    }

    public List<Student> getEligibleStudentsForCompany(String companyId, StudentManager studentManager) {
        Company c = companyMap.get(companyId);
        List<Student> eligible = new ArrayList<>();
        if (c != null) {
            for (Student s : studentManager.getStudentMap().values()) {
                if (s.getCgpa() >= c.getRequiredCgpa()) {
                    eligible.add(s);
                }
            }
        }
        return eligible;
    }

    public void printPlacementStatistics() {
        System.out.println("Placement Statistics: " + companyMap.size() + " companies registered.");
        for (Company c : companyMap.values()) {
            System.out.println(c.getName() + " - Role: " + c.getJobRole() + " - Min CGPA: " + c.getRequiredCgpa());
        }
    }
}
