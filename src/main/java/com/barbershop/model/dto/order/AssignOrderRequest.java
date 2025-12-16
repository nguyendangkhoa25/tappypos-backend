package com.barbershop.model.dto.order;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignOrderRequest {
    private Long employeeId;
}
