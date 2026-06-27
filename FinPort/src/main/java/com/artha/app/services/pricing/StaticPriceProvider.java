package com.artha.app.services.pricing;

import com.artha.app.models.Asset;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Default in-memory price provider used until a real market data source is wired in.
 * Values are illustrative — replace with a Yahoo Finance / CoinGecko client for production.
 */
@Component
@ConditionalOnMissingBean(name = "priceProvider")
public class StaticPriceProvider implements PriceProvider {

    private static final Map<String, BigDecimal> PRICES = Map.ofEntries(
            // Indian equities (NSE) — INR
            Map.entry("RELIANCE", new BigDecimal("2920.50")),
            Map.entry("TCS",       new BigDecimal("4085.00")),
            Map.entry("INFY",      new BigDecimal("1735.20")),
            Map.entry("HDFCBANK",  new BigDecimal("1645.75")),
            Map.entry("ITC",       new BigDecimal("445.10")),
            // US equities — USD
            Map.entry("AAPL",      new BigDecimal("223.45")),
            Map.entry("MSFT",      new BigDecimal("421.80")),
            Map.entry("GOOGL",     new BigDecimal("168.20")),
            Map.entry("TSLA",      new BigDecimal("245.60")),
            // Crypto — USD
            Map.entry("BTC",       new BigDecimal("64500.00")),
            Map.entry("ETH",       new BigDecimal("3250.00")),
            Map.entry("SOL",       new BigDecimal("148.40")),
            // Gold ETF — INR
            Map.entry("GOLDBEES",  new BigDecimal("72.85")),
            // Index funds
            Map.entry("NIFTY50",   new BigDecimal("245.30"))
    );

    @Override
    public BigDecimal getPrice(Asset asset) {
        if (asset == null || asset.getSymbol() == null) return null;
        return PRICES.get(asset.getSymbol().toUpperCase());
    }

    @Override
    public String name() {
        return "static";
    }
}