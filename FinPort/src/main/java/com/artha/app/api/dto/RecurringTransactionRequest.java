package com.artha.app.api.dto;

import com.artha.app.models.Cadence;
import com.artha.app.models.RecurringTransaction;
import com.artha.app.models.Transaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionRequest(
        @NotNull(message = "categoryId is required")
        Long categoryId,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        BigDecimal amount,

        @NotBlank(message = "description is required")
        @Size(max = 255)
        String description,

        @NotNull(message = "transactionType is required")
        Transaction.TransactionType transactionType,

        @NotNull(message = "cadence is required")
        Cadence cadence,

        @NotNull(message = "startDate is required")
        LocalDate startDate,

        LocalDate endDate,

        @Min(1)
        Integer dayOfMonth,

        @Min(1)
        Integer dayOfWeek,

        Boolean active
) {
    public RecurringTransaction toEntity(com.artha.app.models.User user,
                                         com.artha.app.models.Category category) {
        RecurringTransaction r = new RecurringTransaction();
        r.setUser(user);
        r.setCategory(category);
        r.setAmount(amount);
        r.setDescription(description);
        r.setTransactionType(transactionType);
        r.setCadence(cadence);
        r.setStartDate(startDate);
        r.setEndDate(endDate);
        r.setDayOfMonth(dayOfMonth);
        r.setDayOfWeek(dayOfWeek);
        r.setActive(active == null ? true : active);
        return r;
    }
}