package com.tappy.pos.service.recipe;

import com.tappy.pos.model.dto.recipe.RecipeCostDTO;
import com.tappy.pos.model.entity.product.Product;
import com.tappy.pos.model.entity.recipe.Recipe;
import com.tappy.pos.model.entity.recipe.RecipeItem;
import com.tappy.pos.multitenant.TenantContext;
import com.tappy.pos.repository.product.ProductRepository;
import com.tappy.pos.repository.recipe.RecipeItemRepository;
import com.tappy.pos.repository.recipe.RecipeRepository;
import com.tappy.pos.service.MessageService;
import com.tappy.pos.service.audit.ActivityLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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
}
