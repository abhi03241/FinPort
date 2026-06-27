package com.artha.app.api.v1;

import com.artha.app.models.Category;
import com.artha.app.models.Transaction;
import com.artha.app.services.CategoryService;
import com.artha.app.services.TransactionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = TransactionApiController.class)
@ActiveProfiles("test")
class TransactionApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;

    @MockBean
    private CategoryService categoryService;

    private Category category() {
        Category c = new Category("Food");
        c.setId(1L);
        return c;
    }

    private Transaction transaction() {
        Transaction t = new Transaction();
        t.setId(1L);
        t.setAmount(250.0);
        t.setDate(LocalDate.of(2026, 6, 1));
        t.setDescription("Lunch");
        t.setTransactionType(Transaction.TransactionType.EXPENSE);
        t.setCategory(category());
        return t;
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void list_returnsPagedJson() throws Exception {
        Page<Transaction> page = new PageImpl<>(List.of(transaction()), PageRequest.of(0, 20), 1);
        when(transactionService.findTransactions(any(), any(), any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Lunch"))
                .andExpect(jsonPath("$.content[0].amount").value(250.0))
                .andExpect(jsonPath("$.content[0].transactionType").value("EXPENSE"))
                .andExpect(jsonPath("$.content[0].category.name").value("Food"));
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void get_returnsTransaction() throws Exception {
        when(transactionService.getTransactionById(1L)).thenReturn(transaction());

        mockMvc.perform(get("/api/v1/transactions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Lunch"));
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void get_missing_returns404() throws Exception {
        when(transactionService.getTransactionById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/transactions/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void create_valid_returns201() throws Exception {
        when(categoryService.getCategoryById(1L)).thenReturn(category());
        when(transactionService.save(any(Transaction.class))).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            t.setId(42L);
            return t;
        });

        String body = """
                {
                  "amount": 99.99,
                  "date": "2026-06-15",
                  "description": "Coffee",
                  "transactionType": "EXPENSE",
                  "categoryId": 1
                }
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.endsWith("/api/v1/transactions/42")))
                .andExpect(jsonPath("$.description").value("Coffee"))
                .andExpect(jsonPath("$.category.id").value(1));
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void create_unknownCategory_returns400() throws Exception {
        when(categoryService.getCategoryById(99L)).thenReturn(null);

        String body = """
                {
                  "amount": 50.0,
                  "date": "2026-06-15",
                  "description": "X",
                  "transactionType": "EXPENSE",
                  "categoryId": 99
                }
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void create_invalid_returns400WithFieldErrors() throws Exception {
        String body = """
                {
                  "amount": -1,
                  "date": null,
                  "description": "",
                  "transactionType": null,
                  "categoryId": null
                }
                """;

        mockMvc.perform(post("/api/v1/transactions")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void delete_existing_returns204() throws Exception {
        when(transactionService.getTransactionById(1L)).thenReturn(transaction());

        mockMvc.perform(delete("/api/v1/transactions/1").with(csrf()))
                .andExpect(status().isNoContent());
        verify(transactionService).deleteById(1L);
    }
}