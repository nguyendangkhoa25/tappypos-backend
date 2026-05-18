package com.tappy.pos.service.product;

import com.tappy.pos.model.dto.category.CategoryDTO;
import com.tappy.pos.model.dto.category.CreateCategoryRequest;
import com.tappy.pos.model.dto.category.UpdateCategoryRequest;

import java.util.List;

public interface CategoryService {

    List<CategoryDTO> getAllCategories();

    CategoryDTO getCategoryById(Long id);

    CategoryDTO createCategory(CreateCategoryRequest request);

    CategoryDTO updateCategory(Long id, UpdateCategoryRequest request);

    void deleteCategory(Long id);

    List<CategoryDTO> getSubcategories(Long parentId);
}
