package com.tappy.pos.model.dto.consignment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/** One consigned title's settle-by-sales line over the chosen period. */
@Data
@Builder
public class ConsignmentSettlementLineDTO {
    private Long productId;
    private String productName;
    private Integer quantityPlaced;
    private Integer quantitySold;   // units sold in the period (from completed orders)
    private BigDecimal unitPrice;    // owed to publisher per sold unit
    private BigDecimal amountDue;    // quantitySold × unitPrice
}
