package com.barbershop.model.dto.order;

import lombok.*;

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
    private List<CreateOrderItemRequest> orderItems;
    private String notes;
}
