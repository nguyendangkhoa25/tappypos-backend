package com.tappy.pos.model.dto.order;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class PayAndCompleteRequest {

    /** Payment method: CASH, CARD, QR. Defaults to CASH. */
    private String paymentMethod;

    /** Cash tendered by the customer (used for change calculation). */
    @DecimalMin(value = "0.0", message = "Amount paid must be >= 0")
    private BigDecimal amountPaid;
}
