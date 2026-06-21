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
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.config.AuthContext;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final ActivityLogService activityLogService;
    private final AuthContext authContext;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getAllCategories() {
        log.info("Getting all categories");

        // Compute this-month date range for revenue stats.
        LocalDate today = LocalDate.now();
        LocalDateTime fromDt = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime toDt   = today.withDayOfMonth(1).plusMonths(1).atStartOfDay();

        // Single query: all category stats in one round-trip.
        Map<Long, Object[]> statsMap = categoryRepository.findAllCategoryStats(fromDt, toDt)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).longValue(),
                        row -> row
                ));

        return categoryRepository.findByDeletedFalseOrderByName()
                .stream()
                .map(c -> mapToDTO(c, statsMap))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDTO getCategoryById(Long id) {
        log.info("Getting category by id: {}", id);
        Category category = categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.category.not.found", id)));
        return mapToDTO(category, null);
    }

    @Override
    public CategoryDTO createCategory(CreateCategoryRequest request) {
        log.info("Creating category: {}", request.getName());

        Category category = Category.builder()
                .tenantId(tenantContext.getCurrentTenantId())
                .name(request.getName())
                .emoji(request.getEmoji() != null && !request.getEmoji().isBlank() ? request.getEmoji() : "📦")
                .deleted(false)
                .build();

        if (request.getParentId() != null) {
            Category parent = categoryRepository.findByIdAndDeletedFalse(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.category.parent.not.found", request.getParentId())));
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        log.info("Category created: {} (id={})", saved.getName(), saved.getId());
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.CATEGORY_CREATED, "CATEGORY", String.valueOf(saved.getId()),
                "Tạo danh mục " + saved.getName(), null);
        return mapToDTO(saved, null);
    }

    @Override
    public CategoryDTO updateCategory(Long id, UpdateCategoryRequest request) {
        log.info("Updating category: {}", id);

        Category category = categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.category.not.found", id)));

        String oldName = category.getName();
        category.setName(request.getName());

        if (request.getEmoji() != null && !request.getEmoji().isBlank()) {
            category.setEmoji(request.getEmoji());
        }

        if (request.getParentId() != null) {
            if (request.getParentId().equals(id)) {
                throw new IllegalArgumentException(messageService.getMessage("error.category.cannotBeSelfParent"));
            }
            Category parent = categoryRepository.findByIdAndDeletedFalse(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.category.parent.not.found", request.getParentId())));
            category.setParent(parent);
        } else {
            category.setParent(null);
        }

        Category updated = categoryRepository.save(category);

        if (!oldName.equals(request.getName())) {
            syncGoldPriceLabel(updated);
        }

        log.info("Category updated: {} (id={})", updated.getName(), updated.getId());
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.CATEGORY_UPDATED, "CATEGORY", String.valueOf(updated.getId()),
                "Cập nhật danh mục " + updated.getName(), null);
        return mapToDTO(updated, null);
    }

    @Override
    public void deleteCategory(Long id) {
        log.info("Deleting category: {}", id);

        Category category = categoryRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.category.not.found", id)));

        List<Category> children = categoryRepository.findByParentIdAndDeletedFalse(id);
        if (!children.isEmpty()) {
            throw new IllegalStateException(messageService.getMessage("error.category.hasSubcategories"));
        }

        if (!category.getProducts().isEmpty()) {
            throw new IllegalStateException(messageService.getMessage("error.category.hasProducts"));
        }

        goldPriceRepository.findByCategoryIdAndDeletedFalse(id).ifPresent(price -> {
            price.softDelete();
            goldPriceRepository.save(price);
            log.info("Gold price {} soft-deleted because category {} was deleted", price.getId(), id);
        });

        category.softDelete();
        categoryRepository.save(category);
        log.info("Category deleted: {} (id={})", category.getName(), id);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), authContext.getCurrentUsername(), null,
                ActivityAction.CATEGORY_DELETED, "CATEGORY", String.valueOf(category.getId()),
                "Xóa danh mục " + category.getName(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDTO> getSubcategories(Long parentId) {
        log.info("Getting subcategories for parent: {}", parentId);
        categoryRepository.findByIdAndDeletedFalse(parentId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.category.not.found", parentId)));
        return categoryRepository.findByParentIdAndDeletedFalse(parentId)
                .stream()
                .map(c -> mapToDTO(c, null))
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void syncGoldPriceLabel(Category category) {
        goldPriceRepository.findByCategoryIdAndDeletedFalse(category.getId()).ifPresent(price -> {
            String parentName = category.getParent() != null ? category.getParent().getName() : "";
            price.setCode(category.getName());
            price.setLabel(parentName.isBlank() ? category.getName() : parentName + " " + category.getName());
            goldPriceRepository.save(price);
            log.info("Gold price {} label synced after category {} rename", price.getId(), category.getId());
        });
    }

    /**
     * @param statsMap  Map from category ID → Object[] row from findAllCategoryStats.
     *                  Pass null (e.g. getCategoryById, create, update) to skip stats.
     */
    private CategoryDTO mapToDTO(Category category, Map<Long, Object[]> statsMap) {
        int childCount = categoryRepository.findByParentIdAndDeletedFalse(category.getId()).size();

        int productCount     = 0;
        int outOfStockCount  = 0;
        double revenueThisMonth = 0.0;

        if (statsMap != null) {
            Object[] row = statsMap.get(category.getId());
            if (row != null) {
                productCount     = row[1] != null ? ((Number) row[1]).intValue()    : 0;
                outOfStockCount  = row[2] != null ? ((Number) row[2]).intValue()    : 0;
                revenueThisMonth = row[3] != null ? ((Number) row[3]).doubleValue() : 0.0;
            }
        }

        return CategoryDTO.builder()
                .id(category.getId())
                .name(category.getName())
                .emoji(category.getEmoji() != null ? category.getEmoji() : "📦")
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .childCount(childCount)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .productCount(productCount)
                .outOfStockCount(outOfStockCount)
                .revenueThisMonth(revenueThisMonth)
                .build();
    }
}
