package com.artha.app.services.ai;

import com.artha.app.models.Transaction;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pure-statistics anomaly detection over a user's transaction history.
 * No LLM required — flags transactions whose amount is unusually large
 * (or small) for their category, measured in standard deviations from
 * the per-category mean.
 */
@Service
public class AnomalyDetectionService {

    /** Z-score threshold above which a transaction is considered anomalous. */
    private static final double Z_THRESHOLD = 2.0;

    /** Minimum sample size required before flagging anything for a category. */
    private static final int MIN_SAMPLES = 5;

    public record Anomaly(
            Long transactionId,
            String description,
            LocalDate date,
            BigDecimal amount,
            String categoryName,
            BigDecimal categoryMean,
            BigDecimal categoryStdDev,
            double zScore,
            Severity severity
    ) {}

    public enum Severity { LOW, MEDIUM, HIGH }

    public List<Anomaly> detect(List<Transaction> history, LocalDate asOf) {
        if (history == null || history.isEmpty()) return List.of();

        // Only look at expenses (income anomalies are usually a bonus, not an alert).
        List<Transaction> expenses = history.stream()
                .filter(t -> t.getTransactionType() == Transaction.TransactionType.EXPENSE)
                .toList();

        // Per-category stats
        Map<Long, DoubleSummaryStatistics> perCat = expenses.stream()
                .filter(t -> t.getCategory() != null)
                .collect(Collectors.groupingBy(
                        t -> t.getCategory().getId(),
                        Collectors.summarizingDouble(t -> t.getAmount())));

        Map<Long, Double> varianceByCat = new java.util.HashMap<>();
        for (var e : perCat.entrySet()) {
            Long catId = e.getKey();
            DoubleSummaryStatistics stats = e.getValue();
            if (stats.getCount() < MIN_SAMPLES) continue;
            double mean = stats.getAverage();
            double sumSq = expenses.stream()
                    .filter(t -> t.getCategory() != null && t.getCategory().getId().equals(catId))
                    .mapToDouble(t -> Math.pow(t.getAmount() - mean, 2))
                    .sum();
            double variance = sumSq / (stats.getCount() - 1);
            varianceByCat.put(catId, variance);
        }

        List<Anomaly> anomalies = new ArrayList<>();
        for (Transaction t : expenses) {
            if (t.getCategory() == null) continue;
            Long catId = t.getCategory().getId();
            DoubleSummaryStatistics stats = perCat.get(catId);
            if (stats == null || stats.getCount() < MIN_SAMPLES) continue;
            Double variance = varianceByCat.get(catId);
            if (variance == null) continue;

            double stdDev = Math.sqrt(variance);
            if (stdDev < 0.01) continue; // constant-spend category — nothing to flag

            double z = (t.getAmount() - stats.getAverage()) / stdDev;
            if (Math.abs(z) < Z_THRESHOLD) continue;

            Severity sev = Math.abs(z) >= 4.0 ? Severity.HIGH
                    : Math.abs(z) >= 3.0 ? Severity.MEDIUM
                    : Severity.LOW;

            anomalies.add(new Anomaly(
                    t.getId(),
                    t.getDescription(),
                    t.getDate(),
                    BigDecimal.valueOf(t.getAmount()).setScale(2, RoundingMode.HALF_UP),
                    t.getCategory().getName(),
                    BigDecimal.valueOf(stats.getAverage()).setScale(2, RoundingMode.HALF_UP),
                    BigDecimal.valueOf(stdDev).setScale(2, RoundingMode.HALF_UP),
                    round2(z),
                    sev));
        }

        anomalies.sort(Comparator.comparing(Anomaly::zScore).reversed());
        return anomalies;
    }

    private static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** Convenience: number of days between two dates (helper kept package-private). */
    static long daysBetween(LocalDate a, LocalDate b) {
        return ChronoUnit.DAYS.between(a, b);
    }
}