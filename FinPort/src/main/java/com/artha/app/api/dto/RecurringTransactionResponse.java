package com.artha.app.api.dto;

import com.artha.app.models.Cadence;
import com.artha.app.models.RecurringTransaction;
import com.artha.app.models.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionResponse(
        Long id,
        Long categoryId,
        String categoryName,
        BigDecimal amount,
        String description,
        Transaction.TransactionType transactionType,
        Cadence cadence,
        LocalDate startDate,
        LocalDate endDate,
        Integer dayOfMonth,
        Integer dayOfWeek,
        LocalDate lastRunDate,
        boolean active
) {
    public static RecurringTransactionResponse from(RecurringTransaction r) {
        return new RecurringTransactionResponse(
                r.getId(),
                r.getCategory().getId(),
                r.getCategory().getName(),
                r.getAmount(),
                r.getDescription(),
                r.getTransactionType(),
                r.getCadence(),
                r.getStartDate(),
                r.getEndDate(),
                r.getDayOfMonth(),
                r.getDayOfWeek(),
                r.getLastRunDate(),
                r.isActive()
        );
    }
}