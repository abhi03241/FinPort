package com.artha.app.repository;

import com.artha.app.models.RecurringTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RecurringTransactionRepository extends JpaRepository<RecurringTransaction, Long> {

    List<RecurringTransaction> findByUserId(Integer userId);

    /** All active rules; caller filters for "due" by comparing against today. */
    List<RecurringTransaction> findByActiveTrue();

    /** Active rules whose lastRunDate is on or before the given date — overdue. */
    List<RecurringTransaction> findByActiveTrueAndLastRunDateLessThanEqual(LocalDate cutoff);

    long countByUserId(Integer userId);
}