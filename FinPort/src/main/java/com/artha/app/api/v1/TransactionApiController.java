package com.artha.app.api.v1;

import com.artha.app.api.dto.PageResponse;
import com.artha.app.api.dto.TransactionRequest;
import com.artha.app.api.dto.TransactionResponse;
import com.artha.app.models.Category;
import com.artha.app.models.Transaction;
import com.artha.app.services.CategoryService;
import com.artha.app.services.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Income and expense transactions")
public class TransactionApiController {

    private final TransactionService transactionService;
    private final CategoryService categoryService;

    public TransactionApiController(TransactionService transactionService,
                                    CategoryService categoryService) {
        this.transactionService = transactionService;
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "List transactions (paged, with optional filters)")
    public PageResponse<TransactionResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) BigDecimal amount,
            @RequestParam(required = false) String amountFilter,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {

        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Transaction> result = transactionService.findTransactions(
                description, amount, amountFilter, startDate, endDate, pageable);
        return PageResponse.from(result, TransactionResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single transaction by id")
    public ResponseEntity<TransactionResponse> get(@PathVariable Long id) {
        Transaction t = transactionService.getTransactionById(id);
        return t != null
                ? ResponseEntity.ok(TransactionResponse.from(t))
                : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Create a new transaction")
    public ResponseEntity<TransactionResponse> create(@Valid @RequestBody TransactionRequest req,
                                                      UriComponentsBuilder uriBuilder) {
        Category category = categoryService.getCategoryById(req.categoryId());
        if (category == null) {
            return ResponseEntity.badRequest().build();
        }
        Transaction t = new Transaction();
        t.setAmount(req.amount());
        t.setDate(req.date());
        t.setDescription(req.description());
        t.setTransactionType(req.transactionType());
        t.setCategory(category);
        transactionService.save(t);
        URI location = uriBuilder.path("/api/v1/transactions/{id}").buildAndExpand(t.getId()).toUri();
        return ResponseEntity.created(location).body(TransactionResponse.from(t));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing transaction")
    public ResponseEntity<TransactionResponse> update(@PathVariable Long id,
                                                      @Valid @RequestBody TransactionRequest req) {
        Transaction existing = transactionService.getTransactionById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        Category category = categoryService.getCategoryById(req.categoryId());
        if (category == null) {
            return ResponseEntity.badRequest().build();
        }
        existing.setAmount(req.amount());
        existing.setDate(req.date());
        existing.setDescription(req.description());
        existing.setTransactionType(req.transactionType());
        existing.setCategory(category);
        transactionService.save(existing);
        return ResponseEntity.ok(TransactionResponse.from(existing));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a transaction")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (transactionService.getTransactionById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        transactionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}