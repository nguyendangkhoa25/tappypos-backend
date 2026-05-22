package com.tappy.pos.model.dto.order;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class UpdateOrderMetaRequest {

    @DecimalMin(value = "0.0", message = "Tip must be >= 0")
    private BigDecimal tip;

    private Long customerId;

    private boolean clearCustomer;

    private String paymentMethod;
}
