package com.tappy.pos.model.entity.stocktake;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

/**
 * One product's counted quantity within a stocktake session.
 * Unique per (session, product) — re-scanning the same product upserts this row.
 */
@Entity
@Table(name = "stocktake_count")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class StocktakeCountEntity extends TenantAwareEntity {

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "inventory_id")
    private Long inventoryId;

    /** System stock snapshot captured when this product was first counted. */
    @Builder.Default
    @Column(name = "expected_qty", nullable = false)
    private Long expectedQty = 0L;

    /** Real physical quantity entered by the counter. */
    @Builder.Default
    @Column(name = "counted_qty", nullable = false)
    private Long countedQty = 0L;

    /** counted - expected (recomputed on every upsert). */
    @Builder.Default
    @Column(name = "difference", nullable = false)
    private Long difference = 0L;

    @Column(name = "counted_by")
    private String countedBy;

    @Column(name = "counted_at")
    private LocalDateTime countedAt;

    @Builder.Default
    @Column(name = "applied", nullable = false)
    private Boolean applied = false;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;
}
