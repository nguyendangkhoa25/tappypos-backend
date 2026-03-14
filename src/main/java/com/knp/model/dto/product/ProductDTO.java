package com.knp.model.dto.product;

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
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private BigDecimal costPrice;
    private String unit;
    private Long vendorId;
    private String vendorName;
    private String status;
    private Set<Long> categoryIds;
    private Map<String, Object> attributes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

