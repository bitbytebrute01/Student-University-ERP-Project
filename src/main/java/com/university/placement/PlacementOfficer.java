package com.university.placement;

import com.university.core.Person;

public class PlacementOfficer extends Person {
    public PlacementOfficer(String id, String name, String email, String phone) {
        super(id, name, email, phone);
    }

    @Override
    public void displayProfile() {
        System.out.println("Placement Officer: " + name);
    }

    @Override
    public String generateReport() {
        return "Placement Officer Report: " + name;
    }

    @Override
    public void exportReport(String filePath) {
    }
}
