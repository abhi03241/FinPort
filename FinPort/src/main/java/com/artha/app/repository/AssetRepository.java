package com.artha.app.repository;

import com.artha.app.models.Asset;
import com.artha.app.models.AssetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findBySymbolIgnoreCase(String symbol);

    Page<Asset> findBySymbolContainingIgnoreCaseOrNameContainingIgnoreCase(
            String symbol, String name, Pageable pageable);

    Page<Asset> findByAssetType(AssetType assetType, Pageable pageable);

    List<Asset> findAllBySymbolInIgnoreCase(List<String> symbols);
}