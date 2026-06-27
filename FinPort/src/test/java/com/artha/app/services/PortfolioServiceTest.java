package com.artha.app.services;

import com.artha.app.models.Asset;
import com.artha.app.models.AssetType;
import com.artha.app.models.Holding;
import com.artha.app.services.pricing.PriceProvider;
import com.artha.app.services.pricing.PricingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PortfolioServiceTest {

    private PricingService pricing;
    private PortfolioService service;

    @BeforeEach
    void setUp() {
        PriceProvider fixed = new PriceProvider() {
            @Override public BigDecimal getPrice(Asset a) { return PRICES.get(a.getSymbol()); }
            @Override public String name() { return "test-fixed"; }
        };
        pricing = new PricingService(fixed, null, false);
        service = new PortfolioService(null, pricing);
    }

    private static final java.util.Map<String, BigDecimal> PRICES = java.util.Map.of(
            "RELIANCE", new BigDecimal("3000.00"),
            "BTC",      new BigDecimal("70000.00")
    );

    private Asset asset(String symbol, AssetType type, String ccy) {
        Asset a = new Asset(symbol, symbol, type);
        a.setCurrency(ccy);
        return a;
    }

    private Holding holding(Asset a, String qty, String buyPrice) {
        Holding h = new Holding();
        h.setAsset(a);
        h.setQuantity(new BigDecimal(qty));
        h.setBuyPrice(new BigDecimal(buyPrice));
        h.setBuyDate(LocalDate.of(2025, 1, 1));
        return h;
    }

    @Test
    @DisplayName("computes totals, gain and per-asset-type allocation correctly")
    void computesSnapshot() {
        Asset reliance = asset("RELIANCE", AssetType.STOCK, "INR");
        Asset btc      = asset("BTC",      AssetType.CRYPTO, "USD");

        List<Holding> holdings = List.of(
                holding(reliance, "10", "2500.00"),   // invested 25,000; current 30,000; +5,000
                holding(btc,      "0.5", "50000.00") // invested 25,000; current 35,000; +10,000
        );

        PortfolioService.Snapshot snap = service.snapshotFor(holdings);

        assertThat(snap.totalInvested()).isEqualByComparingTo("50000");
        assertThat(snap.currentValue()).isEqualByComparingTo("65000");
        assertThat(snap.totalGain()).isEqualByComparingTo("15000");
        assertThat(snap.totalGainPct()).isEqualByComparingTo("30.0000");
        assertThat(snap.holdings()).hasSize(2);
        // Sorted by currentValue desc
        assertThat(snap.holdings().get(0).symbol()).isEqualTo("BTC");
        assertThat(snap.holdings().get(0).gain()).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("handles empty portfolio without divide-by-zero")
    void emptyPortfolio() {
        PortfolioService.Snapshot snap = service.snapshotFor(List.of());

        assertThat(snap.totalInvested()).isEqualByComparingTo("0");
        assertThat(snap.currentValue()).isEqualByComparingTo("0");
        assertThat(snap.totalGain()).isEqualByComparingTo("0");
        assertThat(snap.totalGainPct()).isEqualByComparingTo("0");
        assertThat(snap.holdings()).isEmpty();
    }

    @Test
    @DisplayName("falls back to invested value when price provider returns null")
    void missingPriceUsesInvested() {
        Asset unknown = asset("XYZ", AssetType.STOCK, "USD");
        PortfolioService.Snapshot snap = service.snapshotFor(List.of(holding(unknown, "5", "100")));

        assertThat(snap.currentValue()).isEqualByComparingTo("500");
        assertThat(snap.totalGain()).isEqualByComparingTo("0");
    }
}