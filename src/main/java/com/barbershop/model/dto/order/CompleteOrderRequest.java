package com.barbershop.model.dto.order;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteOrderRequest {
    private List<OrderItemUpdate> itemUpdates;
    private BigDecimal discountAmount;
    private BigDecimal taxPercentage;
    private String notes;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemUpdate {
        private Long itemId;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}

