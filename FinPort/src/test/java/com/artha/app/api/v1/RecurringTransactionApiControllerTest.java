package com.artha.app.api.v1;

import com.artha.app.api.CurrentUserProvider;
import com.artha.app.models.Category;
import com.artha.app.models.RecurringTransaction;
import com.artha.app.models.Transaction;
import com.artha.app.models.User;
import com.artha.app.services.CategoryService;
import com.artha.app.services.RecurringTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

@WebMvcTest(controllers = RecurringTransactionApiController.class)
@ActiveProfiles("test")
class RecurringTransactionApiControllerTest {

    @Autowired MockMvc mvc;

    @MockBean RecurringTransactionService service;
    @MockBean CategoryService categoryService;
    @MockBean CurrentUserProvider currentUserProvider;

    private User user;
    private Category food;

    @BeforeEach
    void setUp() {
        user = new User("abhi", "x"); user.setId(1);
        food = new Category("Food"); food.setId(7L);
        when(currentUserProvider.requireCurrentUser()).thenReturn(user);
    }

    private RecurringTransaction rule(Long id, String amount, LocalDate startDate) {
        RecurringTransaction r = new RecurringTransaction();
        r.setId(id);
        r.setUser(user);
        r.setCategory(food);
        r.setAmount(new BigDecimal(amount));
        r.setDescription("Netflix");
        r.setTransactionType(Transaction.TransactionType.EXPENSE);
        r.setCadence(com.artha.app.models.Cadence.MONTHLY);
        r.setStartDate(startDate);
        r.setDayOfMonth(15);
        r.setActive(true);
        return r;
    }

    @Test
    @WithMockUser(username = "abhi")
    void list_returnsCurrentUserRules() throws Exception {
        when(service.findByUser(user))
                .thenReturn(List.of(rule(1L, "649.00", LocalDate.of(2025, 1, 15))));

        mvc.perform(get("/api/v1/recurring-transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].description").value("Netflix"))
                .andExpect(jsonPath("$[0].amount").value(649.0))
                .andExpect(jsonPath("$[0].cadence").value("MONTHLY"))
                .andExpect(jsonPath("$[0].categoryId").value(7));
    }

    @Test
    @WithMockUser(username = "abhi")
    void create_valid_returns201() throws Exception {
        when(categoryService.getCategoryById(7L)).thenReturn(food);
        when(service.save(any(RecurringTransaction.class))).thenAnswer(inv -> {
            RecurringTransaction r = inv.getArgument(0);
            r.setId(99L);
            return r;
        });

        String body = """
                {
                  "categoryId": 7,
                  "amount": 199.00,
                  "description": "Spotify",
                  "transactionType": "EXPENSE",
                  "cadence": "MONTHLY",
                  "startDate": "2026-07-01",
                  "dayOfMonth": 1,
                  "active": true
                }
                """;

        mvc.perform(post("/api/v1/recurring-transactions")
                        .with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.cadence").value("MONTHLY"))
                .andExpect(jsonPath("$.dayOfMonth").value(1));
    }

    @Test
    @WithMockUser(username = "abhi")
    void create_missingCategory_returns404() throws Exception {
        when(categoryService.getCategoryById(99L)).thenReturn(null);

        String body = """
                {
                  "categoryId": 99,
                  "amount": 1,
                  "description": "x",
                  "transactionType": "EXPENSE",
                  "cadence": "DAILY",
                  "startDate": "2026-07-01"
                }
                """;

        mvc.perform(post("/api/v1/recurring-transactions")
                        .with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "abhi")
    void update_otherUsersRule_returns404() throws Exception {
        when(service.getByIdAndUser(42L, user)).thenReturn(null);

        String body = """
                {
                  "categoryId": 7,
                  "amount": 1,
                  "description": "x",
                  "transactionType": "EXPENSE",
                  "cadence": "DAILY",
                  "startDate": "2026-07-01"
                }
                """;

        mvc.perform(put("/api/v1/recurring-transactions/42")
                        .with(csrf())
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "abhi")
    void delete_returns204() throws Exception {
        when(service.getByIdAndUser(1L, user)).thenReturn(rule(1L, "100", LocalDate.now()));

        mvc.perform(delete("/api/v1/recurring-transactions/1").with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "abhi")
    void runNow_returnsTransactionMetadata() throws Exception {
        RecurringTransaction r = rule(1L, "649", LocalDate.now());
        Transaction tx = new Transaction();
        tx.setId(777L);
        tx.setDate(LocalDate.now());
        tx.setAmount(649.0);
        when(service.getByIdAndUser(1L, user)).thenReturn(r);
        when(service.runNow(r)).thenReturn(tx);

        mvc.perform(post("/api/v1/recurring-transactions/1/run-now").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(777))
                .andExpect(jsonPath("$.amount").value(649));
    }

    @Test
    @WithMockUser(username = "abhi")
    void processDue_returnsCount() throws Exception {
        when(service.processDue(any(LocalDate.class))).thenReturn(List.of(new Transaction(), new Transaction(), new Transaction()));

        mvc.perform(post("/api/v1/recurring-transactions/process-due").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generated").value(3));
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/v1/recurring-transactions"))
                .andExpect(status().isUnauthorized());
    }
}