package com.tappy.pos.model.dto.product;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProductSuggestionDTO {
    private Long id;
    private String name;
    private String emoji;
    private Long defaultPrice;
    private String unit;
    private String productTypeCode;
    private Boolean dynamicPrice;
    private List<String> shopTypes;
    private Integer displayOrder;
    private String categoryName;
    private String nameEn;
    private Integer durationMinutes;
}
