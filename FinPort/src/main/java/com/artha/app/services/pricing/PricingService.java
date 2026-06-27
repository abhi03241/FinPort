package com.artha.app.services.pricing;

import com.artha.app.models.Asset;
import com.artha.app.repository.AssetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caching layer in front of a {@link PriceProvider}. Persists the latest known
 * price onto {@link Asset#lastPrice} so portfolio reads are cheap.
 */
@Service
public class PricingService {

    private static final Logger log = LoggerFactory.getLogger(PricingService.class);
    private static final Duration TTL = Duration.ofMinutes(15);

    private final PriceProvider provider;
    private final AssetRepository assetRepository;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();
    private final boolean persistEnabled;

    public PricingService(PriceProvider provider,
                          AssetRepository assetRepository,
                          @Value("${artha.pricing.persist:true}") boolean persistEnabled) {
        this.provider = provider;
        this.assetRepository = assetRepository;
        this.persistEnabled = persistEnabled;
    }

    /**
     * Return the current price for the asset, fetching via the provider if
     * the cached value is stale or missing. Never returns null if a price is
     * known — falls back to the asset's stored {@code lastPrice}.
     */
    public BigDecimal getPrice(Asset asset) {
        if (asset == null) return null;
        String symbol = asset.getSymbol();
        if (symbol == null) return asset.getLastPrice();

        CacheEntry entry = cache.get(symbol);
        Instant now = Instant.now();
        if (entry != null && entry.price != null
                && entry.fetchedAt != null
                && Duration.between(entry.fetchedAt, now).compareTo(TTL) < 0) {
            return entry.price;
        }

        BigDecimal fetched = safeFetch(asset);
        if (fetched != null) {
            cache.put(symbol, new CacheEntry(fetched, now));
            if (persistEnabled) {
                persistPrice(asset, fetched);
            }
            return fetched;
        }
        return asset.getLastPrice();
    }

    /** Refresh prices for every asset in the database. */
    @Transactional
    public int refreshAll() {
        var assets = assetRepository.findAll();
        int updated = 0;
        for (Asset a : assets) {
            BigDecimal p = safeFetch(a);
            if (p != null) {
                cache.put(a.getSymbol(), new CacheEntry(p, Instant.now()));
                if (persistEnabled) {
                    a.setLastPrice(p);
                    assetRepository.save(a);
                }
                updated++;
            }
        }
        log.info("Pricing refresh via {}: {}/{} assets updated", provider.name(), updated, assets.size());
        return updated;
    }

    public String providerName() {
        return provider.name();
    }

    private BigDecimal safeFetch(Asset asset) {
        try {
            return provider.getPrice(asset);
        } catch (Exception ex) {
            log.warn("Price fetch failed for {} via {}: {}", asset.getSymbol(), provider.name(), ex.getMessage());
            return null;
        }
    }

    private void persistPrice(Asset asset, BigDecimal price) {
        try {
            asset.setLastPrice(price);
            assetRepository.save(asset);
        } catch (Exception ex) {
            log.debug("Could not persist price for {}: {}", asset.getSymbol(), ex.getMessage());
        }
    }

    private record CacheEntry(BigDecimal price, Instant fetchedAt) {}
}