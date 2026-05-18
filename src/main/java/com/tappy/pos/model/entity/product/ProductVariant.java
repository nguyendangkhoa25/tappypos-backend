package com.tappy.pos.model.entity.product;

import com.tappy.pos.model.entity.TenantAwareEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.Map;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ProductVariant extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 100)
    private String sku;

    @Column(length = 100)
    private String barcode;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "variant_options", columnDefinition = "jsonb", nullable = false)
    private Map<String, String> variantOptions;

    @Column(name = "price_override", precision = 15, scale = 2)
    private BigDecimal priceOverride;

    @Column(name = "cost_override", precision = 15, scale = 2)
    private BigDecimal costOverride;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VariantStatus status = VariantStatus.ACTIVE;

    public enum VariantStatus {
        ACTIVE,
        INACTIVE
    }
}
