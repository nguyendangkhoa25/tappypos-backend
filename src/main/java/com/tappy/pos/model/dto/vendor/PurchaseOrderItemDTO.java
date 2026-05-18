package com.tappy.pos.model.dto.vendor;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PurchaseOrderItemDTO {
    private Long id;
    private Long productId;
    private Long variantId;
    private String variantLabel;
    private String productName;
    private String productSku;
    private Integer quantityOrdered;
    private Integer quantityReceived;
    private BigDecimal unitCost;
    private BigDecimal totalCost;
}
