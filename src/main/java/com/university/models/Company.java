package com.university.models;

import com.university.interfaces.Storable;

public class Company implements Storable {
    private String companyId;
    private String name;
    private double requiredCgpa;
    private String jobRole;

    public Company(String companyId, String name, double requiredCgpa, String jobRole) {
        this.companyId = companyId;
        this.name = name;
        this.requiredCgpa = requiredCgpa;
        this.jobRole = jobRole;
    }

    public String getCompanyId() { return companyId; }
    public String getName() { return name; }
    public double getRequiredCgpa() { return requiredCgpa; }
    public String getJobRole() { return jobRole; }

    @Override
    public String getStorageKey() {
        return companyId;
    }
}
