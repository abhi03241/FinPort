package com.artha.app.services;

import com.artha.app.models.Asset;
import com.artha.app.models.AssetType;
import com.artha.app.repository.AssetRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AssetService {

    private final AssetRepository assetRepository;

    public AssetService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public Page<Asset> list(String query, AssetType type, Pageable pageable) {
        if (type != null) {
            return assetRepository.findByAssetType(type, pageable);
        }
        if (query != null && !query.isBlank()) {
            return assetRepository.findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCase(
                    query.trim(), query.trim(), pageable);
        }
        return assetRepository.findAll(pageable);
    }

    public Optional<Asset> findById(Long id) {
        return assetRepository.findById(id);
    }

    public Optional<Asset> findBySymbol(String symbol) {
        return assetRepository.findBySymbolIgnoreCase(symbol);
    }

    @Transactional
    public Asset upsert(Asset asset) {
        return assetRepository.findBySymbolIgnoreCase(asset.getSymbol())
                .map(existing -> {
                    existing.setName(asset.getName());
                    existing.setAssetType(asset.getAssetType());
                    existing.setCurrency(asset.getCurrency());
                    existing.setExchange(asset.getExchange());
                    return assetRepository.save(existing);
                })
                .orElseGet(() -> assetRepository.save(asset));
    }
}