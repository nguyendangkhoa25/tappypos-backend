package com.knp.model.dto.buyback;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BuybackOrderItemDTO {
    private Long id;
    private String itemType;

    // BUY item
    private Long commodityId;
    private String commodityName;
    private String unit;
    private BigDecimal weight;
    private String conditionType;
    private BigDecimal pricePerUnit;

    // SALE item
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;

    // Common
    private BigDecimal totalPrice;
    private String notes;
}
