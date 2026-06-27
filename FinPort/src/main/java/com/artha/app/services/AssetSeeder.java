package com.artha.app.services;

import com.artha.app.models.Asset;
import com.artha.app.models.AssetType;
import com.artha.app.repository.AssetRepository;
import com.artha.app.services.pricing.PricingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * On startup, populate the asset catalog with a handful of well-known tickers
 * across asset types so the UI has something to demo immediately. Safe to run
 * repeatedly — uses upsert.
 */
@Component
public class AssetSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AssetSeeder.class);

    private final AssetService assetService;
    private final PricingService pricingService;

    public AssetSeeder(AssetService assetService, PricingService pricingService) {
        this.assetService = assetService;
        this.pricingService = pricingService;
    }

    @Override
    public void run(String... args) {
        List<Asset> seeds = List.of(
                asset("RELIANCE", "Reliance Industries Ltd",     AssetType.STOCK,      "INR", "NSE"),
                asset("TCS",      "Tata Consultancy Services",    AssetType.STOCK,      "INR", "NSE"),
                asset("INFY",     "Infosys Ltd",                 AssetType.STOCK,      "INR", "NSE"),
                asset("HDFCBANK", "HDFC Bank Ltd",               AssetType.STOCK,      "INR", "NSE"),
                asset("ITC",      "ITC Ltd",                     AssetType.STOCK,      "INR", "NSE"),
                asset("AAPL",     "Apple Inc.",                  AssetType.STOCK,      "USD", "NASDAQ"),
                asset("MSFT",     "Microsoft Corporation",       AssetType.STOCK,      "USD", "NASDAQ"),
                asset("GOOGL",    "Alphabet Inc. Class A",       AssetType.STOCK,      "USD", "NASDAQ"),
                asset("TSLA",     "Tesla, Inc.",                 AssetType.STOCK,      "USD", "NASDAQ"),
                asset("NIFTY50",  "Nippon India Nifty 50 ETF",   AssetType.ETF,        "INR", "NSE"),
                asset("BTC",      "Bitcoin",                     AssetType.CRYPTO,     "USD", null),
                asset("ETH",      "Ethereum",                    AssetType.CRYPTO,     "USD", null),
                asset("SOL",      "Solana",                      AssetType.CRYPTO,     "USD", null),
                asset("GOLDBEES", "Nippon India Gold BeES ETF",  AssetType.GOLD,       "INR", "NSE")
        );
        seeds.forEach(assetService::upsert);

        // Best-effort price snapshot at startup
        try {
            int updated = pricingService.refreshAll();
            log.info("Seeded {} assets; refreshed {} prices", seeds.size(), updated);
        } catch (Exception ex) {
            log.warn("Asset seeding complete; price refresh failed: {}", ex.getMessage());
        }
    }

    private static Asset asset(String symbol, String name, AssetType type, String ccy, String exchange) {
        Asset a = new Asset(symbol, name, type);
        a.setCurrency(ccy);
        a.setExchange(exchange);
        return a;
    }
}