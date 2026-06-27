package com.artha.app.services.ai;

import com.artha.app.models.Transaction;
import com.artha.app.services.RecurringTransactionService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Projects the next N days of net cashflow using a simple combination of:
 *  - average daily net of the last 90 days
 *  - upcoming recurring transactions
 *
 * Deliberately naive — better-than-nothing forecast, not a substitute for
 * proper time-series modelling. Documented as "estimate" everywhere it's shown.
 */
@Service
public class CashflowForecastService {

    /** Lookback window for the rolling average. */
    private static final int HISTORY_DAYS = 90;

    public record ForecastPoint(LocalDate date, BigDecimal projectedBalance) {}

    public record Forecast(
            BigDecimal currentMonthlyIncomeAvg,
            BigDecimal currentMonthlyExpenseAvg,
            BigDecimal projected30DayNet,
            BigDecimal projectedMonthEndBalance,
            List<ForecastPoint> dailySeries
    ) {}

    public Forecast project(BigDecimal currentBalance,
                            List<Transaction> history,
                            List<com.artha.app.models.RecurringTransaction> activeRules,
                            LocalDate today,
                            int days) {
        LocalDate from = today.minusDays(HISTORY_DAYS);
        double totalIncome = 0, totalExpense = 0;
        int incomeCount = 0, expenseCount = 0;
        for (Transaction t : history) {
            if (t.getDate() == null || t.getDate().isBefore(from)) continue;
            if (t.getTransactionType() == Transaction.TransactionType.INCOME) {
                totalIncome += t.getAmount();
                incomeCount++;
            } else if (t.getTransactionType() == Transaction.TransactionType.EXPENSE) {
                totalExpense += t.getAmount();
                expenseCount++;
            }
        }
        // Daily averages
        double dailyIncome = incomeCount == 0 ? 0 : totalIncome / HISTORY_DAYS;
        double dailyExpense = expenseCount == 0 ? 0 : totalExpense / HISTORY_DAYS;
        BigDecimal monthlyIncomeAvg = BigDecimal.valueOf(dailyIncome * 30).setScale(2, RoundingMode.HALF_UP);
        BigDecimal monthlyExpenseAvg = BigDecimal.valueOf(dailyExpense * 30).setScale(2, RoundingMode.HALF_UP);

        BigDecimal running = currentBalance == null ? BigDecimal.ZERO : currentBalance;
        List<ForecastPoint> series = new ArrayList<>();
        for (int i = 1; i <= days; i++) {
            LocalDate d = today.plusDays(i);
            // Add expected recurring transactions on this date
            double recurringNet = 0;
            if (activeRules != null) {
                for (var rule : activeRules) {
                    if (occursOn(rule, d)) {
                        recurringNet += rule.getTransactionType() == Transaction.TransactionType.INCOME
                                ? rule.getAmount().doubleValue()
                                : -rule.getAmount().doubleValue();
                    }
                }
            }
            double dayDelta = (dailyIncome - dailyExpense) + recurringNet;
            running = running.add(BigDecimal.valueOf(dayDelta));
            series.add(new ForecastPoint(d, running.setScale(2, RoundingMode.HALF_UP)));
        }

        BigDecimal net30 = series.stream()
                .filter(p -> !p.date().isAfter(today.plusDays(30)))
                .reduce((a, b) -> b)
                .map(p -> p.projectedBalance().subtract(currentBalance == null ? BigDecimal.ZERO : currentBalance))
                .orElse(BigDecimal.ZERO);

        return new Forecast(monthlyIncomeAvg, monthlyExpenseAvg, net30, running, series);
    }

    /**
     * Simple occurrence check: same day-of-week for WEEKLY, day-of-month for MONTHLY, etc.
     * Good enough for projection purposes.
     */
    private boolean occursOn(com.artha.app.models.RecurringTransaction rule, LocalDate date) {
        if (!rule.isActive()) return false;
        if (rule.getStartDate() != null && date.isBefore(rule.getStartDate())) return false;
        if (rule.getEndDate() != null && date.isAfter(rule.getEndDate())) return false;
        return switch (rule.getCadence()) {
            case DAILY -> true;
            case WEEKLY -> date.getDayOfWeek() == rule.effectiveDayOfWeek();
            case MONTHLY -> Math.min(rule.getDayOfMonth() != null ? rule.getDayOfMonth() : date.getDayOfMonth(),
                    date.lengthOfMonth()) == date.getDayOfMonth();
            case YEARLY -> date.getDayOfYear() == rule.getStartDate().getDayOfYear();
        };
    }
}