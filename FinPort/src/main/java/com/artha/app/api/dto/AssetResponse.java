package com.artha.app.api.dto;

import com.artha.app.models.Asset;
import com.artha.app.models.AssetType;

import java.math.BigDecimal;

public record AssetResponse(
        Long id,
        String symbol,
        String name,
        AssetType assetType,
        String currency,
        String exchange,
        BigDecimal lastPrice
) {
    public static AssetResponse from(Asset a) {
        return new AssetResponse(
                a.getId(), a.getSymbol(), a.getName(), a.getAssetType(),
                a.getCurrency(), a.getExchange(), a.getLastPrice());
    }
}