package com.tappy.pos.controller.finance;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.finance.CreateDebtRequest;
import com.tappy.pos.model.dto.finance.CustomerDebtDTO;
import com.tappy.pos.model.dto.finance.CustomerDebtStatementDTO;
import com.tappy.pos.model.dto.finance.CustomerDebtSummaryDTO;
import com.tappy.pos.model.dto.finance.DebtPaymentDTO;
import com.tappy.pos.model.dto.finance.RecordDebtPaymentRequest;
import com.tappy.pos.service.finance.CustomerDebtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/customer-debts")
@RequiredArgsConstructor
@RequiresFeature("CUSTOMER_DEBT")
public class CustomerDebtController {

    private final CustomerDebtService debtService;

    /** Per-customer outstanding balances — the main "Công nợ" list. */
    @GetMapping
    public ResponseEntity<ApiResponse<List<CustomerDebtSummaryDTO>>> getBalances() {
        log.info("Endpoint: GET /customer-debts");
        return ResponseEntity.ok(ApiResponse.success(debtService.getBalances(), "OK"));
    }

    /** Shop-wide total outstanding. */
    @GetMapping("/total")
    public ResponseEntity<ApiResponse<BigDecimal>> getTotalOutstanding() {
        log.info("Endpoint: GET /customer-debts/total");
        return ResponseEntity.ok(ApiResponse.success(debtService.getTotalOutstanding(), "OK"));
    }

    /** One customer's statement: debts + repayments + balance. */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<ApiResponse<CustomerDebtStatementDTO>> getStatement(@PathVariable Long customerId) {
        log.info("Endpoint: GET /customer-debts/customer/{}", customerId);
        return ResponseEntity.ok(ApiResponse.success(debtService.getCustomerStatement(customerId), "OK"));
    }

    /** Record a credit sale / manual debt (ghi nợ). */
    @PostMapping
    public ResponseEntity<ApiResponse<CustomerDebtDTO>> createDebt(@Valid @RequestBody CreateDebtRequest request) {
        log.info("Endpoint: POST /customer-debts");
        return ResponseEntity.ok(ApiResponse.success(debtService.createDebt(request), "Debt recorded"));
    }

    /** Record a repayment (thu nợ). */
    @PostMapping("/payments")
    public ResponseEntity<ApiResponse<DebtPaymentDTO>> recordPayment(@Valid @RequestBody RecordDebtPaymentRequest request) {
        log.info("Endpoint: POST /customer-debts/payments");
        return ResponseEntity.ok(ApiResponse.success(debtService.recordPayment(request), "Payment recorded"));
    }

    /** Soft-delete a debt entry. */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDebt(@PathVariable Long id) {
        log.info("Endpoint: DELETE /customer-debts/{}", id);
        debtService.deleteDebt(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Debt deleted"));
    }
}
