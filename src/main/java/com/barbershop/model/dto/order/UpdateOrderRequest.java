package com.barbershop.model.dto.order;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderRequest {
    private Long assignedEmployeeId;
    private String status;
    private String notes;
}
