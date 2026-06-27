package com.artha.app.repository;

import com.artha.app.models.Holding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HoldingRepository extends JpaRepository<Holding, Long> {
    List<Holding> findByUserId(Integer userId);

    long countByUserId(Integer userId);
}