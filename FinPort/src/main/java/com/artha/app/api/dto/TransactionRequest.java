package com.artha.app.api.dto;

import com.artha.app.models.Transaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record TransactionRequest(
        @NotNull(message = "amount is required")
        @DecimalMin(value = "0.01", message = "amount must be greater than zero")
        Double amount,

        @NotNull(message = "date is required")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate date,

        @NotBlank(message = "description is required")
        @Size(max = 255, message = "description must be 255 characters or fewer")
        String description,

        @NotNull(message = "transactionType is required")
        Transaction.TransactionType transactionType,

        @NotNull(message = "categoryId is required")
        Long categoryId
) {
}