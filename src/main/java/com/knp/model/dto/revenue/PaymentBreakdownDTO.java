package com.knp.model.dto.revenue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentBreakdownDTO {
    private String paymentMethod;
    private Long orderCount;
    private BigDecimal totalAmount;
    private Double percentage;
}
