package com.tappy.pos.model.dto.finance;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/** A single customer's debt statement: all debts + all repayments + current balance. */
@Data
@Builder
public class CustomerDebtStatementDTO {
    private Long customerId;
    private String customerName;
    private BigDecimal totalOutstanding;
    private List<CustomerDebtDTO> debts;
    private List<DebtPaymentDTO> payments;
}
