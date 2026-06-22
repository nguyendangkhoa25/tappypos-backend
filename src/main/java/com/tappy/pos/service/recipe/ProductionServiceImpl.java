package com.tappy.pos.service.recipe;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.inventory.InventoryDTO;
import com.tappy.pos.model.dto.recipe.IngredientConsumptionDTO;
import com.tappy.pos.model.dto.recipe.ProduceRequest;
import com.tappy.pos.model.dto.recipe.ProductionBatchDTO;
import com.tappy.pos.model.dto.recipe.ProductionSummaryDTO;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.recipe.ProductionBatch;
import com.tappy.pos.model.entity.recipe.Recipe;
import com.tappy.pos.model.entity.recipe.RecipeItem;
import com.tappy.pos.model.enums.ActivityAction;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.recipe.ProductionBatchRepository;
import com.tappy.pos.repository.recipe.RecipeItemRepository;
import com.tappy.pos.repository.recipe.RecipeRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.inventory.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductionServiceImpl implements ProductionService {

    private final RecipeRepository recipeRepository;
    private final RecipeItemRepository recipeItemRepository;
    private final ProductionBatchRepository batchRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final RecipeServiceImpl recipeService;   // reuse cost math
    private final MessageService messageService;
    private final ActivityLogService activityLogService;
    private final TenantContext tenantContext;

    @Override
    @Transactional
    public ProductionBatchDTO produce(ProduceRequest request) {
        if (request.getQuantity() == null || request.getQuantity().signum() <= 0) {
            throw new BadRequestException(messageService.getMessage("error.production.invalid.quantity"));
        }
        long produceQty = request.getQuantity().setScale(0, RoundingMode.HALF_UP).longValueExact();

        Product finished = productRepository.findByIdAndDeletedFalse(request.getFinishedProductId())
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.product.not.found", request.getFinishedProductId())));
        Recipe recipe = recipeRepository.findByFinishedProductIdAndDeletedFalse(request.getFinishedProductId())
                .orElseThrow(() -> new BadRequestException(messageService.getMessage("error.production.no.recipe")));
        List<RecipeItem> items = recipeItemRepository.findByRecipeIdAndDeletedFalse(recipe.getId());
        if (items.isEmpty()) {
            throw new BadRequestException(messageService.getMessage("error.production.no.recipe"));
        }

        BigDecimal yield = recipe.getYieldQuantity() != null && recipe.getYieldQuantity().signum() > 0
                ? recipe.getYieldQuantity() : BigDecimal.ONE;
        BigDecimal runs = BigDecimal.valueOf(produceQty).divide(yield, 6, RoundingMode.HALF_UP);

        // ── Validate ingredient stock; collect deductions (ceil to the stock unit, v1) ──
        record Deduction(Long inventoryId, long qty, String name) {}
        List<Deduction> deductions = new ArrayList<>();
        List<String> shortfalls = new ArrayList<>();
        for (RecipeItem it : items) {
            long required = it.getQuantity().multiply(runs).setScale(0, RoundingMode.CEILING).longValueExact();
            if (required <= 0) continue;
            Product ing = productRepository.findByIdAndDeletedFalse(it.getIngredientProductId()).orElse(null);
            String name = ing != null ? ing.getName() : ("#" + it.getIngredientProductId());
            InventoryDTO inv = firstInventory(it.getIngredientProductId());
            long available = inv != null && inv.getQuantityInStock() != null ? inv.getQuantityInStock() : 0L;
            if (inv == null || available < required) {
                shortfalls.add(name + " (cần " + required + ", còn " + available + ")");
            } else {
                deductions.add(new Deduction(inv.getId(), required, name));
            }
        }
        if (!shortfalls.isEmpty()) {
            throw new BadRequestException(
                    messageService.getMessage("error.production.insufficient") + ": " + String.join("; ", shortfalls));
        }

        // ── Finished-goods inventory row must exist to receive stock ──
        InventoryDTO finishedInv = firstInventory(finished.getId());
        if (finishedInv == null) {
            throw new BadRequestException(messageService.getMessage("error.production.no.finished.inventory"));
        }

        // ── Apply: deduct ingredients, add finished goods (one transaction) ──
        for (Deduction d : deductions) {
            inventoryService.removeStock(d.inventoryId(), d.qty());
        }
        inventoryService.addStock(finishedInv.getId(), produceQty);

        // ── Cost snapshot ──
        RecipeServiceImpl.CostResult cost = recipeService.computeCost(recipe, items);
        BigDecimal batchIngredientCost = cost.ingredientCost().multiply(runs).setScale(2, RoundingMode.HALF_UP);
        BigDecimal unitCost = cost.unitCost();

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        String tenantId = tenantContext.getCurrentTenantId();
        ProductionBatch batch = ProductionBatch.builder()
                .tenantId(tenantId)
                .finishedProductId(finished.getId())
                .recipeId(recipe.getId())
                .quantityProduced(BigDecimal.valueOf(produceQty))
                .ingredientCost(batchIngredientCost)
                .unitCost(unitCost)
                .status(ProductionBatch.Status.COMPLETED)
                .producedBy(actor)
                .notes(request.getNotes())
                .build();
        ProductionBatch saved = batchRepository.save(batch);

        // Optional re-price: update the finished product's cost_price to the produced unit cost.
        if (request.isUpdateCostPrice()) {
            finished.setCostPrice(unitCost);
            productRepository.save(finished);
        }

        activityLogService.logAsync(tenantId, actor, null,
                ActivityAction.PRODUCTION_RUN, "PRODUCTION", String.valueOf(saved.getId()),
                "activity.production.run", null, String.valueOf(produceQty),
                (finished.getUnit() != null ? finished.getUnit() : "cái"), finished.getName(), unitCost.toPlainString());

        return toDTO(saved, finished.getName());
    }

    @Override
    @Transactional
    public ProductionBatchDTO markSpoiled(Long batchId) {
        ProductionBatch batch = batchRepository.findByIdAndDeletedFalse(batchId)
                .orElseThrow(() -> new ResourceNotFoundException(messageService.getMessage("error.production.batch.not.found")));
        if (batch.getStatus() != ProductionBatch.Status.COMPLETED) {
            throw new BadRequestException(messageService.getMessage("error.production.not.completed"));
        }
        // Write off the produced finished-goods stock.
        InventoryDTO finishedInv = firstInventory(batch.getFinishedProductId());
        if (finishedInv != null) {
            long qty = batch.getQuantityProduced().setScale(0, RoundingMode.HALF_UP).longValueExact();
            long available = finishedInv.getQuantityInStock() != null ? finishedInv.getQuantityInStock() : 0L;
            inventoryService.removeStock(finishedInv.getId(), Math.min(qty, available));
        }
        batch.setStatus(ProductionBatch.Status.SPOILED);
        ProductionBatch saved = batchRepository.save(batch);

        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        Product finished = productRepository.findByIdAndDeletedFalse(batch.getFinishedProductId()).orElse(null);
        activityLogService.logAsync(tenantContext.getCurrentTenantId(), actor, null,
                ActivityAction.PRODUCTION_SPOILED, "PRODUCTION", String.valueOf(saved.getId()),
                "activity.production.spoiled", null, String.valueOf(saved.getId()),
                (finished != null ? " — " + finished.getName() : ""));
        return toDTO(saved, finished != null ? finished.getName() : null);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductionBatchDTO> listBatches(LocalDate from, LocalDate to, Pageable pageable) {
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : LocalDateTime.of(1970, 1, 1, 0, 0);
        LocalDateTime toDt = to != null ? to.atTime(23, 59, 59) : LocalDateTime.of(9999, 12, 31, 23, 59, 59);
        return batchRepository.findInRange(fromDt, toDt, pageable).map(b -> {
            Product p = productRepository.findByIdAndDeletedFalse(b.getFinishedProductId()).orElse(null);
            return toDTO(b, p != null ? p.getName() : null);
        });
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private InventoryDTO firstInventory(Long productId) {
        Page<InventoryDTO> page = inventoryService.getInventoryByProductId(productId, PageRequest.of(0, 1));
        return page.isEmpty() ? null : page.getContent().get(0);
    }

    private ProductionBatchDTO toDTO(ProductionBatch b, String finishedName) {
        return ProductionBatchDTO.builder()
                .id(b.getId())
                .finishedProductId(b.getFinishedProductId())
                .finishedProductName(finishedName)
                .recipeId(b.getRecipeId())
                .quantityProduced(b.getQuantityProduced())
                .ingredientCost(b.getIngredientCost())
                .unitCost(b.getUnitCost())
                .status(b.getStatus().name())
                .producedBy(b.getProducedBy())
                .notes(b.getNotes())
                .createdAt(b.getCreatedAt())
                .build();
    }

    // ── Reports (M4) ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<IngredientConsumptionDTO> getConsumption(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : LocalDateTime.now().minusDays(30).toLocalDate().atStartOfDay();
        LocalDateTime toDt = to != null ? to.atTime(23, 59, 59) : LocalDateTime.now();

        // Aggregate ingredient usage across COMPLETED batches, reconstructed from the recipe
        // (v1 — no per-batch consumption snapshot; uses the current recipe definition).
        Map<Long, BigDecimal> qtyByIngredient = new HashMap<>();
        Map<Long, BigDecimal> costByIngredient = new HashMap<>();
        Map<Long, List<RecipeItem>> itemsCache = new HashMap<>();

        for (ProductionBatch b : batchRepository.findAllInRange(fromDt, toDt)) {
            if (b.getStatus() != ProductionBatch.Status.COMPLETED || b.getRecipeId() == null) continue;
            Recipe recipe = recipeRepository.findByIdAndDeletedFalse(b.getRecipeId()).orElse(null);
            if (recipe == null) continue;
            BigDecimal yield = recipe.getYieldQuantity() != null && recipe.getYieldQuantity().signum() > 0
                    ? recipe.getYieldQuantity() : BigDecimal.ONE;
            BigDecimal runs = b.getQuantityProduced().divide(yield, 6, RoundingMode.HALF_UP);
            List<RecipeItem> items = itemsCache.computeIfAbsent(b.getRecipeId(),
                    recipeItemRepository::findByRecipeIdAndDeletedFalse);
            for (RecipeItem it : items) {
                BigDecimal consumed = it.getQuantity().multiply(runs);
                Product ing = productRepository.findByIdAndDeletedFalse(it.getIngredientProductId()).orElse(null);
                BigDecimal unitCost = ing != null && ing.getCostPrice() != null ? ing.getCostPrice() : BigDecimal.ZERO;
                qtyByIngredient.merge(it.getIngredientProductId(), consumed, BigDecimal::add);
                costByIngredient.merge(it.getIngredientProductId(), consumed.multiply(unitCost), BigDecimal::add);
            }
        }

        return qtyByIngredient.entrySet().stream()
                .map(e -> {
                    Product ing = productRepository.findByIdAndDeletedFalse(e.getKey()).orElse(null);
                    return IngredientConsumptionDTO.builder()
                            .ingredientProductId(e.getKey())
                            .ingredientName(ing != null ? ing.getName() : null)
                            .unit(ing != null ? ing.getUnit() : null)
                            .totalQuantity(e.getValue().setScale(3, RoundingMode.HALF_UP))
                            .totalCost(costByIngredient.getOrDefault(e.getKey(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP))
                            .build();
                })
                .sorted(Comparator.comparing(IngredientConsumptionDTO::getTotalCost).reversed())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductionSummaryDTO getSummary(LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : LocalDateTime.now().minusDays(30).toLocalDate().atStartOfDay();
        LocalDateTime toDt = to != null ? to.atTime(23, 59, 59) : LocalDateTime.now();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        long completed = 0, spoiled = 0, todayCount = 0;
        BigDecimal units = BigDecimal.ZERO, ingredientCost = BigDecimal.ZERO, todayUnits = BigDecimal.ZERO;
        for (ProductionBatch b : batchRepository.findAllInRange(fromDt, toDt)) {
            if (b.getStatus() == ProductionBatch.Status.SPOILED) { spoiled++; continue; }
            completed++;
            units = units.add(b.getQuantityProduced());
            ingredientCost = ingredientCost.add(b.getIngredientCost() != null ? b.getIngredientCost() : BigDecimal.ZERO);
            if (b.getCreatedAt() != null && !b.getCreatedAt().isBefore(todayStart)) {
                todayCount++;
                todayUnits = todayUnits.add(b.getQuantityProduced());
            }
        }
        return ProductionSummaryDTO.builder()
                .batchCount(completed)
                .spoiledCount(spoiled)
                .totalUnitsProduced(units)
                .totalIngredientCost(ingredientCost.setScale(2, RoundingMode.HALF_UP))
                .todayBatchCount(todayCount)
                .todayUnitsProduced(todayUnits)
                .build();
    }
}
