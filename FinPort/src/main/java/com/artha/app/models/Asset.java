package com.artha.app.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Entity
@Table(name = "assets", uniqueConstraints = @UniqueConstraint(columnNames = "symbol"))
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 32)
    @Column(nullable = false, length = 32)
    private String symbol;

    @NotBlank
    @Size(max = 128)
    @Column(nullable = false, length = 128)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private AssetType assetType;

    @Size(max = 8)
    @Column(length = 8)
    private String currency = "INR";

    @Size(max = 64)
    @Column(length = 64)
    private String exchange;

    /**
     * Last known price (snapshot). Updated by PricingService from the active
     * PriceProvider. Kept on the entity for cheap reads; for real-time data,
     * hit the provider directly.
     */
    @Column(precision = 18, scale = 6)
    private BigDecimal lastPrice;

    public Asset() {}

    public Asset(String symbol, String name, AssetType assetType) {
        this.symbol = symbol.toUpperCase();
        this.name = name;
        this.assetType = assetType;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol == null ? null : symbol.toUpperCase(); }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public AssetType getAssetType() { return assetType; }
    public void setAssetType(AssetType assetType) { this.assetType = assetType; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getExchange() { return exchange; }
    public void setExchange(String exchange) { this.exchange = exchange; }

    public BigDecimal getLastPrice() { return lastPrice; }
    public void setLastPrice(BigDecimal lastPrice) { this.lastPrice = lastPrice; }
}