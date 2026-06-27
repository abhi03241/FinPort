package com.artha.app.api.dto;

import com.artha.app.models.Asset;
import com.artha.app.models.AssetType;
import com.artha.app.models.Holding;
import com.artha.app.services.PortfolioService;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record HoldingRequest(
        @NotNull(message = "assetId is required")
        Long assetId,

        @NotNull(message = "quantity is required")
        @DecimalMin(value = "0.000000001", message = "quantity must be positive")
        BigDecimal quantity,

        @NotNull(message = "buyPrice is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "buyPrice must be positive")
        BigDecimal buyPrice,

        @NotNull(message = "buyDate is required")
        LocalDate buyDate,

        String notes
) {
    public Holding toEntity(Asset asset, com.artha.app.models.User user) {
        Holding h = new Holding();
        h.setAsset(asset);
        h.setUser(user);
        h.setQuantity(quantity);
        h.setBuyPrice(buyPrice);
        h.setBuyDate(buyDate);
        h.setNotes(notes);
        return h;
    }
}