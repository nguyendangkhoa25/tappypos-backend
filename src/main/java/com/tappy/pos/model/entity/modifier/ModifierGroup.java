package com.tappy.pos.model.entity.modifier;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * A reusable group of add-on options (e.g. Size, Đá, Đường, Topping) that can be attached
 * to products. {@code minSelect}/{@code maxSelect} bound how many options a customer picks.
 */
@Entity
@Table(name = "modifier_groups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ModifierGroup extends TenantAwareEntity {

    @Column(nullable = false, length = 150)
    private String name;

    @Builder.Default
    @Column(name = "min_select", nullable = false)
    private Integer minSelect = 0;

    @Builder.Default
    @Column(name = "max_select", nullable = false)
    private Integer maxSelect = 1;

    @Builder.Default
    @Column(nullable = false)
    private Boolean required = false;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "legacy_id", length = 50)
    private String legacyId;

    @Builder.Default
    @OneToMany(mappedBy = "modifierGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<ModifierOption> options = new ArrayList<>();
}
