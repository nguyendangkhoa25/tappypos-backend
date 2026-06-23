package com.tappy.pos.model.dto.finance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

/** Record a repayment (thu nợ) for a customer; allocated to oldest open debts first. */
@Data
public class RecordDebtPaymentRequest {
    @NotNull
    private Long customerId;

    @NotNull
    @Positive
    private BigDecimal amount;

    /** CASH | TRANSFER | CARD — defaults to CASH when absent. */
    private String method;

    private String note;
}
