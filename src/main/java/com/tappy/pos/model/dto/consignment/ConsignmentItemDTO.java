package com.tappy.pos.model.dto.consignment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ConsignmentItemDTO {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantityPlaced;
    private BigDecimal unitPrice;
}
