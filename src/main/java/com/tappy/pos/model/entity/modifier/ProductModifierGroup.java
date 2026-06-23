package com.tappy.pos.model.entity.modifier;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/** Link row attaching a {@link ModifierGroup} to a product (a group is reusable across products). */
@Entity
@Table(name = "product_modifier_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductModifierGroup extends TenantAwareEntity {

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "modifier_group_id", nullable = false)
    private Long modifierGroupId;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
