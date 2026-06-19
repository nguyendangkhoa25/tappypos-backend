package com.tappy.pos.repository.recipe;

import com.tappy.pos.model.entity.recipe.RecipeItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/** RLS auto-scopes every query to the current tenant. */
public interface RecipeItemRepository extends JpaRepository<RecipeItem, Long> {

    List<RecipeItem> findByRecipeIdAndDeletedFalse(Long recipeId);

    @Modifying
    @Query("UPDATE RecipeItem ri SET ri.deleted = true WHERE ri.recipeId = :recipeId")
    void softDeleteByRecipeId(@Param("recipeId") Long recipeId);
}
