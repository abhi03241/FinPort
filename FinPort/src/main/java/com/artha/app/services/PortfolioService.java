package com.artha.app.services;

import com.artha.app.models.Asset;
import com.artha.app.models.AssetType;
import com.artha.app.models.Holding;
import com.artha.app.models.User;
import com.artha.app.services.pricing.PricingService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Aggregations over a user's holdings: net worth, allocation, gain/loss.
 * Pure functions of the current holdings + pricing service — easy to unit test.
 */
@Service
public class PortfolioService {

    private final HoldingService holdingService;
    private final PricingService pricingService;

    public PortfolioService(HoldingService holdingService, PricingService pricingService) {
        this.holdingService = holdingService;
        this.pricingService = pricingService;
    }

    public Snapshot snapshotFor(User user) {
        List<Holding> holdings = holdingService.findByUser(user);
        return computeSnapshot(holdings);
    }

    public Snapshot snapshotFor(List<Holding> holdings) {
        return computeSnapshot(holdings);
    }

    private Snapshot computeSnapshot(List<Holding> holdings) {
        BigDecimal invested = BigDecimal.ZERO;
        BigDecimal current = BigDecimal.ZERO;
        Map<AssetType, BigDecimal> investedByType = new EnumMap<>(AssetType.class);
        Map<AssetType, BigDecimal> currentByType = new EnumMap<>(AssetType.class);
        List<EnrichedHolding> rows = new ArrayList<>(holdings.size());

        for (Holding h : holdings) {
            Asset asset = h.getAsset();
            BigDecimal price = pricingService.getPrice(asset);
            BigDecimal currentValue = price == null
                    ? h.investedValue()
                    : price.multiply(h.getQuantity());

            BigDecimal lineInvested = h.investedValue();
            invested = invested.add(lineInvested);
            current = current.add(currentValue);
            investedByType.merge(asset.getAssetType(), lineInvested, BigDecimal::add);
            currentByType.merge(asset.getAssetType(), currentValue, BigDecimal::add);

            rows.add(new EnrichedHolding(
                    h.getId(),
                    asset.getId(),
                    asset.getSymbol(),
                    asset.getName(),
                    asset.getAssetType(),
                    asset.getCurrency(),
                    h.getQuantity(),
                    h.getBuyPrice(),
                    price,
                    currentValue,
                    lineInvested,
                    currentValue.subtract(lineInvested),
                    h.getBuyDate()));
        }

        rows.sort(Comparator.comparing(EnrichedHolding::currentValue).reversed());

        BigDecimal gain = current.subtract(invested);
        BigDecimal gainPct = invested.signum() == 0
                ? BigDecimal.ZERO
                : gain.multiply(BigDecimal.valueOf(100))
                    .divide(invested, 4, RoundingMode.HALF_UP);

        return new Snapshot(invested, current, gain, gainPct, rows, investedByType, currentByType);
    }

    public record Snapshot(
            BigDecimal totalInvested,
            BigDecimal currentValue,
            BigDecimal totalGain,
            BigDecimal totalGainPct,
            List<EnrichedHolding> holdings,
            Map<AssetType, BigDecimal> investedByType,
            Map<AssetType, BigDecimal> currentValueByType
    ) {}

    public record EnrichedHolding(
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
            java.time.LocalDate buyDate
    ) {}
}