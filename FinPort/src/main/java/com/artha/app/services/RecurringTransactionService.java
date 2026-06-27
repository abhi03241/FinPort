package com.artha.app.services;

import com.artha.app.models.Cadence;
import com.artha.app.models.Category;
import com.artha.app.models.RecurringTransaction;
import com.artha.app.models.Transaction;
import com.artha.app.models.User;
import com.artha.app.repository.RecurringTransactionRepository;
import com.artha.app.services.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

@Service
public class RecurringTransactionService {

    private static final Logger log = LoggerFactory.getLogger(RecurringTransactionService.class);

    private final RecurringTransactionRepository recurringRepository;
    private final TransactionRepository transactionRepository;

    public RecurringTransactionService(RecurringTransactionRepository recurringRepository,
                                       TransactionRepository transactionRepository) {
        this.recurringRepository = recurringRepository;
        this.transactionRepository = transactionRepository;
    }

    public List<RecurringTransaction> findByUser(User user) {
        return recurringRepository.findByUserId(user.getId());
    }

    public RecurringTransaction getByIdAndUser(Long id, User user) {
        return recurringRepository.findById(id)
                .filter(r -> r.getUser().getId() == user.getId())
                .orElse(null);
    }

    @Transactional
    public RecurringTransaction save(RecurringTransaction r) {
        return recurringRepository.save(r);
    }

    @Transactional
    public void deleteById(Long id) {
        recurringRepository.deleteById(id);
    }

    /**
     * Process every active rule and generate the missing Transaction rows
     * between the last run (or startDate) and the supplied today.
     *
     * @return the list of generated transactions
     */
    @Transactional
    public List<Transaction> processDue(LocalDate today) {
        List<RecurringTransaction> rules = recurringRepository.findByActiveTrue();
        List<Transaction> generated = new ArrayList<>();
        for (RecurringTransaction rule : rules) {
            try {
                generated.addAll(generateForRule(rule, today));
            } catch (Exception ex) {
                log.warn("Failed to process recurring rule id={}: {}", rule.getId(), ex.getMessage());
            }
        }
        return generated;
    }

    /** Generate every missing occurrence for one rule up to and including today. */
    List<Transaction> generateForRule(RecurringTransaction rule, LocalDate today) {
        if (rule.getStartDate().isAfter(today)) {
            return List.of();
        }
        LocalDate endCap = rule.getEndDate() != null && rule.getEndDate().isBefore(today)
                ? rule.getEndDate()
                : today;

        LocalDate cursor = rule.getLastRunDate() != null
                ? nextOccurrenceAfter(rule, rule.getLastRunDate())
                : firstOccurrence(rule);

        List<Transaction> generated = new ArrayList<>();
        LocalDate latest = rule.getLastRunDate();

        while (!cursor.isAfter(endCap)) {
            Transaction tx = materialize(rule, cursor);
            generated.add(transactionRepository.save(tx));
            latest = cursor;
            cursor = nextOccurrenceAfter(rule, cursor);
        }
        if (latest != null && (rule.getLastRunDate() == null || latest.isAfter(rule.getLastRunDate()))) {
            rule.setLastRunDate(latest);
            recurringRepository.save(rule);
        }
        return generated;
    }

    /**
     * First date on or after {@code startDate} that satisfies the cadence.
     * DAILY and YEARLY fire on startDate itself; WEEKLY snaps forward to the
     * configured day-of-week; MONTHLY anchors on dayOfMonth, clamped to month length.
     */
    LocalDate firstOccurrence(RecurringTransaction rule) {
        LocalDate start = rule.getStartDate();
        return switch (rule.getCadence()) {
            case DAILY, YEARLY -> start;
            case WEEKLY -> start.with(TemporalAdjusters.nextOrSame(effectiveDow(rule)));
            case MONTHLY -> {
                int targetDay = rule.getDayOfMonth() != null ? rule.getDayOfMonth() : start.getDayOfMonth();
                int safeDay = Math.min(targetDay, start.lengthOfMonth());
                yield start.withDayOfMonth(safeDay);
            }
        };
    }

    /** Force-create one occurrence now (manual trigger from REST). */
    @Transactional
    public Transaction runNow(RecurringTransaction rule) {
        LocalDate today = LocalDate.now();
        Transaction tx = materialize(rule, today);
        tx = transactionRepository.save(tx);
        rule.setLastRunDate(today);
        recurringRepository.save(rule);
        return tx;
    }

    private Transaction materialize(RecurringTransaction rule, LocalDate when) {
        Transaction tx = new Transaction();
        tx.setAmount(rule.getAmount().doubleValue());
        tx.setDate(when);
        tx.setDescription(rule.getDescription());
        tx.setTransactionType(rule.getTransactionType());
        // Reattach category in the current session to avoid LazyInitializationException
        Category cat = rule.getCategory();
        tx.setCategory(cat);
        return tx;
    }

    /**
     * Given a rule and a date that the rule already fired on, return the
     * next date the rule should fire.
     */
    LocalDate nextOccurrenceAfter(RecurringTransaction rule, LocalDate from) {
        return switch (rule.getCadence()) {
            case DAILY   -> from.plusDays(1);
            case WEEKLY  -> from.with(TemporalAdjusters.next(effectiveDow(rule)));
            case MONTHLY -> monthlyNext(rule, from);
            case YEARLY  -> from.plusYears(1);
        };
    }

    private DayOfWeek effectiveDow(RecurringTransaction rule) {
        return rule.effectiveDayOfWeek();
    }

    private LocalDate monthlyNext(RecurringTransaction rule, LocalDate from) {
        int targetDay = rule.getDayOfMonth() != null ? rule.getDayOfMonth() : from.getDayOfMonth();
        LocalDate candidate = from.plusMonths(1);
        int safeDay = Math.min(targetDay, candidate.lengthOfMonth());
        return candidate.withDayOfMonth(safeDay);
    }
}