package com.tappy.pos.repository.recipe;

import com.tappy.pos.model.entity.recipe.Recipe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** RLS auto-scopes every query to the current tenant. */
public interface RecipeRepository extends JpaRepository<Recipe, Long> {

    Optional<Recipe> findByIdAndDeletedFalse(Long id);

    Optional<Recipe> findByFinishedProductIdAndDeletedFalse(Long finishedProductId);

    boolean existsByFinishedProductIdAndDeletedFalse(Long finishedProductId);

    Page<Recipe> findByDeletedFalseOrderByUpdatedAtDesc(Pageable pageable);
}
