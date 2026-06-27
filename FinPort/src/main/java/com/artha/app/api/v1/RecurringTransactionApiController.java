package com.artha.app.api.v1;

import com.artha.app.api.CurrentUserProvider;
import com.artha.app.api.dto.RecurringTransactionRequest;
import com.artha.app.api.dto.RecurringTransactionResponse;
import com.artha.app.models.Category;
import com.artha.app.models.RecurringTransaction;
import com.artha.app.models.Transaction;
import com.artha.app.models.User;
import com.artha.app.services.CategoryService;
import com.artha.app.services.RecurringTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recurring-transactions")
@Tag(name = "Recurring transactions", description = "Subscription-like rules that auto-post transactions on schedule")
public class RecurringTransactionApiController {

    private final RecurringTransactionService service;
    private final CategoryService categoryService;
    private final CurrentUserProvider currentUserProvider;

    public RecurringTransactionApiController(RecurringTransactionService service,
                                             CategoryService categoryService,
                                             CurrentUserProvider currentUserProvider) {
        this.service = service;
        this.categoryService = categoryService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @Operation(summary = "List the current user's recurring transaction rules")
    public List<RecurringTransactionResponse> list() {
        User user = currentUserProvider.requireCurrentUser();
        return service.findByUser(user).stream()
                .map(RecurringTransactionResponse::from)
                .toList();
    }

    @PostMapping
    @Operation(summary = "Create a recurring rule for the current user")
    public ResponseEntity<RecurringTransactionResponse> create(@Valid @RequestBody RecurringTransactionRequest req) {
        User user = currentUserProvider.requireCurrentUser();
        Category category = requireCategory(req.categoryId());
        RecurringTransaction saved = service.save(req.toEntity(user, category));
        return ResponseEntity.status(HttpStatus.CREATED).body(RecurringTransactionResponse.from(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a recurring rule owned by the current user")
    public RecurringTransactionResponse update(@PathVariable Long id,
                                               @Valid @RequestBody RecurringTransactionRequest req) {
        User user = currentUserProvider.requireCurrentUser();
        RecurringTransaction existing = service.getByIdAndUser(id, user);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurring rule not found");
        }
        Category category = requireCategory(req.categoryId());
        existing.setCategory(category);
        existing.setAmount(req.amount());
        existing.setDescription(req.description());
        existing.setTransactionType(req.transactionType());
        existing.setCadence(req.cadence());
        existing.setStartDate(req.startDate());
        existing.setEndDate(req.endDate());
        existing.setDayOfMonth(req.dayOfMonth());
        existing.setDayOfWeek(req.dayOfWeek());
        existing.setActive(req.active() == null ? existing.isActive() : req.active());
        return RecurringTransactionResponse.from(service.save(existing));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a recurring rule owned by the current user")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User user = currentUserProvider.requireCurrentUser();
        if (service.getByIdAndUser(id, user) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurring rule not found");
        }
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/run-now")
    @Operation(summary = "Force-create one occurrence today and update lastRunDate")
    public Map<String, Object> runNow(@PathVariable Long id) {
        User user = currentUserProvider.requireCurrentUser();
        RecurringTransaction rule = service.getByIdAndUser(id, user);
        if (rule == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurring rule not found");
        }
        Transaction tx = service.runNow(rule);
        return Map.of(
                "transactionId", tx.getId(),
                "postedDate", tx.getDate().toString(),
                "amount", tx.getAmount()
        );
    }

    @PostMapping("/process-due")
    @Operation(summary = "Manually trigger the scheduler sweep (usually runs daily at 00:05)")
    public Map<String, Object> processDue() {
        List<Transaction> generated = service.processDue(LocalDate.now());
        return Map.of(
                "generated", generated.size(),
                "asOf", LocalDate.now().toString()
        );
    }

    private Category requireCategory(Long id) {
        Category c = categoryService.getCategoryById(id);
        if (c == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found: " + id);
        }
        return c;
    }
}