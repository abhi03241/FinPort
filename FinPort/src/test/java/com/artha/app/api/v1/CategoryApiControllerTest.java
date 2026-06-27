package com.artha.app.api.v1;

import com.artha.app.models.Category;
import com.artha.app.services.CategoryService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CategoryApiController.class)
@ActiveProfiles("test")
class CategoryApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void list_returnsPagedJson() throws Exception {
        Category c = new Category("Food");
        c.setId(1L);
        Page<Category> page = new PageImpl<>(List.of(c), PageRequest.of(0, 20), 1);
        when(categoryService.getCategories(any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].name").value("Food"))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void get_returnsCategory() throws Exception {
        Category c = new Category("Food");
        c.setId(1L);
        when(categoryService.getCategoryById(1L)).thenReturn(c);

        mockMvc.perform(get("/api/v1/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Food"));
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void get_returns404WhenMissing() throws Exception {
        when(categoryService.getCategoryById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/v1/categories/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void create_valid_returns201WithLocation() throws Exception {
        Category saved = new Category("Travel");
        saved.setId(7L);
        when(categoryService.getCategoryById(7L)).thenReturn(saved);

        // categoryService.save is void-ish in our service; we just need to capture and stub
        doAnswer(inv -> {
            Category arg = inv.getArgument(0);
            arg.setId(7L);
            return null;
        }).when(categoryService).save(any(Category.class));

        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Travel\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.name").value("Travel"));
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void create_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("name"));
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void update_missing_returns404() throws Exception {
        when(categoryService.getCategoryById(99L)).thenReturn(null);

        mockMvc.perform(put("/api/v1/categories/99")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void delete_existing_returns204() throws Exception {
        Category c = new Category("Food");
        c.setId(1L);
        when(categoryService.getCategoryById(1L)).thenReturn(c);

        mockMvc.perform(delete("/api/v1/categories/1").with(csrf()))
                .andExpect(status().isNoContent());
        verify(categoryService).deleteById(1L);
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void delete_missing_returns404() throws Exception {
        when(categoryService.getCategoryById(99L)).thenReturn(null);

        mockMvc.perform(delete("/api/v1/categories/99").with(csrf()))
                .andExpect(status().isNotFound());
        verify(categoryService, never()).deleteById(anyLong());
    }

    @Test
    void unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized());
    }
}