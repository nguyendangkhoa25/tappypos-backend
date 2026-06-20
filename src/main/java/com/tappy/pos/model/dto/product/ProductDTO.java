package com.tappy.pos.model.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDTO {

    private Long id;
    private Long productTypeId;
    private String productTypeName;
    private String productTypeCode;
    private String sku;
    private String barcode;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal costPrice;
    private BigDecimal commissionRate;
    private Integer durationMinutes;
    private String unit;
    private String altUnit;
    private BigDecimal altUnitFactor;
    private BigDecimal altUnitPrice;
    private BigDecimal wholesalePrice;
    private Long vendorId;
    private String vendorName;
    private String shelfLocation;
    private String status;
    /** FINISHED (default, sellable) | INGREDIENT | BOTH — Phase 3 two-stage inventory. */
    private String productKind;
    private Set<Long> categoryIds;
    private Set<String> categoryNames;
    private Map<String, Object> attributes;
    private Boolean hasVariants;
    private Boolean hasModifiers;
    private Long stockQuantity;
    private Boolean inStock;
    private String imageUrl;
    private Long sourcePawnId;
    private String inventoryMode;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

