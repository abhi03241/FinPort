package com.artha.app.api.v1;

import com.artha.app.api.CurrentUserProvider;
import com.artha.app.models.*;
import com.artha.app.services.AssetService;
import com.artha.app.services.HoldingService;
import com.artha.app.services.PortfolioService;
import com.artha.app.services.pricing.PricingService;
import com.artha.app.testsupport.SecurityTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {HoldingApiController.class, AssetApiController.class, PortfolioApiController.class})
@Import({AssetServiceBridge.class, SecurityTestSupport.class})
@ActiveProfiles("test")
class InvestmentApiControllersTest {

    @Autowired MockMvc mvc;

    @MockBean AssetService assetService;
    @MockBean HoldingService holdingService;
    @MockBean PortfolioService portfolioService;
    @MockBean PricingService pricingService;
    @MockBean CurrentUserProvider currentUserProvider;

    private User user;
    private Asset reliance;
    private Asset btc;

    @BeforeEach
    void setUp() {
        user = new User("abhi", "x");
        user.setId(1);
        reliance = new Asset("RELIANCE", "Reliance", AssetType.STOCK); reliance.setId(10L);
        btc      = new Asset("BTC",      "Bitcoin",  AssetType.CRYPTO); btc.setId(11L);

        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(pricingService.getPrice(any(Asset.class))).thenAnswer(inv -> {
            Asset a = inv.getArgument(0);
            return switch (a.getSymbol()) {
                case "RELIANCE" -> new BigDecimal("3000.00");
                case "BTC"      -> new BigDecimal("70000.00");
                default         -> null;
            };
        });
    }

    @Test
    @WithMockUser(username = "abhi")
    void listAssets_returnsSeededAssets() throws Exception {
        when(assetService.list(any(), any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(reliance, btc)));

        mvc.perform(get("/api/v1/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].symbol").value("RELIANCE"))
                .andExpect(jsonPath("$.content[1].symbol").value("BTC"));
    }

    @Test
    @WithMockUser(username = "abhi")
    void getAsset_returnsAsset() throws Exception {
        when(assetService.findById(10L)).thenReturn(java.util.Optional.of(reliance));

        mvc.perform(get("/api/v1/assets/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("RELIANCE"));
    }

    @Test
    @WithMockUser(username = "abhi")
    void listHoldings_returnsEnrichedRows() throws Exception {
        Holding h = new Holding(reliance, user, new BigDecimal("10"), new BigDecimal("2500"), LocalDate.of(2025, 1, 1));
        h.setId(99L);
        PortfolioService.Snapshot snap = new PortfolioService.Snapshot(
                new BigDecimal("25000"), new BigDecimal("30000"),
                new BigDecimal("5000"), new BigDecimal("20.00"),
                List.of(new PortfolioService.EnrichedHolding(
                        99L, 10L, "RELIANCE", "Reliance", AssetType.STOCK, "INR",
                        new BigDecimal("10"), new BigDecimal("2500"),
                        new BigDecimal("3000"), new BigDecimal("30000"),
                        new BigDecimal("25000"), new BigDecimal("5000"),
                        LocalDate.of(2025, 1, 1))),
                java.util.Map.of(AssetType.STOCK, new BigDecimal("25000")),
                java.util.Map.of(AssetType.STOCK, new BigDecimal("30000")));
        when(portfolioService.snapshotFor(user)).thenReturn(snap);

        mvc.perform(get("/api/v1/holdings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("RELIANCE"))
                .andExpect(jsonPath("$[0].gain").value(5000));
    }

    @Test
    @WithMockUser(username = "abhi")
    void createHolding_returns201AndEnrichedRow() throws Exception {
        when(assetService.findById(10L)).thenReturn(java.util.Optional.of(reliance));
        when(holdingService.save(any(Holding.class))).thenAnswer(inv -> {
            Holding h = inv.getArgument(0);
            h.setId(42L);
            return h;
        });
        PortfolioService.Snapshot snap = new PortfolioService.Snapshot(
                new BigDecimal("25000"), new BigDecimal("30000"),
                new BigDecimal("5000"), new BigDecimal("20.00"),
                List.of(new PortfolioService.EnrichedHolding(
                        42L, 10L, "RELIANCE", "Reliance", AssetType.STOCK, "INR",
                        new BigDecimal("10"), new BigDecimal("2500"),
                        new BigDecimal("3000"), new BigDecimal("30000"),
                        new BigDecimal("25000"), new BigDecimal("5000"),
                        LocalDate.of(2025, 1, 1))),
                java.util.Map.of(), java.util.Map.of());
        when(portfolioService.snapshotFor(any(List.class))).thenReturn(snap);

        String body = """
                {"assetId":10,"quantity":10,"buyPrice":2500,"buyDate":"2025-01-01"}
                """;

        mvc.perform(post("/api/v1/holdings")
                        .with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.symbol").value("RELIANCE"));
    }

    @Test
    @WithMockUser(username = "abhi")
    void createHolding_invalidAsset_returns404() throws Exception {
        when(assetService.findById(99L)).thenReturn(java.util.Optional.empty());

        String body = """
                {"assetId":99,"quantity":1,"buyPrice":1,"buyDate":"2025-01-01"}
                """;

        mvc.perform(post("/api/v1/holdings")
                        .with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "abhi")
    void createHolding_invalidQuantity_returns400() throws Exception {
        String body = """
                {"assetId":10,"quantity":0,"buyPrice":100,"buyDate":"2025-01-01"}
                """;

        mvc.perform(post("/api/v1/holdings")
                        .with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("quantity"));
    }

    @Test
    @WithMockUser(username = "abhi")
    void portfolioSummary_returnsAggregates() throws Exception {
        PortfolioService.Snapshot snap = new PortfolioService.Snapshot(
                new BigDecimal("50000"), new BigDecimal("65000"),
                new BigDecimal("15000"), new BigDecimal("30.0000"),
                List.of(),
                java.util.Map.of(AssetType.STOCK, new BigDecimal("30000"), AssetType.CRYPTO, new BigDecimal("35000")),
                java.util.Map.of(AssetType.STOCK, new BigDecimal("30000"), AssetType.CRYPTO, new BigDecimal("35000")));
        when(portfolioService.snapshotFor(user)).thenReturn(snap);

        mvc.perform(get("/api/v1/portfolio/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalInvested").value(50000))
                .andExpect(jsonPath("$.currentValue").value(65000))
                .andExpect(jsonPath("$.totalGain").value(15000))
                .andExpect(jsonPath("$.totalGainPct").value(30.0))
                .andExpect(jsonPath("$.allocation.STOCK").exists())
                .andExpect(jsonPath("$.allocation.CRYPTO").exists());
    }

    @Test
    @WithMockUser(username = "abhi")
    void refreshPrices_returnsUpdatedCount() throws Exception {
        when(pricingService.refreshAll()).thenReturn(14);
        when(pricingService.providerName()).thenReturn("static");

        mvc.perform(post("/api/v1/portfolio/refresh-prices").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(14))
                .andExpect(jsonPath("$.provider").value("static"));
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/v1/holdings"))
                .andExpect(status().isUnauthorized());
    }
}