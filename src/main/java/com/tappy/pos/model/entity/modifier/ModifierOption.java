package com.tappy.pos.model.entity.modifier;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/** One selectable option within a {@link ModifierGroup}, with a price delta added to the line. */
@Entity
@Table(name = "modifier_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ModifierOption extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "modifier_group_id", nullable = false)
    private ModifierGroup modifierGroup;

    @Column(nullable = false, length = 150)
    private String name;

    @Builder.Default
    @Column(name = "price_delta", precision = 15, scale = 2, nullable = false)
    private BigDecimal priceDelta = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;
}
