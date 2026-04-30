package com.knp.service.product;

import com.knp.exception.ResourceNotFoundException;
import com.knp.model.dto.category.CategoryDTO;
import com.knp.model.dto.category.CreateCategoryRequest;
import com.knp.model.dto.category.UpdateCategoryRequest;
import com.knp.model.entity.product.Category;
import com.knp.model.entity.product.Product;
import com.knp.repository.product.CategoryRepository;
import com.knp.service.MessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryServiceImpl Unit Tests")
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category rootCategory;
    private Category childCategory;

    @BeforeEach
    void setUp() {
        rootCategory = Category.builder()
                .name("Vàng")
                .deleted(false)
                .build();
        rootCategory.setId(1L);
        rootCategory.setProducts(new HashSet<>());

        childCategory = Category.builder()
                .name("Vàng 24K")
                .deleted(false)
                .build();
        childCategory.setId(2L);
        childCategory.setParent(rootCategory);
        childCategory.setProducts(new HashSet<>());

        lenient().when(messageService.getMessage(anyString(), any(Object[].class)))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageService.getMessage(anyString()))
                .thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageService.getMessage(eq("error.category.cannotBeSelfParent")))
                .thenReturn("A category cannot be its own parent.");
        lenient().when(messageService.getMessage(eq("error.category.hasSubcategories")))
                .thenReturn("Cannot delete category with subcategories.");
        lenient().when(messageService.getMessage(eq("error.category.hasProducts")))
                .thenReturn("Cannot delete category assigned to products.");
    }

    // ── getAllCategories ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return all non-deleted categories ordered by name")
    void testGetAllCategories_ReturnsList() {
        when(categoryRepository.findByDeletedFalseOrderByName())
                .thenReturn(List.of(rootCategory, childCategory));

        List<CategoryDTO> result = categoryService.getAllCategories();

        assertThat(result).hasSize(2);
        verify(categoryRepository, times(2)).findByParentIdAndDeletedFalse(any());
    }

    @Test
    @DisplayName("Should return empty list when no categories exist")
    void testGetAllCategories_Empty() {
        when(categoryRepository.findByDeletedFalseOrderByName()).thenReturn(Collections.emptyList());

        List<CategoryDTO> result = categoryService.getAllCategories();

        assertThat(result).isEmpty();
    }

    // ── getCategoryById ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return category DTO by id")
    void testGetCategoryById_Success() {
        when(categoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.findByParentIdAndDeletedFalse(1L)).thenReturn(Collections.emptyList());

        CategoryDTO result = categoryService.getCategoryById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Vàng");
    }

    @Test
    @DisplayName("Should include parentId and parentName when category has parent")
    void testGetCategoryById_IncludesParentInfo() {
        when(categoryRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(childCategory));
        when(categoryRepository.findByParentIdAndDeletedFalse(2L)).thenReturn(Collections.emptyList());

        CategoryDTO result = categoryService.getCategoryById(2L);

        assertThat(result.getParentId()).isEqualTo(1L);
        assertThat(result.getParentName()).isEqualTo("Vàng");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for missing id")
    void testGetCategoryById_NotFound() {
        when(categoryRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getCategoryById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── createCategory ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should create root category without parent")
    void testCreateCategory_WithoutParent() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setName("Kim Cương");

        when(categoryRepository.save(any(Category.class))).thenReturn(rootCategory);
        when(categoryRepository.findByParentIdAndDeletedFalse(any())).thenReturn(Collections.emptyList());

        CategoryDTO result = categoryService.createCategory(req);

        assertThat(result).isNotNull();
        verify(categoryRepository).save(any(Category.class));
        verify(categoryRepository, never()).findByIdAndDeletedFalse(any());
    }

    @Test
    @DisplayName("Should create sub-category with valid parent")
    void testCreateCategory_WithParent() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setName("Vàng 18K");
        req.setParentId(1L);

        when(categoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(childCategory);
        when(categoryRepository.findByParentIdAndDeletedFalse(childCategory.getId()))
                .thenReturn(Collections.emptyList());

        CategoryDTO result = categoryService.createCategory(req);

        assertThat(result).isNotNull();
        verify(categoryRepository).findByIdAndDeletedFalse(1L);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when parent not found")
    void testCreateCategory_ParentNotFound() {
        CreateCategoryRequest req = new CreateCategoryRequest();
        req.setName("Sub");
        req.setParentId(99L);

        when(categoryRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.createCategory(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Parent category not found");
    }

    // ── updateCategory ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should update category name")
    void testUpdateCategory_Success() {
        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setName("Vàng Nhẫn");

        when(categoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(rootCategory);
        when(categoryRepository.findByParentIdAndDeletedFalse(1L)).thenReturn(Collections.emptyList());

        CategoryDTO result = categoryService.updateCategory(1L, req);

        assertThat(result).isNotNull();
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when parentId equals own id")
    void testUpdateCategory_SelfParent() {
        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setName("Vàng");
        req.setParentId(1L);

        when(categoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(rootCategory));

        assertThatThrownBy(() -> categoryService.updateCategory(1L, req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be its own parent");
    }

    @Test
    @DisplayName("Should remove parent when parentId is null in update")
    void testUpdateCategory_RemoveParent() {
        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setName("Vàng 18K");
        req.setParentId(null);

        when(categoryRepository.findByIdAndDeletedFalse(2L)).thenReturn(Optional.of(childCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(childCategory);
        when(categoryRepository.findByParentIdAndDeletedFalse(2L)).thenReturn(Collections.emptyList());

        categoryService.updateCategory(2L, req);

        verify(categoryRepository).save(argThat(c -> c.getParent() == null));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when updating non-existent category")
    void testUpdateCategory_NotFound() {
        UpdateCategoryRequest req = new UpdateCategoryRequest();
        req.setName("X");

        when(categoryRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(99L, req))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── deleteCategory ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should soft-delete category with no children and no products")
    void testDeleteCategory_Success() {
        when(categoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.findByParentIdAndDeletedFalse(1L)).thenReturn(Collections.emptyList());
        when(categoryRepository.save(any(Category.class))).thenReturn(rootCategory);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("Should throw IllegalStateException when category has children")
    void testDeleteCategory_HasChildren() {
        when(categoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.findByParentIdAndDeletedFalse(1L)).thenReturn(List.of(childCategory));

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("subcategories");
    }

    @Test
    @DisplayName("Should throw IllegalStateException when category has products")
    void testDeleteCategory_HasProducts() {
        Product product = mock(Product.class);
        Set<Product> products = new HashSet<>();
        products.add(product);
        rootCategory.setProducts(products);

        when(categoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.findByParentIdAndDeletedFalse(1L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("products");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when deleting non-existent category")
    void testDeleteCategory_NotFound() {
        when(categoryRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getSubcategories ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Should return subcategories for a valid parent")
    void testGetSubcategories_Success() {
        when(categoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.findByParentIdAndDeletedFalse(1L)).thenReturn(List.of(childCategory));
        when(categoryRepository.findByParentIdAndDeletedFalse(2L)).thenReturn(Collections.emptyList());

        List<CategoryDTO> result = categoryService.getSubcategories(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Vàng 24K");
    }

    @Test
    @DisplayName("Should return empty list when parent has no subcategories")
    void testGetSubcategories_Empty() {
        when(categoryRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(rootCategory));
        when(categoryRepository.findByParentIdAndDeletedFalse(1L)).thenReturn(Collections.emptyList());

        List<CategoryDTO> result = categoryService.getSubcategories(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for non-existent parent")
    void testGetSubcategories_ParentNotFound() {
        when(categoryRepository.findByIdAndDeletedFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getSubcategories(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
