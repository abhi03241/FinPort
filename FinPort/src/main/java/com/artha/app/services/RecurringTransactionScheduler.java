package com.artha.app.services;

import com.artha.app.models.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
public class RecurringTransactionScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionScheduler.class);

    private final RecurringTransactionService service;

    public RecurringTransactionScheduler(RecurringTransactionService service) {
        this.service = service;
    }

    /**
     * Runs at 00:05 every day. Fires all active recurring rules whose
     * next occurrence falls on or before today.
     *
     * Cron format: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "${artha.recurring.cron:0 5 0 * * *}")
    public void runDaily() {
        LocalDate today = LocalDate.now();
        List<Transaction> generated = service.processDue(today);
        log.info("Recurring transaction sweep for {}: generated {} transactions", today, generated.size());
    }
}