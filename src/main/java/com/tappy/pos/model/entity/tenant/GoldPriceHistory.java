package com.tappy.pos.model.entity.tenant;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** A point-in-time snapshot of a shop gold-price row, for the shop's own price-history chart (4b). */
@Entity
@Table(name = "gold_price_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class GoldPriceHistory extends TenantAwareEntity {

    @Column(nullable = false, length = 50)
    private String code;

    @Column(length = 100)
    private String label;

    @Builder.Default
    @Column(nullable = false, precision = 20, scale = 0)
    private BigDecimal buy = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 20, scale = 0)
    private BigDecimal sell = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 20, scale = 0)
    private BigDecimal pawn = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    @Column(name = "legacy_id", length = 50)
    private String legacyId;
}
