package com.artha.app.api.v1;

import com.artha.app.api.CurrentUserProvider;
import com.artha.app.api.dto.CategorizeRequest;
import com.artha.app.models.Transaction;
import com.artha.app.models.User;
import com.artha.app.services.TransactionService;
import com.artha.app.services.ai.AnomalyDetectionService;
import com.artha.app.services.ai.AutoCategorizationService;
import com.artha.app.services.ai.CashflowForecastService;
import com.artha.app.services.ai.InsightsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/ai")
@Tag(name = "AI", description = "Auto-categorization, anomalies, forecasts, insights")
public class AiApiController {

    private final AutoCategorizationService autoCategorization;
    private final AnomalyDetectionService anomalyDetection;
    private final CashflowForecastService cashflowForecast;
    private final InsightsService insights;
    private final TransactionService transactionService;
    private final RecurringTransactionServiceBridge recurringBridge;
    private final CurrentUserProvider currentUserProvider;

    public AiApiController(AutoCategorizationService autoCategorization,
                           AnomalyDetectionService anomalyDetection,
                           CashflowForecastService cashflowForecast,
                           InsightsService insights,
                           TransactionService transactionService,
                           RecurringTransactionServiceBridge recurringBridge,
                           CurrentUserProvider currentUserProvider) {
        this.autoCategorization = autoCategorization;
        this.anomalyDetection = anomalyDetection;
        this.cashflowForecast = cashflowForecast;
        this.insights = insights;
        this.transactionService = transactionService;
        this.recurringBridge = recurringBridge;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/categorize")
    @Operation(summary = "Suggest a category for a transaction description")
    public Map<String, Object> categorize(@Valid @RequestBody CategorizeRequest req) {
        AutoCategorizationService.Suggestion s = autoCategorization.suggest(req.description(), req.amount());
        if (s == null) {
            return Map.of("matched", false);
        }
        return Map.of(
                "matched", true,
                "categoryId", s.categoryId(),
                "categoryName", s.categoryName(),
                "confidence", s.confidence(),
                "provider", s.provider()
        );
    }

    @GetMapping("/anomalies")
    @Operation(summary = "List transactions whose amount is an outlier for their category (z-score ≥ 2)")
    public List<AnomalyDetectionService.Anomaly> anomalies() {
        List<Transaction> history = transactionService.findAll();
        return anomalyDetection.detect(history, LocalDate.now());
    }

    @GetMapping("/forecast")
    @Operation(summary = "Project the next N days of net cashflow (default 30)")
    public CashflowForecastService.Forecast forecast(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) BigDecimal currentBalance) {
        User user = currentUserProvider.requireCurrentUser();
        List<Transaction> history = transactionService.findAll();
        var rules = recurringBridge.listActive(user);
        return cashflowForecast.project(
                currentBalance != null ? currentBalance : BigDecimal.ZERO,
                history, rules, LocalDate.now(), Math.max(1, Math.min(days, 365)));
    }

    @GetMapping("/insights")
    @Operation(summary = "Personalized insights: monthly net, top category, anomalies, savings rate")
    public List<InsightsService.Insight> insights() {
        List<Transaction> history = transactionService.findAll();
        List<AnomalyDetectionService.Anomaly> anomalies = anomalyDetection.detect(history, LocalDate.now());
        return insights.generate(history, anomalies);
    }
}