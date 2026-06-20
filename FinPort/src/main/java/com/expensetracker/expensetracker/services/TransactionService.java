package com.expensetracker.expensetracker.services;

import com.expensetracker.expensetracker.models.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Page<Transaction> findTransactions(String description, BigDecimal amount, String amountFilter,
                                              LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return transactionRepository.findFilteredTransactions(description, amount, amountFilter, startDate, endDate, pageable);
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id).orElse(null);
    }

    // Fetch total expenses for month, week, year
    public Map<String, Double> getTotalExpenses() {
        Map<String, Double> totalExpenses = new HashMap<>();

        // Current date
        LocalDate currentDate = LocalDate.now();

        // Calculate total expenses for the current month
        LocalDate firstDayOfMonth = currentDate.with(TemporalAdjusters.firstDayOfMonth());
        double monthlyExpense = transactionRepository.sumExpensesBetweenDates(firstDayOfMonth, currentDate);
        totalExpenses.put("monthly", monthlyExpense);

        // Calculate total expenses for the current week (assuming week starts on Monday)
        LocalDate startOfWeek = currentDate.with(java.time.DayOfWeek.MONDAY);
        double weeklyExpense = transactionRepository.sumExpensesBetweenDates(startOfWeek, currentDate);
        totalExpenses.put("weekly", weeklyExpense);

        // Calculate total expenses for the current year
        LocalDate firstDayOfYear = currentDate.with(TemporalAdjusters.firstDayOfYear());
        double yearlyExpense = transactionRepository.sumExpensesBetweenDates(firstDayOfYear, currentDate);
        totalExpenses.put("yearly", yearlyExpense);

        return totalExpenses;
    }


    // Method to retrieve total incomes for the current month, week, and year
    public Map<String, Double> getTotalIncomes() {
        Map<String, Double> totalIncomes = new HashMap<>();

        // Get current date
        LocalDate currentDate = LocalDate.now();

        // Calculate total incomes for the current month
        LocalDate firstDayOfMonth = currentDate.with(TemporalAdjusters.firstDayOfMonth());
        double monthlyIncome = transactionRepository.sumIncomesBetweenDates(firstDayOfMonth, currentDate);
        totalIncomes.put("monthly", monthlyIncome);

        // Calculate total incomes for the current week (assuming week starts on Monday)
        LocalDate startOfWeek = currentDate.with(DayOfWeek.MONDAY);
        double weeklyIncome = transactionRepository.sumIncomesBetweenDates(startOfWeek, currentDate);
        totalIncomes.put("weekly", weeklyIncome);

        // Calculate total incomes for the current year
        LocalDate firstDayOfYear = currentDate.with(TemporalAdjusters.firstDayOfYear());
        double yearlyIncome = transactionRepository.sumIncomesBetweenDates(firstDayOfYear, currentDate);
        totalIncomes.put("yearly", yearlyIncome);

        return totalIncomes;
    }


    // Method to retrieve total incomes for the current month, week, and year
    public Map<String, Double> getTotalTransactions() {
        Map<String, Double> totalIncomes = new HashMap<>();

        // Get current date
        LocalDate currentDate = LocalDate.now();

        // Calculate total incomes for the current month
        LocalDate firstDayOfMonth = currentDate.with(TemporalAdjusters.firstDayOfMonth());
        double monthlyIncome = transactionRepository.countTransactionsBetweenDates(firstDayOfMonth, currentDate);
        totalIncomes.put("monthly", monthlyIncome);

        // Calculate total incomes for the current week (assuming week starts on Monday)
        LocalDate startOfWeek = currentDate.with(DayOfWeek.MONDAY);
        double weeklyIncome = transactionRepository.countTransactionsBetweenDates(startOfWeek, currentDate);
        totalIncomes.put("weekly", weeklyIncome);

        // Calculate total incomes for the current year
        LocalDate firstDayOfYear = currentDate.with(TemporalAdjusters.firstDayOfYear());
        double yearlyIncome = transactionRepository.countTransactionsBetweenDates(firstDayOfYear, currentDate);
        totalIncomes.put("yearly", yearlyIncome);

        return totalIncomes;
    }


    public Map<String, Double> getTotalIncomesByMonth() {
        Map<String, Double> totalIncomesByMonth = new HashMap<>();

        // Get the current year
        LocalDate currentDate = LocalDate.now();
        int currentYear = currentDate.getYear();

        // Loop through each month of the current year
        for (int month = 1; month <= 12; month++) {
            YearMonth yearMonth = YearMonth.of(currentYear, month);
            LocalDate firstDayOfMonth = yearMonth.atDay(1); // First day of the month
            LocalDate lastDayOfMonth = yearMonth.atEndOfMonth(); // Last day of the month

            // Calculate total incomes for the month
            Double totalIncome = transactionRepository.countIncomesBetweenDates(firstDayOfMonth, lastDayOfMonth);

            // Check for null value to avoid NullPointerException
            totalIncomesByMonth.put(yearMonth.getMonth().name(), totalIncome != null ? totalIncome : 0.0);
        }

        return totalIncomesByMonth;
    }

    public Map<String, Double> getTotalExpensesByMonth() {
        Map<String, Double> totalExpensesByMonth = new HashMap<>();

        // Get the current year
        LocalDate currentDate = LocalDate.now();
        int currentYear = currentDate.getYear();

        // Loop through each month of the current year
        for (int month = 1; month <= 12; month++) {
            YearMonth yearMonth = YearMonth.of(currentYear, month);
            LocalDate firstDayOfMonth = yearMonth.atDay(1); // First day of the month
            LocalDate lastDayOfMonth = yearMonth.atEndOfMonth(); // Last day of the month

            // Calculate total expenses for the month
            Double totalExpense = transactionRepository.countExpensesBetweenDates(firstDayOfMonth, lastDayOfMonth);

            // If no expenses found (null), set totalExpense to 0.0
            totalExpensesByMonth.put(yearMonth.getMonth().name(), totalExpense != null ? totalExpense : 0.0);
        }

        return totalExpensesByMonth;
    }
    //
    // Method to retrieve total transactions count by month for the current year
    public Map<String, Integer> getTotalTransactionsByMonth() {
        Map<String, Integer> totalTransactionsByMonth = new HashMap<>();

        // Get the current year
        LocalDate currentDate = LocalDate.now();
        int currentYear = currentDate.getYear();

        // Loop through each month of the current year
        for (int month = 1; month <= 12; month++) {
            YearMonth yearMonth = YearMonth.of(currentYear, month);
            LocalDate firstDayOfMonth = yearMonth.atDay(1); // First day of the month
            LocalDate lastDayOfMonth = yearMonth.atEndOfMonth(); // Last day of the month

            // Calculate total transactions for the month
            int totalTransactions = transactionRepository.countTransactionsMonth(firstDayOfMonth, lastDayOfMonth);
            totalTransactionsByMonth.put(yearMonth.getMonth().name(), totalTransactions);
        }

        return totalTransactionsByMonth;
    }



}
