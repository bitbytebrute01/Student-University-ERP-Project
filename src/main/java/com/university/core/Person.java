package com.university.core;

import com.university.interfaces.Printable;
import com.university.interfaces.ReportGenerator;
import com.university.interfaces.Storable;

public abstract class Person implements Printable, ReportGenerator, Storable {
    protected String id;
    protected String name;
    protected String email;
    protected String phone;

    public Person(String id, String name, String email, String phone) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String getStorageKey() {
        return id;
    }

    public abstract void displayProfile();

    @Override
    public void printDetails() {
        displayProfile();
    }
}
