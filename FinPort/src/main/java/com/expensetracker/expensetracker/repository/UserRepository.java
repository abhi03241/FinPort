package com.expensetracker.expensetracker.repository;


import com.expensetracker.expensetracker.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Integer> {
    User findByUserName(String username);
}
