package com.artha.app.services.pricing;

import com.artha.app.models.Asset;

import java.math.BigDecimal;

/**
 * Strategy for fetching the latest market price of an asset.
 * Implementations may hit Yahoo Finance, CoinGecko, a manual broker feed, etc.
 *
 * Implementations MUST be thread-safe and MUST NOT throw — return null on miss.
 */
public interface PriceProvider {

    /**
     * Look up the latest price for the given asset. Returns null if the
     * provider has no data for the asset (e.g. unsupported ticker).
     */
    BigDecimal getPrice(Asset asset);

    /**
     * Human-readable name of this provider (for diagnostics / actuator).
     */
    String name();
}