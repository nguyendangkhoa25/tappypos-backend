package com.tappy.pos.controller.payment;

import com.tappy.pos.annotation.MasterDatabaseOnly;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.service.payment.SubscriptionPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Master-admin payment actions. Used to confirm a VietQR bank transfer (which has no synchronous
 * provider callback) → activates the subscription. Master-only.
 */
@RestController
@RequestMapping("/payments/admin")
@RequiredArgsConstructor
public class PaymentAdminController {

    private final SubscriptionPaymentService paymentService;

    @MasterDatabaseOnly
    @PostMapping("/confirm/{txnRef}")
    public ResponseEntity<ApiResponse<Void>> confirm(@PathVariable String txnRef) {
        paymentService.confirmManual(txnRef);
        return ResponseEntity.ok(ApiResponse.success(null, "OK"));
    }
}
