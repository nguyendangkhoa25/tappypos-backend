package com.knp.model.entity.product;

import jakarta.persistence.*;
import com.knp.model.entity.TenantAwareEntity;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "attribute_group")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeGroup extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_type_id", nullable = false)
    private ProductType productType;

    @Column(nullable = false, length = 100, unique = true)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Builder.Default
    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Builder.Default
    @OneToMany(mappedBy = "attributeGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AttributeDefinition> attributeDefinitions = new HashSet<>();
}

