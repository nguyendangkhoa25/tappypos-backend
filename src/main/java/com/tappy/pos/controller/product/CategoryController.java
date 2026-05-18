package com.tappy.pos.controller.product;

import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.category.CategoryDTO;
import com.tappy.pos.model.dto.category.CreateCategoryRequest;
import com.tappy.pos.model.dto.category.UpdateCategoryRequest;
import com.tappy.pos.service.product.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.tappy.pos.annotation.RequiresFeature;

@Slf4j
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@RequiresFeature("PRODUCT")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Get all categories (flat list with parent info)
     * GET /api/categories
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getAllCategories() {
        log.info("GET /api/categories - Get all categories");
        List<CategoryDTO> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success(categories, "Categories retrieved successfully"));
    }

    /**
     * Get category by ID
     * GET /api/categories/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDTO>> getCategoryById(@PathVariable Long id) {
        log.info("GET /api/categories/{} - Get category by id", id);
        CategoryDTO category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(category, "Category retrieved successfully"));
    }

    /**
     * Create a new category
     * POST /api/categories
     */
    @PostMapping
    public ResponseEntity<ApiResponse<CategoryDTO>> createCategory(
            @RequestBody @Valid CreateCategoryRequest request) {
        log.info("POST /api/categories - Create category: {}", request.getName());
        CategoryDTO category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(category, "Category created successfully"));
    }

    /**
     * Update category
     * PUT /api/categories/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryDTO>> updateCategory(
            @PathVariable Long id,
            @RequestBody @Valid UpdateCategoryRequest request) {
        log.info("PUT /api/categories/{} - Update category", id);
        CategoryDTO category = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success(category, "Category updated successfully"));
    }

    /**
     * Delete category (soft delete)
     * DELETE /api/categories/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        log.info("DELETE /api/categories/{} - Delete category", id);
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Category deleted successfully"));
    }

    /**
     * Get subcategories of a parent category
     * GET /api/categories/{parentId}/subcategories
     */
    @GetMapping("/{parentId}/subcategories")
    public ResponseEntity<ApiResponse<List<CategoryDTO>>> getSubcategories(@PathVariable Long parentId) {
        log.info("GET /api/categories/{}/subcategories - Get subcategories", parentId);
        List<CategoryDTO> subcategories = categoryService.getSubcategories(parentId);
        return ResponseEntity.ok(ApiResponse.success(subcategories, "Subcategories retrieved successfully"));
    }
}
