package com.artha.app.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;

@Entity
@Table(name = "recurring_transactions", indexes = {
        @Index(name = "idx_rt_user", columnList = "user_id"),
        @Index(name = "idx_rt_active", columnList = "active")
})
public class RecurringTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotNull
    @DecimalMin(value = "0.01", message = "amount must be greater than zero")
    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal amount;

    @NotBlank
    @Size(max = 255)
    @Column(nullable = false, length = 255)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 16)
    private Transaction.TransactionType transactionType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Cadence cadence;

    /** First day the rule should fire. */
    @NotNull
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /** Optional end date — null means forever. */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** For MONTHLY cadence: day of month (1-31). Null uses startDate's day. */
    @Min(1)
    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    /** For WEEKLY cadence: 1-7 (Mon-Sun). Null uses startDate's day. */
    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    /** Last time the scheduler fired this rule and produced a Transaction. */
    @Column(name = "last_run_date")
    private LocalDate lastRunDate;

    @Column(nullable = false)
    private boolean active = true;

    public RecurringTransaction() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Transaction.TransactionType getTransactionType() { return transactionType; }
    public void setTransactionType(Transaction.TransactionType transactionType) { this.transactionType = transactionType; }

    public Cadence getCadence() { return cadence; }
    public void setCadence(Cadence cadence) { this.cadence = cadence; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Integer getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(Integer dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    public Integer getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Integer dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public LocalDate getLastRunDate() { return lastRunDate; }
    public void setLastRunDate(LocalDate lastRunDate) { this.lastRunDate = lastRunDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /** Convenience: the day of week for WEEKLY cadence, defaulting to startDate. */
    public DayOfWeek effectiveDayOfWeek() {
        if (dayOfWeek != null && dayOfWeek >= 1 && dayOfWeek <= 7) {
            return DayOfWeek.of(dayOfWeek);
        }
        return startDate.getDayOfWeek();
    }
}