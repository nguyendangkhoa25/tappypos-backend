package com.tappy.pos.model.entity.product;

import jakarta.persistence.*;
import com.tappy.pos.model.entity.TenantAwareEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "product_type")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductType extends TenantAwareEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 500)
    private String description;

    @Builder.Default
    @OneToMany(mappedBy = "productType", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AttributeDefinition> attributeDefinitions = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "productType", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AttributeGroup> attributeGroups = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "productType")
    private Set<Product> products = new HashSet<>();
}

