package com.artha.app.api.v1;

import com.artha.app.api.CurrentUserProvider;
import com.artha.app.models.Category;
import com.artha.app.models.Transaction;
import com.artha.app.models.User;
import com.artha.app.services.TransactionService;
import com.artha.app.services.ai.AnomalyDetectionService;
import com.artha.app.services.ai.AutoCategorizationService;
import com.artha.app.services.ai.CashflowForecastService;
import com.artha.app.services.ai.InsightsService;
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
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AiApiController.class)
@Import(SecurityTestSupport.class)
@ActiveProfiles("test")
class AiApiControllerTest {

    @Autowired MockMvc mvc;

    @MockBean AutoCategorizationService autoCategorization;
    @MockBean AnomalyDetectionService anomalyDetection;
    @MockBean CashflowForecastService cashflowForecast;
    @MockBean InsightsService insights;
    @MockBean TransactionService transactionService;
    @MockBean RecurringTransactionServiceBridge recurringBridge;
    @MockBean CurrentUserProvider currentUserProvider;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User("abhi", "x"); user.setId(1);
        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
        when(transactionService.findAll()).thenReturn(List.of());
    }

    @Test
    @WithMockUser(username = "abhi")
    void categorize_returnsMatch() throws Exception {
        when(autoCategorization.suggest(anyString(), any()))
                .thenReturn(new AutoCategorizationService.Suggestion(7L, "Food", 0.87, "stub"));

        mvc.perform(post("/api/v1/ai/categorize")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"description\":\"Lunch at office canteen\",\"amount\":250.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(true))
                .andExpect(jsonPath("$.categoryName").value("Food"))
                .andExpect(jsonPath("$.confidence").value(0.87))
                .andExpect(jsonPath("$.provider").value("stub"));
    }

    @Test
    @WithMockUser(username = "abhi")
    void categorize_returnsNoMatchWhenAiReturnsNull() throws Exception {
        when(autoCategorization.suggest(anyString(), any())).thenReturn(null);

        mvc.perform(post("/api/v1/ai/categorize")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"description\":\"???\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.matched").value(false));
    }

    @Test
    @WithMockUser(username = "abhi")
    void categorize_blankDescription_returns400() throws Exception {
        mvc.perform(post("/api/v1/ai/categorize")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"description\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("description"));
    }

    @Test
    @WithMockUser(username = "abhi")
    void anomalies_returnsServiceOutput() throws Exception {
        AnomalyDetectionService.Anomaly a = new AnomalyDetectionService.Anomaly(
                1L, "Big dinner", LocalDate.now(),
                new BigDecimal("2500.00"), "Food",
                new BigDecimal("300.00"), new BigDecimal("50.00"),
                44.0, AnomalyDetectionService.Severity.HIGH);
        when(anomalyDetection.detect(any(), any())).thenReturn(List.of(a));

        mvc.perform(get("/api/v1/ai/anomalies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Big dinner"))
                .andExpect(jsonPath("$[0].severity").value("HIGH"));
    }

    @Test
    @WithMockUser(username = "abhi")
    void forecast_returnsProjection() throws Exception {
        CashflowForecastService.Forecast f = new CashflowForecastService.Forecast(
                new BigDecimal("30000"), new BigDecimal("15000"),
                new BigDecimal("15000"), new BigDecimal("25000"), List.of());
        when(cashflowForecast.project(any(), any(), any(), any(LocalDate.class), any(int.class)))
                .thenReturn(f);
        when(recurringBridge.listActive(user)).thenReturn(List.of());

        mvc.perform(get("/api/v1/ai/forecast?days=30&currentBalance=10000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentMonthlyIncomeAvg").value(30000))
                .andExpect(jsonPath("$.currentMonthlyExpenseAvg").value(15000));
    }

    @Test
    @WithMockUser(username = "abhi")
    void insights_returnsServiceOutput() throws Exception {
        when(insights.generate(any(), any())).thenReturn(List.of(
                new InsightsService.Insight("welcome", "Welcome!"),
                new InsightsService.Insight("anomaly", "Big spend!", "HIGH")));

        mvc.perform(get("/api/v1/ai/insights"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].kind").value("welcome"))
                .andExpect(jsonPath("$[1].severity").value("HIGH"));
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/v1/ai/insights"))
                .andExpect(status().isUnauthorized());
    }
}