package com.tappy.pos.model.dto.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Create / update payload for a master-curated suggested product.
 */
@Data
@Builder
public class ProductSuggestionRequest {

    @NotBlank
    private String name;

    @NotNull
    @PositiveOrZero
    private Long defaultPrice;

    private String unit;

    private String productTypeCode;

    private String emoji;

    private Boolean dynamicPrice;

    private List<String> shopTypes;

    private Integer displayOrder;

    private String categoryName;

    private String nameEn;

    private Integer durationMinutes;
}
