package com.tappy.pos.model.dto.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AddOrderItemRequest {

    @NotNull
    private Long productId;

    @Min(1)
    private int quantity = 1;

    private Long employeeId;

    private String note;
}
