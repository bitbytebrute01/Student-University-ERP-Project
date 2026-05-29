package com.university.interfaces;

import java.util.List;

public interface Searchable<T> {
    T searchById(String id);
    List<T> searchByName(String name);
}
