package com.artha.app.services.ai;

import com.artha.app.models.Transaction;
import com.artha.app.services.CategoryService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates a small, human-readable list of insights about the user's finances.
 * Pure rules — no LLM required. Each insight is a {@code (kind, message, severity?)}
 * triple the UI can render with a small icon and one line of text.
 */
@Service
public class InsightsService {

    public record Insight(String kind, String message, String severity) {
        public Insight(String kind, String message) { this(kind, message, null); }
    }

    private final CategoryService categoryService;

    public InsightsService(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    public List<Insight> generate(List<Transaction> history, List<AnomalyDetectionService.Anomaly> anomalies) {
        List<Insight> insights = new ArrayList<>();
        if (history == null || history.isEmpty()) {
            insights.add(new Insight("welcome", "Add a few transactions to start seeing insights."));
            return insights;
        }

        // Insight 1: total spend this month
        LocalDate firstOfMonth = LocalDate.now().withDayOfMonth(1);
        double monthExpense = history.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.EXPENSE
                        && t.getDate() != null && !t.getDate().isBefore(firstOfMonth))
                .mapToDouble(Transaction::getAmount).sum();
        double monthIncome = history.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.INCOME
                        && t.getDate() != null && !t.getDate().isBefore(firstOfMonth))
                .mapToDouble(Transaction::getAmount).sum();
        insights.add(new Insight("monthly_net",
                "This month so far: spent " + money(monthExpense) + ", earned " + money(monthIncome)
                        + " (net " + money(monthIncome - monthExpense) + ")."));

        // Insight 2: top spending category
        Map<String, Double> byCat = history.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.EXPENSE && t.getCategory() != null
                        && t.getDate() != null && !t.getDate().isBefore(firstOfMonth))
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getName(),
                        Collectors.summingDouble(Transaction::getAmount)));
        byCat.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(e -> insights.add(new Insight("top_category",
                        "Your top spending category this month is " + e.getKey()
                                + " at " + money(e.getValue()) + ".")));

        // Insight 3: anomalies
        if (anomalies != null && !anomalies.isEmpty()) {
            AnomalyDetectionService.Anomaly top = anomalies.get(0);
            insights.add(new Insight("anomaly",
                    "Unusual spend detected: " + top.description() + " (" + money(top.amount().doubleValue())
                            + " in " + top.categoryName() + ", " + top.severity() + ")",
                    top.severity().name()));
        }

        // Insight 4: empty category warning
        long txCount = history.stream()
                .filter(t -> t.getCategory() == null).count();
        if (txCount > 0) {
            insights.add(new Insight("uncategorized",
                    txCount + " transaction" + (txCount == 1 ? "" : "s") + " have no category. "
                            + "Use the AI categorizer to clean them up."));
        }

        // Insight 5: encourage savings if income > expense
        if (monthIncome > 0 && monthExpense > 0) {
            double ratio = monthExpense / monthIncome;
            if (ratio < 0.5) {
                insights.add(new Insight("savings",
                        "Nice — you're spending under 50% of income this month. "
                                + "Consider boosting your savings goals."));
            } else if (ratio > 0.9) {
                insights.add(new Insight("warning",
                        "Heads up: expenses are over 90% of income this month.", "HIGH"));
            }
        }

        return insights;
    }

    private static String money(double v) {
        return BigDecimal.valueOf(v).setScale(0, RoundingMode.HALF_UP).toPlainString();
    }
}