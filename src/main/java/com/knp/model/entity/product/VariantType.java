package com.knp.model.entity.product;

import jakarta.persistence.*;
import com.knp.model.entity.TenantAwareEntity;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "variant_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class VariantType extends TenantAwareEntity {

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /** Null means this variant type applies to all product types. */
    @Column(name = "product_type_id")
    private Long productTypeId;

    @Builder.Default
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Builder.Default
    @OneToMany(mappedBy = "variantType", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<VariantTypeOption> options = new ArrayList<>();
}
