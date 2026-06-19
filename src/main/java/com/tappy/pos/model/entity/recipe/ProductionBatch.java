package com.tappy.pos.model.entity.recipe;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * A production run ("làm bánh"): produced N units of a finished product, deducting ingredient
 * stock and adding finished-goods stock. Snapshots the ingredient cost and cost-per-unit at
 * production time so historical batches keep their real cost even if ingredient prices change.
 */
@Entity
@Table(name = "production_batch")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class ProductionBatch extends TenantAwareEntity {

    @Column(name = "finished_product_id", nullable = false)
    private Long finishedProductId;

    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(name = "quantity_produced", nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityProduced;

    @Column(name = "ingredient_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal ingredientCost;

    @Column(name = "unit_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "produced_by", length = 100)
    private String producedBy;

    @Column(length = 500)
    private String notes;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;

    public enum Status { COMPLETED, SPOILED }
}
