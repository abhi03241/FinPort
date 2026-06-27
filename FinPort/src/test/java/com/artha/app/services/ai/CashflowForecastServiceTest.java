package com.artha.app.services.ai;

import com.artha.app.models.Cadence;
import com.artha.app.models.Category;
import com.artha.app.models.RecurringTransaction;
import com.artha.app.models.Transaction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CashflowForecastServiceTest {

    private final CashflowForecastService service = new CashflowForecastService();

    private Category cat() {
        Category c = new Category("Food");
        c.setId(1L);
        return c;
    }

    private Transaction tx(Transaction.TransactionType type, double amount, LocalDate date) {
        Transaction t = new Transaction();
        t.setCategory(cat());
        t.setAmount(amount);
        t.setDate(date);
        t.setTransactionType(type);
        t.setDescription("x");
        return t;
    }

    private RecurringTransaction rule(BigDecimal amount, Cadence cadence, LocalDate start, Integer dow) {
        RecurringTransaction r = new RecurringTransaction();
        r.setActive(true);
        r.setAmount(amount);
        r.setCadence(cadence);
        r.setStartDate(start);
        r.setDayOfMonth(dow);
        r.setDayOfWeek(dow);
        r.setTransactionType(Transaction.TransactionType.EXPENSE);
        r.setCategory(cat());
        return r;
    }

    @Test
    void projectsPositiveBalanceWhenIncomeExceedsExpense() {
        LocalDate today = LocalDate.of(2026, 6, 15);
        List<Transaction> history = new java.util.ArrayList<>();
        for (int i = 0; i < 90; i++) {
            history.add(tx(Transaction.TransactionType.INCOME, 1000, today.minusDays(i)));
        }
        for (int i = 0; i < 90; i++) {
            history.add(tx(Transaction.TransactionType.EXPENSE, 500, today.minusDays(i)));
        }
        var forecast = service.project(BigDecimal.valueOf(10000), history, List.of(), today, 30);

        assertThat(forecast.currentMonthlyIncomeAvg().doubleValue()).isEqualTo(30000.0);
        assertThat(forecast.currentMonthlyExpenseAvg().doubleValue()).isEqualTo(15000.0);
        assertThat(forecast.projected30DayNet().doubleValue()).isEqualTo(15000.0);
        assertThat(forecast.projectedMonthEndBalance().doubleValue()).isEqualTo(25000.0);
    }

    @Test
    void includesRecurringTransactionOnItsDay() {
        LocalDate today = LocalDate.of(2026, 6, 1);
        RecurringTransaction weekly = rule(new BigDecimal("100"), Cadence.WEEKLY, today,
                java.time.DayOfWeek.FRIDAY.getValue());

        var forecast = service.project(BigDecimal.ZERO, List.of(), List.of(weekly), today, 7);

        assertThat(forecast.dailySeries()).hasSize(7);
        assertThat(forecast.dailySeries().get(3).projectedBalance().doubleValue()).isEqualTo(-100.0);
        assertThat(forecast.dailySeries().get(6).projectedBalance().doubleValue()).isEqualTo(-100.0);
    }
}