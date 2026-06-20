package com.expensetracker.expensetracker.repository;

import com.expensetracker.expensetracker.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Role findByName(String role);
}
