package com.tappy.pos.service.finance;

import com.tappy.pos.model.dto.finance.CreateDebtRequest;
import com.tappy.pos.model.dto.finance.CustomerDebtDTO;
import com.tappy.pos.model.dto.finance.CustomerDebtStatementDTO;
import com.tappy.pos.model.dto.finance.CustomerDebtSummaryDTO;
import com.tappy.pos.model.dto.finance.DebtPaymentDTO;
import com.tappy.pos.model.dto.finance.RecordDebtPaymentRequest;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerDebtService {

    /** Per-customer outstanding balances — the main "Công nợ" list. */
    List<CustomerDebtSummaryDTO> getBalances();

    /** One customer's full statement: debts + repayments + current balance. */
    CustomerDebtStatementDTO getCustomerStatement(Long customerId);

    /** Record a credit sale / manual debt (ghi nợ). */
    CustomerDebtDTO createDebt(CreateDebtRequest request);

    /** Record a repayment (thu nợ); allocated to the customer's oldest open debts first. */
    DebtPaymentDTO recordPayment(RecordDebtPaymentRequest request);

    /** Shop-wide total outstanding debt. */
    BigDecimal getTotalOutstanding();

    /** Soft-delete a debt entry. */
    void deleteDebt(Long id);
}
