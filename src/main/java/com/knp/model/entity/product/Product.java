package com.knp.model.entity.product;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.*;
import com.knp.model.entity.BaseEntity;
import com.knp.model.entity.vendor.Vendor;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Product extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_type_id", nullable = false)
    private ProductType productType;

    @Column(unique = true, nullable = false, length = 100)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 1000)
    private String description;

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "cost_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal costPrice = BigDecimal.ZERO;

    @Column(length = 20)
    private String unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @Builder.Default
    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ProductStatus status = ProductStatus.ACTIVE;

    @ManyToMany
    @JoinTable(
        name = "product_category",
        joinColumns = @JoinColumn(name = "product_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    private Set<Category> categories;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<ProductAttributeValue> attributeValues;

    public enum ProductStatus {
        ACTIVE,
        INACTIVE
    }

    @PostLoad
    @PostConstruct
    private void initializeCollections() {
        if (this.categories == null) {
            this.categories = new HashSet<>();
        }
        if (this.attributeValues == null) {
            this.attributeValues = new HashSet<>();
        }
    }

    public Set<Category> getCategories() {
        if (this.categories == null) {
            this.categories = new HashSet<>();
        }
        return this.categories;
    }

    public Set<ProductAttributeValue> getAttributeValues() {
        if (this.attributeValues == null) {
            this.attributeValues = new HashSet<>();
        }
        return this.attributeValues;
    }

    public ProductAttributeValue getAttributeValue(String attributeCode) {
        return attributeValues.stream()
            .filter(av -> av.getAttribute().getCode().equals(attributeCode))
            .findFirst()
            .orElse(null);
    }
}

