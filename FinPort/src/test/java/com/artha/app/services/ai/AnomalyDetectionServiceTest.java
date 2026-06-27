package com.artha.app.services.ai;

import com.artha.app.models.Category;
import com.artha.app.models.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnomalyDetectionServiceTest {

    private AnomalyDetectionService service;

    @BeforeEach
    void setUp() {
        service = new AnomalyDetectionService();
    }

    private Category cat(Long id, String name) {
        Category c = new Category(name);
        c.setId(id);
        return c;
    }

    private Transaction tx(Category c, double amount, LocalDate date) {
        Transaction t = new Transaction();
        t.setCategory(c);
        t.setAmount(amount);
        t.setDate(date);
        t.setTransactionType(Transaction.TransactionType.EXPENSE);
        t.setDescription("auto");
        return t;
    }

    @Test
    void flagsOutlierAboveTwoStandardDeviations() {
        Category food = cat(1L, "Food");
        LocalDate base = LocalDate.of(2026, 6, 1);
        List<Transaction> txs = new java.util.ArrayList<>();
        for (int i = 0; i < 9; i++) txs.add(tx(food, 300, base.plusDays(i)));
        txs.add(tx(food, 1500, base.plusDays(10)));

        List<AnomalyDetectionService.Anomaly> anomalies = service.detect(txs, LocalDate.of(2026, 6, 30));

        assertThat(anomalies).hasSize(1);
        AnomalyDetectionService.Anomaly a = anomalies.get(0);
        assertThat(a.amount().doubleValue()).isEqualTo(1500.0);
        assertThat(a.categoryName()).isEqualTo("Food");
        assertThat(a.zScore()).isGreaterThan(2.0);
    }

    @Test
    void smallSampleSizeDoesNotFlag() {
        Category food = cat(1L, "Food");
        List<Transaction> txs = List.of(
                tx(food, 100, LocalDate.of(2026, 6, 1)),
                tx(food, 9999, LocalDate.of(2026, 6, 2))
        );

        assertThat(service.detect(txs, LocalDate.now())).isEmpty();
    }

    @Test
    void emptyHistoryReturnsEmpty() {
        assertThat(service.detect(List.of(), LocalDate.now())).isEmpty();
    }

    @Test
    void constantSpendCategoryDoesNotFlag() {
        Category rent = cat(1L, "Rent");
        List<Transaction> txs = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) txs.add(tx(rent, 1000, LocalDate.of(2026, 6, 1).plusDays(i)));
        assertThat(service.detect(txs, LocalDate.now())).isEmpty();
    }

    @Test
    void anomaliesAreSortedByAbsoluteZScoreDescending() {
        Category food = cat(1L, "Food");
        LocalDate base = LocalDate.of(2026, 6, 1);
        // 50 normal tx with very tight spread (100 / 105 alternating) keeps the
        // stddev small, so two clearly-different outliers both exceed z=2.
        List<Transaction> txs = new java.util.ArrayList<>();
        for (int i = 0; i < 50; i++) txs.add(tx(food, i % 2 == 0 ? 100 : 105, base.plusDays(i % 28)));
        txs.add(tx(food, 600,  base.plusDays(15)));
        txs.add(tx(food, 1500, base.plusDays(16)));

        List<AnomalyDetectionService.Anomaly> anomalies = service.detect(txs, LocalDate.of(2026, 6, 30));

        assertThat(anomalies).hasSize(2);
        assertThat(Math.abs(anomalies.get(0).zScore()))
                .isGreaterThanOrEqualTo(Math.abs(anomalies.get(1).zScore()));
        assertThat(anomalies.get(0).amount().doubleValue()).isEqualTo(1500.0);
    }
}