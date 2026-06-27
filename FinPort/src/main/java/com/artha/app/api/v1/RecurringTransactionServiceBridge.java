package com.artha.app.api.v1;

import com.artha.app.models.RecurringTransaction;
import com.artha.app.models.User;
import com.artha.app.services.RecurringTransactionService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Tiny bridge so the AI controller doesn't have to depend on
 * {@link RecurringTransactionService} directly. Mirrors the
 * {@link AssetServiceBridge} pattern.
 */
@Component
public class RecurringTransactionServiceBridge {

    private final RecurringTransactionService service;

    public RecurringTransactionServiceBridge(RecurringTransactionService service) {
        this.service = service;
    }

    public List<RecurringTransaction> listActive(User user) {
        return service.findByUser(user).stream().filter(RecurringTransaction::isActive).toList();
    }
}