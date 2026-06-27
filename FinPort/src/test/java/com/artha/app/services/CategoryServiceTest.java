package com.artha.app.services;

import com.artha.app.models.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category food;
    private Category travel;

    @BeforeEach
    void setUp() {
        food = new Category("Food");
        food.setId(1L);
        travel = new Category("Travel");
        travel.setId(2L);
    }

    @Test
    @DisplayName("getCategories with no filter returns all categories paginated")
    void getCategories_noFilter_returnsAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Category> page = new PageImpl<>(List.of(food, travel), pageable, 2);
        when(categoryRepository.findAll(pageable)).thenReturn(page);

        Page<Category> result = categoryService.getCategories(null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).extracting(Category::getName)
                .containsExactly("Food", "Travel");
        verify(categoryRepository).findAll(pageable);
        verify(categoryRepository, never()).findByNameContainingIgnoreCase(any(), any());
    }

    @Test
    @DisplayName("getCategories with whitespace filter delegates to name search")
    void getCategories_blankFilter_usesNameSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Category> page = new PageImpl<>(List.of(food), pageable, 1);
        when(categoryRepository.findByNameContainingIgnoreCase("   ", pageable)).thenReturn(page);

        Page<Category> result = categoryService.getCategories("   ", pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(categoryRepository).findByNameContainingIgnoreCase("   ", pageable);
        verify(categoryRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("getCategories with name filter delegates to findByNameContainingIgnoreCase")
    void getCategories_withFilter_usesNameSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Category> page = new PageImpl<>(List.of(food), pageable, 1);
        when(categoryRepository.findByNameContainingIgnoreCase("foo", pageable)).thenReturn(page);

        Page<Category> result = categoryService.getCategories("foo", pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(categoryRepository).findByNameContainingIgnoreCase("foo", pageable);
    }

    @Test
    @DisplayName("save persists the category")
    void save_persistsCategory() {
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);

        categoryService.save(food);

        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(food);
    }

    @Test
    @DisplayName("getCategoryById returns category when found")
    void getCategoryById_found() {
        when(categoryRepository.findById(1L)).thenReturn(java.util.Optional.of(food));

        Category result = categoryService.getCategoryById(1L);

        assertThat(result).isNotNull().isSameAs(food);
    }

    @Test
    @DisplayName("getCategoryById returns null when not found")
    void getCategoryById_notFound() {
        when(categoryRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        Category result = categoryService.getCategoryById(99L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("getAllCategories returns every category")
    void getAllCategories_returnsAll() {
        when(categoryRepository.findAll()).thenReturn(List.of(food, travel));

        List<Category> result = categoryService.getAllCategories();

        assertThat(result).hasSize(2);
    }
}