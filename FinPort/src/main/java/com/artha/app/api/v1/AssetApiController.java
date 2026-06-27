package com.artha.app.api.v1;

import com.artha.app.api.dto.AssetResponse;
import com.artha.app.api.dto.PageResponse;
import com.artha.app.models.Asset;
import com.artha.app.models.AssetType;
import com.artha.app.services.AssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/assets")
@Tag(name = "Assets", description = "Tradeable assets (stocks, mutual funds, crypto, gold, …)")
public class AssetApiController {

    private final AssetService assetService;

    public AssetApiController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    @Operation(summary = "List assets (paged, optionally filtered by query or type)")
    public PageResponse<AssetResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) AssetType type) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 200));
        Page<Asset> result = assetService.list(q, type, pageable);
        return PageResponse.from(result, AssetResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single asset by id")
    public AssetResponse get(@PathVariable Long id) {
        return assetService.findById(id)
                .map(AssetResponse::from)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Asset not found"));
    }
}