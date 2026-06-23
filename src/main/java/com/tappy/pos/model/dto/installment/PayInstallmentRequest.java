package com.tappy.pos.model.dto.installment;

import lombok.Data;

import java.math.BigDecimal;

/** Record payment of one kỳ. amount defaults to the kỳ's scheduled amount when omitted. */
@Data
public class PayInstallmentRequest {
    private BigDecimal amount;
    private String method;   // CASH / TRANSFER...
}
