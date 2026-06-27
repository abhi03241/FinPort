package com.artha.app.api.dto;

import com.artha.app.models.AssetType;
import com.artha.app.services.PortfolioService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PortfolioSummaryResponse(
        BigDecimal totalInvested,
        BigDecimal currentValue,
        BigDecimal totalGain,
        BigDecimal totalGainPct,
        int holdingsCount,
        Map<AssetType, BigDecimal> allocation,
        List<EnrichedHoldingResponse> holdings
) {
    public static PortfolioSummaryResponse from(PortfolioService.Snapshot s) {
        Map<AssetType, BigDecimal> allocation = new java.util.EnumMap<>(AssetType.class);
        s.currentValueByType().forEach((k, v) -> {
            if (s.currentValue().signum() > 0) {
                allocation.put(k,
                        v.multiply(BigDecimal.valueOf(100))
                         .divide(s.currentValue(), 2, java.math.RoundingMode.HALF_UP));
            } else {
                allocation.put(k, BigDecimal.ZERO);
            }
        });
        return new PortfolioSummaryResponse(
                s.totalInvested(),
                s.currentValue(),
                s.totalGain(),
                s.totalGainPct(),
                s.holdings().size(),
                allocation,
                s.holdings().stream().map(EnrichedHoldingResponse::from).toList()
        );
    }
}