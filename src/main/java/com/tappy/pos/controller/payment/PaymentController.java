package com.tappy.pos.controller.payment;

import com.tappy.pos.annotation.RequiresFeature;
import com.tappy.pos.model.dto.ApiResponse;
import com.tappy.pos.model.dto.payment.CheckoutRequest;
import com.tappy.pos.model.dto.payment.CheckoutResponse;
import com.tappy.pos.service.payment.SubscriptionPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Shop-facing subscription payment endpoints. Gated by SHOP_INFO (same as the subscription view) and
 * read-only-exempt in TenantInterceptor, so an EXPIRED shop can still pay to renew.
 */
@RestController
@RequestMapping("/payments")
@RequiresFeature("SHOP_INFO")
@RequiredArgsConstructor
public class PaymentController {

    private final SubscriptionPaymentService paymentService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(@RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(ApiResponse.success(paymentService.createCheckout(request), "OK"));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> history() {
        return ResponseEntity.ok(ApiResponse.success(paymentService.historyForCurrentTenant(), "OK"));
    }
}
