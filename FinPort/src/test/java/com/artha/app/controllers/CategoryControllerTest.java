package com.artha.app.controllers;

import com.artha.app.models.Category;
import com.artha.app.services.CategoryService;
import com.artha.app.testsupport.SecurityTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CategoryController.class)
@Import(SecurityTestSupport.class)
@ActiveProfiles("test")
class CategoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CategoryService categoryService;

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void listCategories_returnsCategoriesView() throws Exception {
        Category food = new Category("Food");
        food.setId(1L);
        Page<Category> page = new PageImpl<>(List.of(food), PageRequest.of(0, 10), 1);
        when(categoryService.getCategories(any(), any())).thenReturn(page);

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(view().name("categories"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("requestURI"));
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void showAddCategoryForm_returnsCreateView() throws Exception {
        mockMvc.perform(get("/categories/add"))
                .andExpect(status().isOk())
                .andExpect(view().name("create-new-category"))
                .andExpect(model().attributeExists("category"));
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void saveCategory_validInput_redirectsToList() throws Exception {
        mockMvc.perform(post("/categories/add")
                        .with(csrf())
                        .param("name", "Food"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void saveCategory_blankName_returnsFormWithErrors() throws Exception {
        mockMvc.perform(post("/categories/add")
                        .with(csrf())
                        .param("name", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("create-new-category"))
                .andExpect(model().attributeHasFieldErrors("category", "name"));
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void editCategory_notFound_redirectsToList() throws Exception {
        when(categoryService.getCategoryById(99L)).thenReturn(null);

        mockMvc.perform(get("/categories/edit/99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/categories"));
    }

    @Test
    @WithMockUser(username = "abhi", roles = "USER")
    void editCategory_found_returnsEditView() throws Exception {
        Category food = new Category("Food");
        food.setId(1L);
        when(categoryService.getCategoryById(1L)).thenReturn(food);

        mockMvc.perform(get("/categories/edit/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-category"))
                .andExpect(model().attribute("category", food));
    }
}