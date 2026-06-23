package com.tappy.pos.service.recipe;

import com.tappy.pos.exception.BadRequestException;
import com.tappy.pos.exception.ResourceNotFoundException;
import com.tappy.pos.model.dto.recipe.RecipeCostDTO;
import com.tappy.pos.model.dto.recipe.RecipeDTO;
import com.tappy.pos.model.dto.recipe.SaveRecipeRequest;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.recipe.Recipe;
import com.tappy.pos.model.entity.recipe.RecipeItem;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.recipe.RecipeItemRepository;
import com.tappy.pos.repository.recipe.RecipeRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecipeServiceImpl cost math Unit Tests")
class RecipeServiceImplTest {

    @Mock private RecipeRepository recipeRepository;
    @Mock private RecipeItemRepository recipeItemRepository;
    @Mock private ProductRepository productRepository;
    @Mock private MessageService messageService;
    @Mock private ActivityLogService activityLogService;
    @Mock private TenantContext tenantContext;

    @InjectMocks private RecipeServiceImpl service;

    private static final String TENANT = "t1";

    @BeforeEach
    void setUp() {
        lenient().when(tenantContext.getCurrentTenantId()).thenReturn(TENANT);
        lenient().when(messageService.getMessage(anyString())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(messageService.getMessage(anyString(), any(Object[].class))).thenAnswer(inv -> inv.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("baker", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Product ingredient(long id, String name, String cost) {
        Product p = new Product();
        p.setId(id); p.setName(name); p.setCostPrice(new BigDecimal(cost)); p.setUnit("kg");
        return p;
    }

    private RecipeItem item(long id, long ingredientId, String qty) {
        return RecipeItem.builder().recipeId(1L).ingredientProductId(ingredientId)
                .quantity(new BigDecimal(qty)).build();
    }

    @Test
    @DisplayName("getCost computes unit cost = (ingredients + labor + overhead) / yield")
    void getCost_computesUnitCost() {
        // recipe yields 2 cakes; labor 10k, overhead 5k
        Recipe recipe = Recipe.builder()
                .finishedProductId(100L).yieldQuantity(new BigDecimal("2"))
                .laborCost(new BigDecimal("10000")).overheadCost(new BigDecimal("5000")).build();
        recipe.setId(1L);
        // 0.2kg flour @ 50,000 = 10,000 ; 4 eggs @ 3,000 = 12,000 → ingredients 22,000
        var items = List.of(item(1, 1, "0.2"), item(2, 2, "4"));
        Product finished = new Product(); finished.setId(100L); finished.setName("Bánh kem"); finished.setPrice(new BigDecimal("28000"));

        when(recipeRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(recipe));
        when(recipeItemRepository.findByRecipeIdAndDeletedFalse(1L)).thenReturn(items);
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(finished));
        when(productRepository.findAllById(List.of(1L, 2L)))
                .thenReturn(List.of(ingredient(1, "Bột", "50000"), ingredient(2, "Trứng", "3000")));

        RecipeCostDTO cost = service.getCost(1L, new BigDecimal("0.30"));

        assertThat(cost.getIngredientCost()).isEqualByComparingTo("22000");
        assertThat(cost.getTotalCost()).isEqualByComparingTo("37000");   // 22000 + 10000 + 5000
        assertThat(cost.getUnitCost()).isEqualByComparingTo("18500");    // 37000 / 2
        // suggested = 18500 / (1 - 0.30) = 26428.57 → rounded
        assertThat(cost.getSuggestedPrice()).isEqualByComparingTo("26429");
        // gross margin at sell 28000: (28000 - 18500)/28000 = 33.9%
        assertThat(cost.getGrossMarginPct()).isEqualByComparingTo("33.9");
        assertThat(cost.isMissingIngredientCost()).isFalse();
    }

    @Test
    @DisplayName("getCost flags an ingredient with no cost price")
    void getCost_flagsMissingCost() {
        Recipe recipe = Recipe.builder()
                .finishedProductId(100L).yieldQuantity(BigDecimal.ONE)
                .laborCost(BigDecimal.ZERO).overheadCost(BigDecimal.ZERO).build();
        recipe.setId(1L);
        var items = List.of(item(1, 1, "1"));
        Product noCost = ingredient(1, "Màu thực phẩm", "0");

        when(recipeRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(recipe));
        when(recipeItemRepository.findByRecipeIdAndDeletedFalse(1L)).thenReturn(items);
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.empty());
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(noCost));

        RecipeCostDTO cost = service.getCost(1L, null);

        assertThat(cost.getUnitCost()).isEqualByComparingTo("0");
        assertThat(cost.isMissingIngredientCost()).isTrue();
    }

    @Test
    @DisplayName("getCost on a missing recipe throws not-found")
    void getCost_missingRecipe_throws() {
        when(recipeRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getCost(1L, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("error.recipe.not.found");
    }

    @Test
    @DisplayName("getCost flags needsReprice when computed cost exceeds the stored cost price")
    void getCost_needsReprice_whenCostExceedsStored() {
        Recipe recipe = Recipe.builder()
                .finishedProductId(100L).yieldQuantity(BigDecimal.ONE)
                .laborCost(BigDecimal.ZERO).overheadCost(BigDecimal.ZERO).build();
        recipe.setId(1L);
        var items = List.of(item(1, 1, "1"));
        Product finished = new Product();
        finished.setId(100L);
        finished.setPrice(new BigDecimal("20000"));
        finished.setCostPrice(new BigDecimal("5000"));   // stored cost below computed 10,000

        when(recipeRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(recipe));
        when(recipeItemRepository.findByRecipeIdAndDeletedFalse(1L)).thenReturn(items);
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(finished));
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(ingredient(1, "Bột", "10000")));

        RecipeCostDTO cost = service.getCost(1L, null);

        assertThat(cost.getUnitCost()).isEqualByComparingTo("10000");
        assertThat(cost.getStoredCostPrice()).isEqualByComparingTo("5000");
        assertThat(cost.isNeedsReprice()).isTrue();
    }

    // ── saveRecipe ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("saveRecipe creates a new recipe with items, applies a default yield, and logs RECIPE_CREATED")
    void saveRecipe_new_createsWithItems() {
        Product finished = new Product();
        finished.setId(100L);
        finished.setName("Bánh mì");

        SaveRecipeRequest req = new SaveRecipeRequest();
        req.setFinishedProductId(100L);
        req.setYieldQuantity(null);   // → default ONE
        req.setLaborCost(new BigDecimal("5000"));
        req.setNotes("ghi chú");
        SaveRecipeRequest.Item good = new SaveRecipeRequest.Item();
        good.setIngredientProductId(1L);
        good.setQuantity(new BigDecimal("2"));
        good.setUnit("kg");
        // skipped: null id
        SaveRecipeRequest.Item nullId = new SaveRecipeRequest.Item();
        nullId.setIngredientProductId(null);
        nullId.setQuantity(new BigDecimal("1"));
        // skipped: non-positive quantity
        SaveRecipeRequest.Item zeroQty = new SaveRecipeRequest.Item();
        zeroQty.setIngredientProductId(2L);
        zeroQty.setQuantity(BigDecimal.ZERO);
        req.setItems(List.of(good, nullId, zeroQty));

        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(finished));
        when(recipeRepository.findByFinishedProductIdAndDeletedFalse(100L)).thenReturn(Optional.empty());
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> {
            Recipe r = inv.getArgument(0);
            r.setId(50L);
            return r;
        });
        when(recipeItemRepository.save(any(RecipeItem.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(ingredient(1, "Bột", "3000")));

        RecipeDTO dto = service.saveRecipe(req);

        assertThat(dto.getYieldQuantity()).isEqualByComparingTo("1");   // default
        assertThat(dto.getItems()).hasSize(1);                          // two skipped
        assertThat(dto.getFinishedProductName()).isEqualTo("Bánh mì");
        verify(recipeItemRepository).softDeleteByRecipeId(50L);
        verify(recipeItemRepository, times(1)).save(any(RecipeItem.class));
        verify(activityLogService).logAsync(eq(TENANT), eq("baker"), isNull(),
                eq(com.tappy.pos.model.enums.ActivityAction.RECIPE_CREATED),
                eq("RECIPE"), anyString(), eq("activity.recipe.created"), isNull(), eq("Bánh mì"));
    }

    @Test
    @DisplayName("saveRecipe updates an existing recipe and logs RECIPE_UPDATED")
    void saveRecipe_existing_updates() {
        Product finished = new Product();
        finished.setId(100L);
        finished.setName("Bánh mì");
        Recipe existing = Recipe.builder().tenantId(TENANT).finishedProductId(100L)
                .yieldQuantity(BigDecimal.ONE).build();
        existing.setId(50L);

        SaveRecipeRequest req = new SaveRecipeRequest();
        req.setFinishedProductId(100L);
        req.setYieldQuantity(new BigDecimal("3"));

        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(finished));
        when(recipeRepository.findByFinishedProductIdAndDeletedFalse(100L)).thenReturn(Optional.of(existing));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.findAllById(List.of())).thenReturn(List.of());

        RecipeDTO dto = service.saveRecipe(req);

        assertThat(dto.getYieldQuantity()).isEqualByComparingTo("3");
        verify(activityLogService).logAsync(eq(TENANT), eq("baker"), isNull(),
                eq(com.tappy.pos.model.enums.ActivityAction.RECIPE_UPDATED),
                eq("RECIPE"), anyString(), eq("activity.recipe.updated"), isNull(), eq("Bánh mì"));
    }

    @Test
    @DisplayName("saveRecipe rejects an item that is the finished product itself")
    void saveRecipe_selfIngredient_rejected() {
        Product finished = new Product();
        finished.setId(100L);
        finished.setName("Bánh mì");

        SaveRecipeRequest req = new SaveRecipeRequest();
        req.setFinishedProductId(100L);
        SaveRecipeRequest.Item self = new SaveRecipeRequest.Item();
        self.setIngredientProductId(100L);   // same as finished
        self.setQuantity(new BigDecimal("1"));
        req.setItems(List.of(self));

        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(finished));
        when(recipeRepository.findByFinishedProductIdAndDeletedFalse(100L)).thenReturn(Optional.empty());
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> {
            Recipe r = inv.getArgument(0);
            r.setId(50L);
            return r;
        });

        assertThatThrownBy(() -> service.saveRecipe(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("error.recipe.self.ingredient");
    }

    @Test
    @DisplayName("saveRecipe on a missing finished product throws not-found")
    void saveRecipe_missingFinishedProduct_throws() {
        SaveRecipeRequest req = new SaveRecipeRequest();
        req.setFinishedProductId(404L);
        when(productRepository.findByIdAndDeletedFalse(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveRecipe(req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("error.product.not.found");
    }

    // ── getRecipeById / getRecipeByProduct / listRecipes ──────────────────────────

    @Test
    @DisplayName("getRecipeById returns the DTO with items and ingredient names")
    void getRecipeById_returnsDto() {
        Recipe recipe = Recipe.builder().finishedProductId(100L).yieldQuantity(new BigDecimal("2"))
                .laborCost(BigDecimal.ZERO).overheadCost(BigDecimal.ZERO).build();
        recipe.setId(1L);
        var items = List.of(item(1, 1, "0.5"));
        Product finished = new Product();
        finished.setId(100L);
        finished.setName("Bánh");

        when(recipeRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(recipe));
        when(recipeItemRepository.findByRecipeIdAndDeletedFalse(1L)).thenReturn(items);
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.of(finished));
        when(productRepository.findAllById(List.of(1L))).thenReturn(List.of(ingredient(1, "Đường", "20000")));

        RecipeDTO dto = service.getRecipeById(1L);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getFinishedProductName()).isEqualTo("Bánh");
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getIngredientName()).isEqualTo("Đường");
        // 0.5 × 20,000 = 10,000 line cost
        assertThat(dto.getItems().get(0).getLineCost()).isEqualByComparingTo("10000");
    }

    @Test
    @DisplayName("getRecipeById on a missing recipe throws not-found")
    void getRecipeById_missing_throws() {
        when(recipeRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getRecipeById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("error.recipe.not.found");
    }

    @Test
    @DisplayName("getRecipeByProduct returns the recipe for a finished product")
    void getRecipeByProduct_found() {
        Recipe recipe = Recipe.builder().finishedProductId(100L).yieldQuantity(BigDecimal.ONE)
                .laborCost(BigDecimal.ZERO).overheadCost(BigDecimal.ZERO).build();
        recipe.setId(1L);

        when(recipeRepository.findByFinishedProductIdAndDeletedFalse(100L)).thenReturn(Optional.of(recipe));
        when(recipeItemRepository.findByRecipeIdAndDeletedFalse(1L)).thenReturn(List.of());
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.empty());
        when(productRepository.findAllById(List.of())).thenReturn(List.of());

        RecipeDTO dto = service.getRecipeByProduct(100L);

        assertThat(dto).isNotNull();
        assertThat(dto.getFinishedProductId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("getRecipeByProduct returns null when no recipe exists for the product")
    void getRecipeByProduct_absent_returnsNull() {
        when(recipeRepository.findByFinishedProductIdAndDeletedFalse(100L)).thenReturn(Optional.empty());
        assertThat(service.getRecipeByProduct(100L)).isNull();
    }

    @Test
    @DisplayName("listRecipes maps every recipe page entry to a DTO")
    void listRecipes_mapsPage() {
        Recipe recipe = Recipe.builder().finishedProductId(100L).yieldQuantity(BigDecimal.ONE)
                .laborCost(BigDecimal.ZERO).overheadCost(BigDecimal.ZERO).build();
        recipe.setId(1L);
        Pageable pageable = PageRequest.of(0, 10);

        when(recipeRepository.findByDeletedFalseOrderByUpdatedAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(recipe)));
        when(recipeItemRepository.findByRecipeIdAndDeletedFalse(1L)).thenReturn(List.of());
        when(productRepository.findByIdAndDeletedFalse(100L)).thenReturn(Optional.empty());
        when(productRepository.findAllById(List.of())).thenReturn(List.of());

        Page<RecipeDTO> page = service.listRecipes(pageable);

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(1L);
    }

    // ── deleteRecipe ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteRecipe soft-deletes the recipe and its items and logs RECIPE_DELETED")
    void deleteRecipe_softDeletes() {
        Recipe recipe = Recipe.builder().tenantId(TENANT).finishedProductId(100L).build();
        recipe.setId(1L);
        when(recipeRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(recipe));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(inv -> inv.getArgument(0));

        service.deleteRecipe(1L);

        assertThat(recipe.isDeleted()).isTrue();
        verify(recipeRepository).save(recipe);
        verify(recipeItemRepository).softDeleteByRecipeId(1L);
        verify(activityLogService).logAsync(eq(TENANT), eq("baker"), isNull(),
                eq(com.tappy.pos.model.enums.ActivityAction.RECIPE_DELETED),
                eq("RECIPE"), anyString(), eq("activity.recipe.deleted"), isNull(), anyString());
    }

    @Test
    @DisplayName("deleteRecipe on a missing recipe throws not-found")
    void deleteRecipe_missing_throws() {
        when(recipeRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteRecipe(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("error.recipe.not.found");
        verify(recipeRepository, never()).save(any());
    }
}
