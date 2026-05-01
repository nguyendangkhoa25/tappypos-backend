package com.knp.model.entity.product;

import jakarta.persistence.*;
import com.knp.model.entity.TenantAwareEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "variant_type_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class VariantTypeOption extends TenantAwareEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_type_id", nullable = false)
    private VariantType variantType;

    @Column(nullable = false, length = 100)
    private String value;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
