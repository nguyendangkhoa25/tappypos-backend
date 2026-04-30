package com.knp.model.entity.product;

import jakarta.persistence.*;
import com.knp.model.entity.BaseEntity;
import lombok.*;

@Entity
@Table(name = "attribute_definition")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeDefinition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_type_id", nullable = false)
    private ProductType productType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attribute_group_id")
    private AttributeGroup attributeGroup;

    @Column(nullable = false, length = 100)
    private String code;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "data_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private DataType dataType;

    @Builder.Default
    @Column(nullable = false)
    private Boolean required = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean searchable = false;

    @Builder.Default
    @Column(nullable = false)
    private Boolean filterable = false;

    @Builder.Default
    @Column(name = "display_order")
    private Integer displayOrder = 0;

    public enum DataType {
        STRING,
        TEXT,
        NUMBER,
        BOOLEAN,
        DATE
    }
}

