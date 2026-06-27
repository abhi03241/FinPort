package com.artha.app.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "holdings", indexes = {
        @Index(name = "idx_holdings_user", columnList = "user_id")
})
public class Holding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @DecimalMin(value = "0.000000001", message = "quantity must be positive")
    @Column(nullable = false, precision = 24, scale = 8)
    private BigDecimal quantity;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false, message = "buyPrice must be positive")
    @Column(name = "buy_price", nullable = false, precision = 18, scale = 6)
    private BigDecimal buyPrice;

    @NotNull
    @Column(name = "buy_date", nullable = false)
    private LocalDate buyDate;

    @Size(max = 255)
    @Column(length = 255)
    private String notes;

    public Holding() {}

    public Holding(Asset asset, User user, BigDecimal quantity, BigDecimal buyPrice, LocalDate buyDate) {
        this.asset = asset;
        this.user = user;
        this.quantity = quantity;
        this.buyPrice = buyPrice;
        this.buyDate = buyDate;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Asset getAsset() { return asset; }
    public void setAsset(Asset asset) { this.asset = asset; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }

    public BigDecimal getBuyPrice() { return buyPrice; }
    public void setBuyPrice(BigDecimal buyPrice) { this.buyPrice = buyPrice; }

    public LocalDate getBuyDate() { return buyDate; }
    public void setBuyDate(LocalDate buyDate) { this.buyDate = buyDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public BigDecimal investedValue() {
        return buyPrice.multiply(quantity);
    }
}