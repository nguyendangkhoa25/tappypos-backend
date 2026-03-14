package com.knp.model.dto.marketprice;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class MarketPriceDTO {
    private Long id;
    private String name;
    private String unit;
    private BigDecimal buyPrice;
    private BigDecimal sellPrice;
    private Boolean isActive;
    private String notes;
    private Integer sortOrder;
}
