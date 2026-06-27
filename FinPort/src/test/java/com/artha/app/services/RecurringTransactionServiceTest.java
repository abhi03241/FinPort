package com.artha.app.services;

import com.artha.app.models.Cadence;
import com.artha.app.models.Category;
import com.artha.app.models.RecurringTransaction;
import com.artha.app.models.Transaction;
import com.artha.app.models.User;
import com.artha.app.repository.RecurringTransactionRepository;
import com.artha.app.services.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceTest {

    @Mock RecurringTransactionRepository recurringRepository;
    @Mock TransactionRepository transactionRepository;

    @InjectMocks RecurringTransactionService service;

    private User user;
    private Category food;

    @BeforeEach
    void setUp() {
        user = new User("abhi", "x"); user.setId(1);
        food = new Category("Food"); food.setId(7L);

        lenient().when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(System.nanoTime());
            return t;
        });
    }

    private RecurringTransaction rule(Cadence c, LocalDate start, Integer dom, Integer dow) {
        RecurringTransaction r = new RecurringTransaction();
        r.setUser(user);
        r.setCategory(food);
        r.setAmount(new BigDecimal("100"));
        r.setDescription("Test");
        r.setTransactionType(Transaction.TransactionType.EXPENSE);
        r.setCadence(c);
        r.setStartDate(start);
        r.setDayOfMonth(dom);
        r.setDayOfWeek(dow);
        r.setActive(true);
        return r;
    }

    @Test
    @DisplayName("processDue on a fresh DAILY rule generates one occurrence per day since startDate")
    void daily_generatesAllMissing() {
        RecurringTransaction r = rule(Cadence.DAILY, LocalDate.of(2026, 6, 1), null, null);
        when(recurringRepository.findByActiveTrue()).thenReturn(List.of(r));

        List<Transaction> generated = service.processDue(LocalDate.of(2026, 6, 5));

        assertThat(generated).hasSize(5);
        assertThat(generated.get(0).getDate()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(generated.get(4).getDate()).isEqualTo(LocalDate.of(2026, 6, 5));
        assertThat(r.getLastRunDate()).isEqualTo(LocalDate.of(2026, 6, 5));
    }

    @Test
    @DisplayName("processDue skips days before startDate and stops at endDate")
    void respectsStartAndEnd() {
        RecurringTransaction r = rule(Cadence.DAILY, LocalDate.of(2026, 6, 10), null, null);
        r.setEndDate(LocalDate.of(2026, 6, 12));
        when(recurringRepository.findByActiveTrue()).thenReturn(List.of(r));

        // Today is June 30 — but endDate caps us at June 12.
        List<Transaction> generated = service.processDue(LocalDate.of(2026, 6, 30));

        assertThat(generated).hasSize(3);
        assertThat(generated.get(0).getDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(generated.get(2).getDate()).isEqualTo(LocalDate.of(2026, 6, 12));
        assertThat(r.getLastRunDate()).isEqualTo(LocalDate.of(2026, 6, 12));
    }

    @Test
    @DisplayName("processDue continues from lastRunDate — no double-posting")
    void continuesFromLastRun() {
        RecurringTransaction r = rule(Cadence.DAILY, LocalDate.of(2026, 6, 1), null, null);
        r.setLastRunDate(LocalDate.of(2026, 6, 3));
        when(recurringRepository.findByActiveTrue()).thenReturn(List.of(r));

        List<Transaction> generated = service.processDue(LocalDate.of(2026, 6, 7));

        assertThat(generated).hasSize(4); // 4,5,6,7
        assertThat(generated.get(0).getDate()).isEqualTo(LocalDate.of(2026, 6, 4));
        assertThat(r.getLastRunDate()).isEqualTo(LocalDate.of(2026, 6, 7));
    }

    @Test
    @DisplayName("WEEKLY cadence picks the configured day-of-week")
    void weekly_usesDow() {
        // Friday weekly, starting Monday June 1 2026 — first occurrence should be Fri June 5.
        RecurringTransaction r = rule(Cadence.WEEKLY, LocalDate.of(2026, 6, 1), null, DayOfWeek.FRIDAY.getValue());
        when(recurringRepository.findByActiveTrue()).thenReturn(List.of(r));

        List<Transaction> generated = service.processDue(LocalDate.of(2026, 6, 30));

        // Fridays: 5, 12, 19, 26 → 4 occurrences
        assertThat(generated).hasSize(4);
        assertThat(generated.get(0).getDate()).isEqualTo(LocalDate.of(2026, 6, 5));
        assertThat(generated.get(3).getDate()).isEqualTo(LocalDate.of(2026, 6, 26));
    }

    @Test
    @DisplayName("MONTHLY cadence honours dayOfMonth and clamps Feb 30 -> Feb 28/29")
    void monthly_clampsShortMonths() {
        RecurringTransaction r = rule(Cadence.MONTHLY, LocalDate.of(2026, 1, 31), 31, null);
        when(recurringRepository.findByActiveTrue()).thenReturn(List.of(r));

        List<Transaction> generated = service.processDue(LocalDate.of(2026, 6, 15));

        // Jan 31, Feb 28, Mar 31, Apr 30, May 31, Jun 30 (June capped at 30 since today is 15th... no wait
        // processDue includes today, but nextOccurrenceAfter for Jun 30 happens to land past today)
        // Let me recompute: starting Jan 31, today June 15.
        // Cursor = Jan 31 (no lastRunDate). Generates Jan 31. Next = Feb 28. Generates. Next = Mar 31.
        // Generates. Next = Apr 30. Generates. Next = May 31. Generates. Next = Jun 30 (which is AFTER today).
        // So we should get 5 occurrences: Jan, Feb, Mar, Apr, May.
        assertThat(generated).hasSize(5);
        assertThat(generated.get(0).getDate()).isEqualTo(LocalDate.of(2026, 1, 31));
        assertThat(generated.get(1).getDate()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(generated.get(2).getDate()).isEqualTo(LocalDate.of(2026, 3, 31));
        assertThat(generated.get(3).getDate()).isEqualTo(LocalDate.of(2026, 4, 30));
        assertThat(generated.get(4).getDate()).isEqualTo(LocalDate.of(2026, 5, 31));
    }

    @Test
    @DisplayName("runNow creates a single transaction for today and updates lastRunDate")
    void runNow_single() {
        RecurringTransaction r = rule(Cadence.MONTHLY, LocalDate.of(2026, 1, 1), 1, null);
        LocalDate before = LocalDate.now();

        Transaction tx = service.runNow(r);

        assertThat(tx.getDate()).isEqualTo(before);
        assertThat(tx.getAmount()).isEqualTo(100.0);
        assertThat(r.getLastRunDate()).isEqualTo(before);
        ArgumentCaptor<RecurringTransaction> captor = ArgumentCaptor.forClass(RecurringTransaction.class);
        verify(recurringRepository).save(captor.capture());
        assertThat(captor.getValue().getLastRunDate()).isEqualTo(before);
    }

    @Test
    @DisplayName("nextOccurrenceAfter handles all cadences correctly")
    void nextOccurrenceAfter() {
        RecurringTransaction d = rule(Cadence.DAILY, LocalDate.now(), null, null);
        assertThat(service.nextOccurrenceAfter(d, LocalDate.of(2026, 6, 1)))
                .isEqualTo(LocalDate.of(2026, 6, 2));

        RecurringTransaction w = rule(Cadence.WEEKLY, LocalDate.now(), null, DayOfWeek.MONDAY.getValue());
        // Starting from a Wednesday June 3, next Monday is June 8
        assertThat(service.nextOccurrenceAfter(w, LocalDate.of(2026, 6, 3)))
                .isEqualTo(LocalDate.of(2026, 6, 8));

        RecurringTransaction m = rule(Cadence.MONTHLY, LocalDate.now(), 15, null);
        assertThat(service.nextOccurrenceAfter(m, LocalDate.of(2026, 6, 20)))
                .isEqualTo(LocalDate.of(2026, 7, 15));

        RecurringTransaction y = rule(Cadence.YEARLY, LocalDate.now(), null, null);
        assertThat(service.nextOccurrenceAfter(y, LocalDate.of(2026, 6, 1)))
                .isEqualTo(LocalDate.of(2027, 6, 1));
    }
}