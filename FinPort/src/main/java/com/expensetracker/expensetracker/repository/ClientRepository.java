package com.expensetracker.expensetracker.repository;

import com.expensetracker.expensetracker.models.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Integer> {

}
