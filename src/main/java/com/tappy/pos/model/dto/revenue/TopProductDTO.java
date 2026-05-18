package com.tappy.pos.model.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductDTO {
    private Long productId;
    private String productName;
    private Long quantitySold;
    private BigDecimal revenue;
    private BigDecimal cost;
    private BigDecimal profit;
}
