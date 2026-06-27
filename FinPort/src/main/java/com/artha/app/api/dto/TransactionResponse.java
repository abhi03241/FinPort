package com.artha.app.api.dto;

import com.artha.app.models.Transaction;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        Double amount,
        @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
        String description,
        Transaction.TransactionType transactionType,
        CategoryResponse category
) {
    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(
                t.getId(),
                t.getAmount(),
                t.getDate(),
                t.getDescription(),
                t.getTransactionType(),
                CategoryResponse.from(t.getCategory())
        );
    }
}