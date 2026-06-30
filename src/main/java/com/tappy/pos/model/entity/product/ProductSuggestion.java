package com.tappy.pos.model.entity.product;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

/**
 * A master-curated "suggested product" shown when creating a new shop of a given type.
 * Lives in the shared {@code product_suggestions} table (created in V001).
 */
@Entity
@Table(name = "product_suggestions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String name;

    @Column(length = 20)
    private String emoji;

    @Column(name = "default_price")
    private Long defaultPrice;

    @Column(length = 50)
    private String unit;

    @Column(name = "product_type_code", length = 50)
    private String productTypeCode;

    @Column(name = "dynamic_price")
    private Boolean dynamicPrice;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "shop_types", columnDefinition = "text[]")
    private List<String> shopTypes;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "category_name", length = 200)
    private String categoryName;

    @Column(name = "name_en", length = 200)
    private String nameEn;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;
}
