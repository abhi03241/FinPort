package com.artha.app.api.v1;

import com.artha.app.api.CurrentUserProvider;
import com.artha.app.api.dto.EnrichedHoldingResponse;
import com.artha.app.api.dto.HoldingRequest;
import com.artha.app.models.Asset;
import com.artha.app.models.Holding;
import com.artha.app.models.User;
import com.artha.app.services.HoldingService;
import com.artha.app.services.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v1/holdings")
@Tag(name = "Holdings", description = "Per-user portfolio holdings")
public class HoldingApiController {

    private final HoldingService holdingService;
    private final AssetServiceBridge assetBridge;
    private final PortfolioService portfolioService;
    private final CurrentUserProvider currentUserProvider;

    public HoldingApiController(HoldingService holdingService,
                                AssetServiceBridge assetBridge,
                                PortfolioService portfolioService,
                                CurrentUserProvider currentUserProvider) {
        this.holdingService = holdingService;
        this.assetBridge = assetBridge;
        this.portfolioService = portfolioService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    @Operation(summary = "List the current user's holdings enriched with current price + gain/loss")
    public List<EnrichedHoldingResponse> list() {
        User user = currentUserProvider.requireCurrentUser();
        return portfolioService.snapshotFor(user).holdings().stream()
                .map(EnrichedHoldingResponse::from)
                .toList();
    }

    @PostMapping
    @Operation(summary = "Create a new holding for the current user")
    public ResponseEntity<EnrichedHoldingResponse> create(@Valid @RequestBody HoldingRequest req) {
        User user = currentUserProvider.requireCurrentUser();
        Asset asset = assetBridge.requireAsset(req.assetId());
        Holding holding = req.toEntity(asset, user);
        holdingService.save(holding);
        PortfolioService.Snapshot snap = portfolioService.snapshotFor(List.of(holding));
        return ResponseEntity.status(201).body(EnrichedHoldingResponse.from(snap.holdings().get(0)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing holding of the current user")
    public EnrichedHoldingResponse update(@PathVariable Long id, @Valid @RequestBody HoldingRequest req) {
        User user = currentUserProvider.requireCurrentUser();
        Holding existing = holdingService.findByUser(user).stream()
                .filter(h -> h.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Holding not found"));
        Asset asset = assetBridge.requireAsset(req.assetId());
        existing.setAsset(asset);
        existing.setQuantity(req.quantity());
        existing.setBuyPrice(req.buyPrice());
        existing.setBuyDate(req.buyDate());
        existing.setNotes(req.notes());
        holdingService.save(existing);
        PortfolioService.Snapshot snap = portfolioService.snapshotFor(List.of(existing));
        return EnrichedHoldingResponse.from(snap.holdings().get(0));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a holding of the current user")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        User user = currentUserProvider.requireCurrentUser();
        boolean owns = holdingService.findByUser(user).stream()
                .anyMatch(h -> h.getId().equals(id));
        if (!owns) {
            throw new ResponseStatusException(NOT_FOUND, "Holding not found");
        }
        holdingService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}