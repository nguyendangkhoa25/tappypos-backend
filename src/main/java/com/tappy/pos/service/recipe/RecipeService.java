package com.tappy.pos.service.recipe;

import com.tappy.pos.model.dto.recipe.RecipeCostDTO;
import com.tappy.pos.model.dto.recipe.RecipeDTO;
import com.tappy.pos.model.dto.recipe.SaveRecipeRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RecipeService {

    /** Create or replace the recipe (định lượng) for a finished product. */
    RecipeDTO saveRecipe(SaveRecipeRequest request);

    RecipeDTO getRecipeById(Long id);

    /** The recipe for a finished product, or null if none defined. */
    RecipeDTO getRecipeByProduct(Long finishedProductId);

    Page<RecipeDTO> listRecipes(Pageable pageable);

    void deleteRecipe(Long id);

    /** Live cost breakdown + suggested price for a recipe at the given gross margin (e.g. 0.30). */
    RecipeCostDTO getCost(Long recipeId, java.math.BigDecimal marginPct);
}
