package com.tappy.pos.service.recipe;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.model.dto.inventory.InventoryDTO;
import com.tappy.pos.model.dto.recipe.ProduceRequest;
import com.tappy.pos.model.dto.recipe.ProductionBatchDTO;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.recipe.ProductionBatch;
import com.tappy.pos.model.entity.recipe.Recipe;
import com.tappy.pos.model.entity.recipe.RecipeItem;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.recipe.ProductionBatchRepository;
import com.tappy.pos.repository.recipe.RecipeItemRepository;
import com.tappy.pos.repository.recipe.RecipeRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import com.tappy.pos.service.inventory.InventoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductionServiceImpl produce Unit Tests")
class ProductionServiceImplTest {

    @Mock private RecipeRepository recipeRepository;
    @Mock private RecipeItemRepository recipeItemRepository;
    @Mock private ProductionBatchRepository batchRepository;
    @Mock private ProductRepository productRepository;
    @Mock private InventoryService inventoryService;
    @Mock private RecipeServiceImpl recipeService;
    @Mock private MessageService messageService;
    @Mock private ActivityLogService activityLogService;
    @Mock private TenantContext tenantContext;

    @InjectMocks private ProductionServiceImpl service;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("baker", null));
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn("shop1");
        lenient().when(messageService.getMessage(any())).thenReturn("err");
    }

    @AfterEach
    void tearDown() { SecurityContextHolder.clearContext(); }

    private Product finished() {
        Product p = new Product(); p.setId(100L); p.setName("Bánh kem 20cm"); p.setUnit("cái");
        p.setCostPrice(BigDecimal.ZERO);
        return p;
    }
    private Recipe recipe() {
        Recipe r = Recipe.builder().finishedProductId(100L).yieldQuantity(new BigDecimal("2"))
                .laborCost(new BigDecimal("10000")).overheadCost(new BigDecimal("5000")).build();
        r.setId(1L); return r;
    }
    private RecipeItem flourItem() {
        return RecipeItem.builder().recipeId(1L).ingredientProductId(1L).quantity(new BigDecimal("1")).build();
    }
    private InventoryDTO inv(long id, long qty) {
        return InventoryDTO.builder().id(id).quantityInStock(qty).build();
    }

    @Test
    @DisplayName("produce deducts ingredient stock, adds finished stock, snapshots cost")
    void produce_deductsAndAdds() {
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(finished()));
        when(recipeRepository.findByFinishedProductIdAndDeletedFalse(100L)).thenReturn(Optional.of(recipe()));
        when(recipeItemRepository.findByRecipeIdAndDeletedFalse(1L)).thenReturn(List.of(flourItem()));
        // ingredient 1 has 5 in stock (inv id 11); finished 100 inv id 10
        when(inventoryService.getInventoryByProductId(eq(1L), any())).thenReturn(new PageImpl<>(List.of(inv(11, 5))));
        when(inventoryService.getInventoryByProductId(eq(100L), any())).thenReturn(new PageImpl<>(List.of(inv(10, 0))));
        when(recipeService.computeCost(any(), any()))
                .thenReturn(new RecipeServiceImpl.CostResult(new BigDecimal("22000"), new BigDecimal("37000"), new BigDecimal("18500"), false));
        when(batchRepository.save(any(ProductionBatch.class))).thenAnswer(i -> { ProductionBatch b = i.getArgument(0); b.setId(7L); return b; });

        ProduceRequest req = new ProduceRequest();
        req.setFinishedProductId(100L);
        req.setQuantity(new BigDecimal("4"));   // yield 2 → 2 runs → flour required = 1 × 2 = 2

        ProductionBatchDTO dto = service.produce(req);

        verify(inventoryService).removeStock(11L, 2L);     // ingredient deducted
        verify(inventoryService).addStock(10L, 4L);        // finished added
        assertThat(dto.getStatus()).isEqualTo("COMPLETED");
        assertThat(dto.getQuantityProduced()).isEqualByComparingTo("4");
        assertThat(dto.getUnitCost()).isEqualByComparingTo("18500");
        assertThat(dto.getIngredientCost()).isEqualByComparingTo("44000"); // 22000 × 2 runs
    }

    @Test
    @DisplayName("produce rejects when ingredient stock is insufficient (no writes)")
    void produce_rejectsInsufficient() {
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(finished()));
        when(recipeRepository.findByFinishedProductIdAndDeletedFalse(100L)).thenReturn(Optional.of(recipe()));
        when(recipeItemRepository.findByRecipeIdAndDeletedFalse(1L)).thenReturn(List.of(flourItem()));
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(finished())); // any name
        when(inventoryService.getInventoryByProductId(eq(1L), any())).thenReturn(new PageImpl<>(List.of(inv(11, 1)))); // only 1 < 2

        ProduceRequest req = new ProduceRequest();
        req.setFinishedProductId(100L);
        req.setQuantity(new BigDecimal("4"));

        assertThatThrownBy(() -> service.produce(req)).isInstanceOf(BadRequestException.class);

        verify(inventoryService, never()).removeStock(any(), any());
        verify(inventoryService, never()).addStock(any(), any());
        verify(batchRepository, never()).save(any());
    }

    @Test
    @DisplayName("getConsumption aggregates ingredient usage across completed batches")
    void getConsumption_aggregates() {
        ProductionBatch batch = ProductionBatch.builder()
                .recipeId(1L).quantityProduced(new BigDecimal("4"))   // yield 2 → runs 2
                .ingredientCost(new BigDecimal("44000")).status(ProductionBatch.Status.COMPLETED).build();
        batch.setId(5L);
        when(batchRepository.findAllInRange(any(), any())).thenReturn(List.of(batch));
        when(recipeRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(recipe()));
        when(recipeItemRepository.findByRecipeIdAndDeletedFalse(1L)).thenReturn(List.of(flourItem()));
        Product flour = new Product(); flour.setId(1L); flour.setName("Bột"); flour.setUnit("kg"); flour.setCostPrice(new BigDecimal("50000"));
        when(productRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(flour));

        var rows = service.getConsumption(null, null);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).getIngredientName()).isEqualTo("Bột");
        assertThat(rows.get(0).getTotalQuantity()).isEqualByComparingTo("2");   // 1 × 2 runs
        assertThat(rows.get(0).getTotalCost()).isEqualByComparingTo("100000");  // 2 × 50000
    }

    @Test
    @DisplayName("getSummary counts completed vs spoiled and sums units")
    void getSummary_counts() {
        ProductionBatch done = ProductionBatch.builder()
                .quantityProduced(new BigDecimal("4")).ingredientCost(new BigDecimal("44000"))
                .status(ProductionBatch.Status.COMPLETED).build();
        done.setId(1L);
        ProductionBatch spoiled = ProductionBatch.builder()
                .quantityProduced(new BigDecimal("2")).ingredientCost(new BigDecimal("10000"))
                .status(ProductionBatch.Status.SPOILED).build();
        spoiled.setId(2L);
        when(batchRepository.findAllInRange(any(), any())).thenReturn(List.of(done, spoiled));

        var s = service.getSummary(null, null);

        assertThat(s.getBatchCount()).isEqualTo(1);
        assertThat(s.getSpoiledCount()).isEqualTo(1);
        assertThat(s.getTotalUnitsProduced()).isEqualByComparingTo("4");
        assertThat(s.getTotalIngredientCost()).isEqualByComparingTo("44000");
    }

    @Test
    @DisplayName("produce rejects a product with no recipe")
    void produce_rejectsNoRecipe() {
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(finished()));
        when(recipeRepository.findByFinishedProductIdAndDeletedFalse(100L)).thenReturn(Optional.empty());

        ProduceRequest req = new ProduceRequest();
        req.setFinishedProductId(100L);
        req.setQuantity(new BigDecimal("4"));

        assertThatThrownBy(() -> service.produce(req)).isInstanceOf(BadRequestException.class);
        verify(batchRepository, never()).save(any());
    }
}
