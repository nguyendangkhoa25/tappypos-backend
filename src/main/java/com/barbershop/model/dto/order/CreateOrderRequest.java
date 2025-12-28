package com.barbershop.model.dto.order;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String customerNotes;
    private List<CreateOrderItemRequest> orderItems;
    private String notes;
    private BigDecimal discountAmount;
    private BigDecimal taxPercentage;
    private Boolean startImmediately; // If true, order will be created in IN_PROGRESS status
}
