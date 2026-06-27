package com.artha.app.services;

import com.artha.app.models.Category;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase
@ActiveProfiles("test")
class CategoryRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("findByNameContainingIgnoreCase is case-insensitive and partial")
    void findByNameContainingIgnoreCase_works() {
        entityManager.persistAndFlush(new Category("Food & Dining"));
        entityManager.persistAndFlush(new Category("Travel"));
        entityManager.persistAndFlush(new Category("FOOD groceries"));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Category> result = categoryRepository.findByNameContainingIgnoreCase("food", pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(Category::getName)
                .containsExactlyInAnyOrder("Food & Dining", "FOOD groceries");
    }

    @Test
    @DisplayName("findAll pageable returns all categories")
    void findAll_pageable() {
        entityManager.persistAndFlush(new Category("A"));
        entityManager.persistAndFlush(new Category("B"));
        entityManager.persistAndFlush(new Category("C"));

        Page<Category> result = categoryRepository.findAll(PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("category name must be unique")
    void uniqueNameConstraint() {
        entityManager.persistAndFlush(new Category("Food"));

        Exception ex = org.junit.jupiter.api.Assertions.assertThrows(
                Exception.class,
                () -> {
                    entityManager.persistAndFlush(new Category("Food"));
                    entityManager.flush();
                });

        // Spring wraps the underlying JPA / Hibernate exception; assert the message
        // mentions the uniqueness violation rather than coupling to a specific type.
        assertThat(ex.getMessage()).containsIgnoringCase("unique");
    }
}