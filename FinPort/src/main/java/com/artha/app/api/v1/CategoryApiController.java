package com.artha.app.api.v1;

import com.artha.app.api.dto.CategoryRequest;
import com.artha.app.api.dto.CategoryResponse;
import com.artha.app.api.dto.PageResponse;
import com.artha.app.models.Category;
import com.artha.app.services.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Categories", description = "Manage transaction categories")
public class CategoryApiController {

    private final CategoryService categoryService;

    public CategoryApiController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @Operation(summary = "List categories (paged, optionally filtered by name)")
    public PageResponse<CategoryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String name) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<Category> result = categoryService.getCategories(name, pageable);
        return PageResponse.from(result, CategoryResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single category by id")
    public ResponseEntity<CategoryResponse> get(@PathVariable Long id) {
        Category c = categoryService.getCategoryById(id);
        return c != null
                ? ResponseEntity.ok(CategoryResponse.from(c))
                : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "Create a new category")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest req,
                                                   UriComponentsBuilder uriBuilder) {
        Category c = new Category(req.name());
        categoryService.save(c);
        URI location = uriBuilder.path("/api/v1/categories/{id}").buildAndExpand(c.getId()).toUri();
        return ResponseEntity.created(location).body(CategoryResponse.from(c));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an existing category")
    public ResponseEntity<CategoryResponse> update(@PathVariable Long id,
                                                   @Valid @RequestBody CategoryRequest req) {
        Category existing = categoryService.getCategoryById(id);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        existing.setName(req.name());
        categoryService.save(existing);
        return ResponseEntity.ok(CategoryResponse.from(existing));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a category")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (categoryService.getCategoryById(id) == null) {
            return ResponseEntity.notFound().build();
        }
        categoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}