package com.tappy.pos.service.recipe;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.recipe.RecipeCostDTO;
import com.tappy.pos.model.dto.recipe.RecipeDTO;
import com.tappy.pos.model.dto.recipe.RecipeItemDTO;
import com.tappy.pos.model.dto.recipe.SaveRecipeRequest;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.recipe.Recipe;
import com.tappy.pos.model.entity.recipe.RecipeItem;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.recipe.RecipeItemRepository;
import com.tappy.pos.repository.recipe.RecipeRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeServiceImpl implements RecipeService {

    private final RecipeRepository recipeRepository;
    private final RecipeItemRepository recipeItemRepository;
    private final ProductRepository productRepository;
    private final MessageService messageService;
    private final ActivityLogService activityLogService;
    private final TenantContext tenantContext;

    private static final BigDecimal DEFAULT_MARGIN = new BigDecimal("0.30");

    @Override
    @Transactional
    public RecipeDTO saveRecipe(SaveRecipeRequest request) {
        Product finished = productRepository.findByIdAndDeletedFalse(request.getFinishedProductId())
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.product.not.found", request.getFinishedProductId())));

        String tenantId = tenantContext.getCurrentTenantId();
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();

        BigDecimal yield = request.getYieldQuantity() != null && request.getYieldQuantity().signum() > 0
                ? request.getYieldQuantity() : BigDecimal.ONE;

        Recipe recipe = recipeRepository.findByFinishedProductIdAndDeletedFalse(request.getFinishedProductId())
                .orElse(null);
        boolean isNew = recipe == null;
        if (isNew) {
            recipe = Recipe.builder()
                    .tenantId(tenantId)
                    .finishedProductId(request.getFinishedProductId())
                    .createdBy(actor)
                    .build();
        }
        recipe.setYieldQuantity(yield);
        recipe.setLaborCost(nz(request.getLaborCost()));
        recipe.setOverheadCost(nz(request.getOverheadCost()));
        recipe.setNotes(request.getNotes());
        Recipe saved = recipeRepository.save(recipe);

        // Replace items: soft-delete existing, insert provided.
        recipeItemRepository.softDeleteByRecipeId(saved.getId());
        List<RecipeItem> newItems = new ArrayList<>();
        if (request.getItems() != null) {
            for (SaveRecipeRequest.Item it : request.getItems()) {
                if (it.getIngredientProductId() == null || it.getQuantity() == null
                        || it.getQuantity().signum() <= 0) continue;
                if (it.getIngredientProductId().equals(request.getFinishedProductId())) {
                    throw new BadRequestException(messageService.getMessage("error.recipe.self.ingredient"));
                }
                RecipeItem ri = RecipeItem.builder()
                        .tenantId(tenantId)
                        .recipeId(saved.getId())
                        .ingredientProductId(it.getIngredientProductId())
                        .quantity(it.getQuantity())
                        .unit(it.getUnit())
                        .build();
                newItems.add(recipeItemRepository.save(ri));
            }
        }

        activityLogService.logAsync(tenantId, actor, null,
                isNew ? ActivityAction.RECIPE_CREATED : ActivityAction.RECIPE_UPDATED,
                "RECIPE", String.valueOf(saved.getId()),
                isNew ? "activity.recipe.created" : "activity.recipe.updated", null, finished.getName());

        return buildRecipeDTO(saved, newItems, finished);
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeDTO getRecipeById(Long id) {
        Recipe recipe = recipeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.recipe.not.found")));
        return buildRecipeDTO(recipe, recipeItemRepository.findByRecipeIdAndDeletedFalse(id), null);
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeDTO getRecipeByProduct(Long finishedProductId) {
        Recipe recipe = recipeRepository.findByFinishedProductIdAndDeletedFalse(finishedProductId).orElse(null);
        if (recipe == null) return null;
        return buildRecipeDTO(recipe, recipeItemRepository.findByRecipeIdAndDeletedFalse(recipe.getId()), null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RecipeDTO> listRecipes(Pageable pageable) {
        return recipeRepository.findByDeletedFalseOrderByUpdatedAtDesc(pageable)
                .map(r -> buildRecipeDTO(r, recipeItemRepository.findByRecipeIdAndDeletedFalse(r.getId()), null));
    }

    @Override
    @Transactional
    public void deleteRecipe(Long id) {
        Recipe recipe = recipeRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.recipe.not.found")));
        recipe.softDelete();
        recipeRepository.save(recipe);
        recipeItemRepository.softDeleteByRecipeId(id);
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.RECIPE_DELETED, "RECIPE", String.valueOf(id),
                "activity.recipe.deleted", null, String.valueOf(id));
    }

    @Override
    @Transactional(readOnly = true)
    public RecipeCostDTO getCost(Long recipeId, BigDecimal marginPct) {
        Recipe recipe = recipeRepository.findByIdAndDeletedFalse(recipeId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.recipe.not.found")));
        List<RecipeItem> items = recipeItemRepository.findByRecipeIdAndDeletedFalse(recipeId);
        Product finished = productRepository.findByIdAndDeletedFalse(recipe.getFinishedProductId()).orElse(null);

        CostResult cr = computeCost(recipe, items);
        BigDecimal margin = (marginPct != null && marginPct.signum() > 0 && marginPct.compareTo(BigDecimal.ONE) < 0)
                ? marginPct : DEFAULT_MARGIN;
        BigDecimal suggested = cr.unitCost.divide(BigDecimal.ONE.subtract(margin), 0, RoundingMode.HALF_UP);

        BigDecimal sell = finished != null && finished.getPrice() != null ? finished.getPrice() : BigDecimal.ZERO;
        BigDecimal grossMargin = sell.signum() > 0
                ? sell.subtract(cr.unitCost).divide(sell, 4, RoundingMode.HALF_UP).multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;
        BigDecimal storedCost = finished != null && finished.getCostPrice() != null ? finished.getCostPrice() : BigDecimal.ZERO;
        boolean needsReprice = cr.unitCost.compareTo(storedCost) > 0;

        return RecipeCostDTO.builder()
                .recipeId(recipeId)
                .finishedProductId(recipe.getFinishedProductId())
                .ingredientCost(cr.ingredientCost)
                .laborCost(recipe.getLaborCost())
                .overheadCost(recipe.getOverheadCost())
                .totalCost(cr.totalCost)
                .yieldQuantity(recipe.getYieldQuantity())
                .unitCost(cr.unitCost)
                .currentSellPrice(sell)
                .suggestedPrice(suggested)
                .grossMarginPct(grossMargin.setScale(1, RoundingMode.HALF_UP))
                .missingIngredientCost(cr.missingCost)
                .storedCostPrice(storedCost)
                .needsReprice(needsReprice)
                .build();
    }

    // ── internals ──────────────────────────────────────────────────────────────

    /** Cost math, reused by save/get/cost and ProductionService. */
    public CostResult computeCost(Recipe recipe, List<RecipeItem> items) {
        BigDecimal ingredientCost = BigDecimal.ZERO;
        boolean missing = false;
        List<Long> ids = items.stream().map(RecipeItem::getIngredientProductId).toList();
        var products = productRepository.findAllById(ids);
        for (RecipeItem it : items) {
            Product ing = products.stream().filter(p -> p.getId().equals(it.getIngredientProductId())).findFirst().orElse(null);
            BigDecimal unitCost = ing != null && ing.getCostPrice() != null ? ing.getCostPrice() : BigDecimal.ZERO;
            if (ing == null || ing.getCostPrice() == null || ing.getCostPrice().signum() == 0) missing = true;
            ingredientCost = ingredientCost.add(it.getQuantity().multiply(unitCost));
        }
        BigDecimal total = ingredientCost.add(nz(recipe.getLaborCost())).add(nz(recipe.getOverheadCost()));
        BigDecimal yield = recipe.getYieldQuantity() != null && recipe.getYieldQuantity().signum() > 0
                ? recipe.getYieldQuantity() : BigDecimal.ONE;
        BigDecimal unitCost = total.divide(yield, 2, RoundingMode.HALF_UP);
        return new CostResult(ingredientCost.setScale(2, RoundingMode.HALF_UP),
                total.setScale(2, RoundingMode.HALF_UP), unitCost, missing);
    }

    private RecipeDTO buildRecipeDTO(Recipe recipe, List<RecipeItem> items, Product finishedMaybe) {
        Product finished = finishedMaybe != null ? finishedMaybe
                : productRepository.findByIdAndDeletedFalse(recipe.getFinishedProductId()).orElse(null);
        var ids = items.stream().map(RecipeItem::getIngredientProductId).toList();
        var ingredients = productRepository.findAllById(ids);

        List<RecipeItemDTO> itemDTOs = new ArrayList<>();
        for (RecipeItem it : items) {
            Product ing = ingredients.stream().filter(p -> p.getId().equals(it.getIngredientProductId())).findFirst().orElse(null);
            BigDecimal unitCost = ing != null && ing.getCostPrice() != null ? ing.getCostPrice() : BigDecimal.ZERO;
            itemDTOs.add(RecipeItemDTO.builder()
                    .id(it.getId())
                    .ingredientProductId(it.getIngredientProductId())
                    .ingredientName(ing != null ? ing.getName() : null)
                    .ingredientUnit(ing != null ? ing.getUnit() : null)
                    .quantity(it.getQuantity())
                    .unit(it.getUnit())
                    .ingredientUnitCost(unitCost)
                    .lineCost(it.getQuantity().multiply(unitCost).setScale(2, RoundingMode.HALF_UP))
                    .build());
        }

        CostResult cr = computeCost(recipe, items);
        return RecipeDTO.builder()
                .id(recipe.getId())
                .finishedProductId(recipe.getFinishedProductId())
                .finishedProductName(finished != null ? finished.getName() : null)
                .yieldQuantity(recipe.getYieldQuantity())
                .laborCost(recipe.getLaborCost())
                .overheadCost(recipe.getOverheadCost())
                .notes(recipe.getNotes())
                .items(itemDTOs)
                .ingredientCost(cr.ingredientCost)
                .totalCost(cr.totalCost)
                .unitCost(cr.unitCost)
                .createdAt(recipe.getCreatedAt())
                .updatedAt(recipe.getUpdatedAt())
                .build();
    }

    private static BigDecimal nz(BigDecimal v) { return v != null ? v : BigDecimal.ZERO; }

    /** Cost breakdown result. */
    public record CostResult(BigDecimal ingredientCost, BigDecimal totalCost, BigDecimal unitCost, boolean missingCost) {}
}
