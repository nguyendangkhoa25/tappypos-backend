package com.knp.service.product;

import com.knp.model.dto.category.CategoryDTO;
import com.knp.model.dto.category.CreateCategoryRequest;
import com.knp.model.dto.category.UpdateCategoryRequest;

import java.util.List;

public interface CategoryService {

    List<CategoryDTO> getAllCategories();

    CategoryDTO getCategoryById(Long id);

    CategoryDTO createCategory(CreateCategoryRequest request);

    CategoryDTO updateCategory(Long id, UpdateCategoryRequest request);

    void deleteCategory(Long id);

    List<CategoryDTO> getSubcategories(Long parentId);
}
