package com.tappy.pos.model.entity.recipe;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/** One ingredient line of a {@link Recipe}: an ingredient product + the quantity it consumes. */
@Entity
@Table(name = "recipe_item")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class RecipeItem extends TenantAwareEntity {

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "ingredient_product_id", nullable = false)
    private Long ingredientProductId;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;

    @Column(length = 20)
    private String unit;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;
}
