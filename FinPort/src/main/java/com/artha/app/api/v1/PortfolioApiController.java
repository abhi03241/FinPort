package com.artha.app.api.v1;

import com.artha.app.api.CurrentUserProvider;
import com.artha.app.api.dto.PortfolioSummaryResponse;
import com.artha.app.models.User;
import com.artha.app.services.PortfolioService;
import com.artha.app.services.pricing.PricingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/portfolio")
@Tag(name = "Portfolio", description = "Net-worth and allocation summaries")
public class PortfolioApiController {

    private final PortfolioService portfolioService;
    private final PricingService pricingService;
    private final CurrentUserProvider currentUserProvider;

    public PortfolioApiController(PortfolioService portfolioService,
                                  PricingService pricingService,
                                  CurrentUserProvider currentUserProvider) {
        this.portfolioService = portfolioService;
        this.pricingService = pricingService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/summary")
    @Operation(summary = "Current user's portfolio summary (totals + holdings + allocation)")
    public PortfolioSummaryResponse summary() {
        User user = currentUserProvider.requireCurrentUser();
        return PortfolioSummaryResponse.from(portfolioService.snapshotFor(user));
    }

    @PostMapping("/refresh-prices")
    @Operation(summary = "Force a price refresh across all assets via the configured price provider")
    public Map<String, Object> refresh() {
        int updated = pricingService.refreshAll();
        return Map.of(
                "updated", updated,
                "provider", pricingService.providerName()
        );
    }
}