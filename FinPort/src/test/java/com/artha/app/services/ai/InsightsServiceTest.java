package com.artha.app.services.ai;

import com.artha.app.models.Category;
import com.artha.app.models.Transaction;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InsightsServiceTest {

    private final InsightsService service = new InsightsService(null);

    @Test
    void emptyHistoryReturnsWelcomeInsight() {
        List<InsightsService.Insight> out = service.generate(List.of(), List.of());
        assertThat(out).hasSize(1);
        assertThat(out.get(0).kind()).isEqualTo("welcome");
    }

    @Test
    void includesMonthlyNetAndTopCategory() {
        Category food = new Category("Food"); food.setId(1L);
        Category rent = new Category("Rent"); rent.setId(2L);
        LocalDate thisMonth = LocalDate.now().withDayOfMonth(5);

        List<Transaction> txs = List.of(
                txWith(Transaction.TransactionType.INCOME, 5000, thisMonth, rent),
                txWith(Transaction.TransactionType.EXPENSE, 2000, thisMonth, food),
                txWith(Transaction.TransactionType.EXPENSE, 1500, thisMonth, rent)
        );

        List<InsightsService.Insight> out = service.generate(txs, List.of());

        assertThat(out).extracting(InsightsService.Insight::kind)
                .contains("monthly_net", "top_category");
        InsightsService.Insight top = out.stream()
                .filter(i -> "top_category".equals(i.kind()))
                .findFirst().orElseThrow();
        assertThat(top.message()).contains("Food");
    }

    @Test
    void flagsAnomalies() {
        Category food = new Category("Food"); food.setId(1L);
        AnomalyDetectionService.Anomaly anomaly = new AnomalyDetectionService.Anomaly(
                42L, "Huge dinner", LocalDate.now(), java.math.BigDecimal.valueOf(2000),
                "Food", java.math.BigDecimal.valueOf(300), java.math.BigDecimal.valueOf(50),
                34.0, AnomalyDetectionService.Severity.HIGH);
        LocalDate today = LocalDate.now();

        List<InsightsService.Insight> out = service.generate(
                List.of(txWith(Transaction.TransactionType.EXPENSE, 300, today, food)),
                List.of(anomaly));

        assertThat(out).extracting(InsightsService.Insight::kind).contains("anomaly");
        InsightsService.Insight i = out.stream()
                .filter(x -> "anomaly".equals(x.kind())).findFirst().orElseThrow();
        assertThat(i.severity()).isEqualTo("HIGH");
        assertThat(i.message()).contains("Huge dinner");
    }

    private Transaction txWith(Transaction.TransactionType type, double amount,
                               LocalDate date, Category category) {
        Transaction t = new Transaction();
        t.setTransactionType(type);
        t.setAmount(amount);
        t.setDate(date);
        t.setCategory(category);
        t.setDescription("x");
        return t;
    }
}