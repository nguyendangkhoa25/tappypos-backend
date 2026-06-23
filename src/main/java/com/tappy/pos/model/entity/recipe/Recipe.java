package com.tappy.pos.model.entity.recipe;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * A recipe / bill-of-materials (định lượng) for one finished product: the ingredients +
 * quantities it consumes, plus labor/overhead and the yield of one production run.
 * Cost-per-unit is computed (not stored) from the current ingredient costs.
 */
@Entity
@Table(name = "recipe")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Recipe extends TenantAwareEntity {

    @Column(name = "finished_product_id", nullable = false)
    private Long finishedProductId;

    @Column(name = "yield_quantity", nullable = false, precision = 12, scale = 3)
    private BigDecimal yieldQuantity;

    @Column(name = "labor_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal laborCost;

    @Column(name = "overhead_cost", nullable = false, precision = 15, scale = 2)
    private BigDecimal overheadCost;

    @Column(length = 500)
    private String notes;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;

    @Column(name = "created_by", length = 100)
    private String createdBy;
}
