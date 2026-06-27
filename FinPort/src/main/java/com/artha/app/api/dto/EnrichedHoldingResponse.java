package com.artha.app.api.dto;

import com.artha.app.models.AssetType;
import com.artha.app.services.PortfolioService;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EnrichedHoldingResponse(
        Long id,
        Long assetId,
        String symbol,
        String name,
        AssetType assetType,
        String currency,
        BigDecimal quantity,
        BigDecimal buyPrice,
        BigDecimal currentPrice,
        BigDecimal currentValue,
        BigDecimal investedValue,
        BigDecimal gain,
        LocalDate buyDate
) {
    public static EnrichedHoldingResponse from(PortfolioService.EnrichedHolding e) {
        return new EnrichedHoldingResponse(
                e.id(), e.assetId(), e.symbol(), e.name(), e.assetType(),
                e.currency(), e.quantity(), e.buyPrice(), e.currentPrice(),
                e.currentValue(), e.investedValue(), e.gain(), e.buyDate());
    }
}