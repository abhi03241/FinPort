package com.artha.app.api.v1;

import com.artha.app.models.Asset;
import com.artha.app.services.AssetService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Thin bridge so HoldingApiController doesn't have to depend on AssetService
 * directly; also gives us a single place to throw 404 cleanly.
 */
@Component
public class AssetServiceBridge {

    private final AssetService assetService;

    public AssetServiceBridge(AssetService assetService) {
        this.assetService = assetService;
    }

    public Asset requireAsset(Long id) {
        return assetService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asset not found: " + id));
    }
}