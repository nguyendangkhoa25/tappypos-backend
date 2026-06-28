package com.tappy.pos.controller.payment;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.dashboard.MasterBillingStatsDTO;
import com.tappy.pos.model.dto.dashboard.PaymentLedgerItemDTO;
import com.tappy.pos.model.dto.payment.RecordOfflinePaymentRequest;
import com.tappy.pos.model.enums.PaymentProvider;
import com.tappy.pos.model.enums.PaymentStatus;
import com.tappy.pos.service.payment.MasterBillingService;
import com.tappy.pos.service.payment.SubscriptionPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * Master-admin billing endpoints (the platform operator's Billing &amp; Revenue cockpit):
 * confirm a manual VietQR transfer, view platform revenue stats + the cross-tenant payment ledger,
 * refund a payment, and record an offline (cash / direct transfer) payment. All master-only.
 */
@RestController
@RequestMapping("/payments/admin")
@RequiredArgsConstructor
// Gated by BILLING_MGMT (master-operator-only): a shop user can never hold this feature, and AGENT
// is deliberately NOT granted it (V008), so @RequiresFeature alone keeps both out — unlike
// @MasterDatabaseOnly, which admits AGENT. Mirrors ProductCatalogController (PRODUCT_CATALOG).
@RequiresFeature("BILLING_MGMT")
public class PaymentAdminController {

    private final SubscriptionPaymentService paymentService;
    private final MasterBillingService billingService;

    @PostMapping("/confirm/{txnRef}")
    public ResponseEntity<ApiResponse<Void>> confirm(@PathVariable String txnRef) {
        paymentService.confirmManual(txnRef);
        return ResponseEntity.ok(ApiResponse.success(null, "OK"));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<MasterBillingStatsDTO>> stats() {
        return ResponseEntity.ok(ApiResponse.success(billingService.getStats()));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<Page<PaymentLedgerItemDTO>>> payments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<PaymentLedgerItemDTO> result = billingService.getPayments(
                parseStatus(status), parseProvider(provider),
                blankToNull(plan), blankToNull(tenantId),
                from != null && !from.isBlank() ? LocalDate.parse(from).atStartOfDay() : null,
                to != null && !to.isBlank() ? LocalDate.parse(to).plusDays(1).atStartOfDay() : null,
                PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/refund/{txnRef}")
    public ResponseEntity<ApiResponse<Void>> refund(@PathVariable String txnRef) {
        paymentService.refund(txnRef);
        return ResponseEntity.ok(ApiResponse.success(null, "OK"));
    }

    @PostMapping("/record-offline")
    public ResponseEntity<ApiResponse<Void>> recordOffline(@RequestBody RecordOfflinePaymentRequest req) {
        paymentService.recordOfflinePayment(req.getTenantId(), req.getPlanCode(), req.getBillingCycle(), req.getNote());
        return ResponseEntity.ok(ApiResponse.success(null, "OK"));
    }

    // ── helpers: tolerant enum/blank parsing (an invalid filter value means "no filter") ──
    private static String blankToNull(String v) { return v == null || v.isBlank() ? null : v; }

    private static PaymentStatus parseStatus(String v) {
        try { return v == null || v.isBlank() ? null : PaymentStatus.valueOf(v.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private static PaymentProvider parseProvider(String v) {
        try { return v == null || v.isBlank() ? null : PaymentProvider.valueOf(v.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
