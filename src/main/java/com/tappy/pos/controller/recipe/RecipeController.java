package com.tappy.pos.controller.recipe;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.recipe.RecipeCostDTO;
import com.tappy.pos.model.dto.recipe.RecipeDTO;
import com.tappy.pos.model.dto.recipe.SaveRecipeRequest;
import com.tappy.pos.service.recipe.RecipeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/recipes")
@RequiredArgsConstructor
@RequiresFeature("RECIPE")
@Slf4j
public class RecipeController {

    private final RecipeService recipeService;

    @PostMapping
    public ResponseEntity<ApiResponse<RecipeDTO>> save(@Valid @RequestBody SaveRecipeRequest request) {
        return ResponseEntity.ok(ApiResponse.success(recipeService.saveRecipe(request), "Recipe saved"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RecipeDTO>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(recipeService.listRecipes(PageRequest.of(page, size)), "Recipes retrieved"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RecipeDTO>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(recipeService.getRecipeById(id), "Recipe retrieved"));
    }

    @GetMapping("/by-product/{productId}")
    public ResponseEntity<ApiResponse<RecipeDTO>> getByProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success(recipeService.getRecipeByProduct(productId), "Recipe retrieved"));
    }

    @GetMapping("/{id}/cost")
    public ResponseEntity<ApiResponse<RecipeCostDTO>> getCost(
            @PathVariable Long id,
            @RequestParam(required = false) BigDecimal margin) {
        return ResponseEntity.ok(ApiResponse.success(recipeService.getCost(id, margin), "Recipe cost computed"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        recipeService.deleteRecipe(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Recipe deleted"));
    }
}
