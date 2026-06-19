package com.tappy.pos.model.dto.order;

import jakarta.validation.constraints.DecimalMin;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request body for POST /orders/preorders/{id}/settle — collect the remaining balance
 * (còn lại) on a PENDING pre-order at pickup and complete it.
 */
@Data
@NoArgsConstructor
public class SettlePreOrderRequest {

    /** Payment method for the balance: CASH, CARD, QR. Defaults to CASH. */
    private String paymentMethod;

    /**
     * Cash tendered for the balance (used for change calculation). Optional — when null,
     * the exact balance due is assumed and change is 0.
     */
    @DecimalMin(value = "0.0", message = "Amount received must be >= 0")
    private BigDecimal amountReceived;
}
