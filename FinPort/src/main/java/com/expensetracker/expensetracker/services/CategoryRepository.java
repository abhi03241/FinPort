package com.expensetracker.expensetracker.services;

import com.expensetracker.expensetracker.models.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Query method for filtering categories by name with pagination
    Page<Category> findByNameContainingIgnoreCase(String name, Pageable pageable);

    // Retrieve all categories without any filter, using pagination
    Page<Category> findAll(Pageable pageable);
}
