package com.tappy.pos.service.product;

import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.category.CategoryDTO;
import com.tappy.pos.model.dto.category.CreateCategoryRequest;
import com.tappy.pos.model.dto.category.UpdateCategoryRequest;
import com.tappy.pos.model.entity.product.Category;
import com.tappy.pos.model.entity.tenant.GoldPrice;
import com.tappy.pos.repository.product.CategoryRepository;
import com.tappy.pos.repository.tenant.GoldPriceRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.multitenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final GoldPriceRepository goldPriceRepository;
    private final MessageService messageService;
    private final TenantContext tenantContext;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        log.info("Getting all categories");
        return categoryRepository.findByDeletedFalseOrderByName()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long id) {
        log.info("Getting category by id: {}", id);
        Category category = categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        return mapToDTO(category);
    }

    @Override
    public CategoryDTO createCategory(CreateCategoryRequest request) {
        log.info("Creating category: {}", request.getName());

        Category category = Category.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .name(request.getName())
                .deleted(false)
                .build();

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findByIdAndDeletedFalse(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found: " + request.getParentId()));
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        log.info("Category created: {} (id={})", saved.getName(), saved.getId());
        return mapToDTO(saved);
    }

    @Override
    public CategoryDTO updateCategory(Long id, UpdateCategoryRequest request) {
        log.info("Updating category: {}", id);

        Category category = categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        String oldName = category.getName();
        category.setName(request.getName());

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException(messageService.getMessage("error.category.cannotBeSelfParent"));
            }
            Category parent = categoryRepository.findByIdAndDeletedFalse(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found: " + request.getParentId()));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        Category updated = categoryRepository.save(category);

        // Keep gold_price denormalized cache in sync when name changes.
        if (!oldName.equals(request.getName())) {
            syncGoldPriceLabel(updated);
        }

        log.info("Category updated: {} (id={})", updated.getName(), updated.getId());
        return mapToDTO(updated);
    }

    @Override
    public void deleteCategory(Long id) {
        log.info("Deleting category: {}", id);

        Category category = categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        List<Category> children = categoryRepository.findByParentIdAndDeletedFalse(id);
        if (!children.isEmpty()) {
            throw new IllegalStateException(messageService.getMessage("error.category.hasSubcategories"));
        }

        if (!category.getProducts().isEmpty()) {
            throw new IllegalStateException(messageService.getMessage("error.category.hasProducts"));
        }

        // Soft-delete any linked gold price so it doesn't become a ghost record.
        goldPriceRepository.findByCategoryIdAndDeletedFalse(id).ifPresent(price -> {
            price.softDelete();
            goldPriceRepository.save(price);
            log.info("Gold price {} soft-deleted because category {} was deleted", price.getId(), id);
        });

        category.softDelete();
        categoryRepository.save(category);
        log.info("Category deleted: {} (id={})", category.getName(), id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getSubcategories(Long parentId) {
        log.info("Getting subcategories for parent: {}", parentId);
        categoryRepository.findByIdAndDeletedFalse(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + parentId));
        return categoryRepository.findByParentIdAndDeletedFalse(parentId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    private void syncGoldPriceLabel(Category category) {
        goldPriceRepository.findByCategoryIdAndDeletedFalse(category.getId()).ifPresent(price -> {
            String parentName = category.getParent() != null ? category.getParent().getName() : "";
            price.setCode(category.getName());
            price.setLabel(parentName.isBlank() ? category.getName() : parentName + " " + category.getName());
            goldPriceRepository.save(price);
            log.info("Gold price {} label synced after category {} rename", price.getId(), category.getId());
        });
    }

    private CategoryDTO mapToDTO(Category category) {
        int childCount = categoryRepository.findByParentIdAndDeletedFalse(category.getId()).size();
        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .childCount(childCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
